package com.guardians.modules.device.service;

import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.device.dto.*;
import com.guardians.modules.device.repository.DeviceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.guardians.modules.device.repository.PairingCodeRepository;
import com.guardians.shared.entity.Device;
import com.guardians.shared.entity.PairingCode;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final PairingCodeRepository pairingCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${pairing.code-expiry-minutes:5}")
    private int codeExpiryMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Register Device ──────────────────────────────────────
    @Transactional
    public DeviceResponse registerDevice(RegisterDeviceRequest req, String ownerEmail) {
        User owner = findUserByEmail(ownerEmail);

        if (deviceRepository.existsByDeviceId(req.deviceId())) {
            // Update FCM token if device already exists
            Device existing = deviceRepository.findByDeviceId(req.deviceId())
                    .orElseThrow(() -> ApiException.notFound("Device not found"));
            if (!existing.getOwner().getId().equals(owner.getId())) {
                throw ApiException.forbidden("Device belongs to another user");
            }
            existing.setFcmToken(req.fcmToken());
            existing.setDeviceName(req.deviceName());
            existing.setLastSeen(Instant.now());
            return DeviceResponse.from(deviceRepository.save(existing));
        }

        Device.DeviceType type = req.type() != null ? req.type() : Device.DeviceType.CHILD;

        Device device = Device.builder()
                .deviceId(req.deviceId())
                .deviceName(req.deviceName())
                .type(type)
                .owner(owner)
                .fcmToken(req.fcmToken())
                .build();

        return DeviceResponse.from(deviceRepository.save(device));
    }

    // ── Get My Devices ───────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DeviceResponse> getMyDevices(String email) {
        User user = findUserByEmail(email);
        return deviceRepository.findByOwnerId(user.getId())
                .stream().map(DeviceResponse::from).toList();
    }

    // ── Get Linked Children (for Parent) ─────────────────────
    @Transactional(readOnly = true)
    public List<DeviceResponse> getLinkedChildren(String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        if (parent.getRole() != User.Role.PARENT) {
            throw ApiException.forbidden("Only parents can view linked children");
        }
        return deviceRepository.findActiveChildDevicesByParentId(parent.getId())
                .stream().map(DeviceResponse::from).toList();
    }

    // ── Generate Pairing Code (Parent) ───────────────────────
    @Transactional
    public GeneratePairingCodeResponse generatePairingCode(String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        if (parent.getRole() != User.Role.PARENT) {
            throw ApiException.forbidden("Only parents can generate pairing codes");
        }

        // Generate a new code (multiple active codes can coexist for this parent)
        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plus(codeExpiryMinutes, ChronoUnit.MINUTES);

        PairingCode pairingCode = PairingCode.builder()
                .code(code)
                .parent(parent)
                .expiresAt(expiresAt)
                .build();

        pairingCodeRepository.save(pairingCode);

        log.info("Pairing code generated for parent {} - expires in {} min", parentEmail, codeExpiryMinutes);

        return new GeneratePairingCodeResponse(code, expiresAt,
                "Share this code with your child's device. Expires in " + codeExpiryMinutes + " minutes.");
    }

    // ── Use Pairing Code (Child Device) ─────────────────────
    // childEmail is optional — if null (unauthenticated child), the parent becomes device owner
    @Transactional
    public PairingResultResponse usePairingCode(UsePairingCodeRequest req, String childEmail) {
        PairingCode pairingCode = pairingCodeRepository
                .findByCodeAndUsedFalse(req.code())
                .orElseThrow(() -> ApiException.badRequest("Invalid or already used pairing code"));

        if (pairingCode.isExpired()) {
            throw ApiException.badRequest("Pairing code has expired. Please ask parent to generate a new code.");
        }

        User parent = pairingCode.getParent();

        // If the child has a registered account use it, otherwise fall back to the parent
        // as owner so the device can still be created without requiring child registration.
        User deviceOwner = (childEmail != null && !childEmail.isBlank())
                ? findUserByEmail(childEmail)
                : parent;

        // Resolve deviceId — generate one if app lost local storage (e.g. reinstall)
        String deviceId = (req.deviceId() != null && !req.deviceId().isBlank())
                ? req.deviceId()
                : UUID.randomUUID().toString();

        // Find or create child device
        Device device;
        if (deviceRepository.existsByDeviceId(deviceId)) {
            device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> ApiException.notFound("Device not found"));
            // Allow re-pairing only if the device is owned by the same user
            // or if we are in anonymous mode (deviceOwner == parent)
            if (!device.getOwner().getId().equals(deviceOwner.getId())) {
                throw ApiException.forbidden("Device belongs to another user");
            }
        } else {
            device = Device.builder()
                    .deviceId(deviceId)
                    .deviceName(req.deviceName())
                    .type(Device.DeviceType.CHILD)
                    .owner(deviceOwner)
                    .fcmToken(req.fcmToken())
                    .build();
        }

        device.setLinkedParent(parent);
        device.setLastSeen(Instant.now());
        deviceRepository.save(device);

        // Mark code as used
        pairingCode.setUsed(true);
        pairingCode.setUsedAt(Instant.now());
        pairingCode.setChildDevice(device);
        pairingCodeRepository.save(pairingCode);

        log.info("Device {} paired with parent {} successfully (owner: {})",
                deviceId, parent.getEmail(), deviceOwner.getEmail());

        return new PairingResultResponse(
                true,
                "Device paired successfully with " + parent.getFullName(),
                parent.getId(),
                parent.getFullName(),
                DeviceResponse.from(device)
        );
    }

    // ── Link by Birth Certificate (Child Device) ─────────────
    @Transactional
    public LinkByBirthCertResponse linkByBirthCert(LinkByBirthCertRequest req) {
        User parent = userRepository.findByEmail(req.parentEmail())
                .orElseThrow(() -> ApiException.badRequest("Invalid parent email or password"));

        if (parent.getRole() != User.Role.PARENT) {
            throw ApiException.forbidden("Account is not a parent account");
        }

        if (!passwordEncoder.matches(req.parentPassword(), parent.getPassword())) {
            throw ApiException.badRequest("Invalid parent email or password");
        }

        String deviceId = (req.deviceInfo() != null && !req.deviceInfo().isBlank())
                ? req.deviceInfo()
                : UUID.randomUUID().toString();

        Device device;
        if (deviceRepository.existsByDeviceId(deviceId)) {
            device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> ApiException.notFound("Device not found"));
        } else {
            String name = (req.deviceName() != null && !req.deviceName().isBlank())
                    ? req.deviceName() : "Child Device";
            device = Device.builder()
                    .deviceId(deviceId)
                    .deviceName(name)
                    .type(Device.DeviceType.CHILD)
                    .owner(parent)
                    .fcmToken(req.fcmToken())
                    .build();
        }

        device.setLinkedParent(parent);
        device.setLastSeen(Instant.now());
        deviceRepository.save(device);

        log.info("Device {} linked to parent {} via birth certificate", deviceId, parent.getEmail());

        return new LinkByBirthCertResponse(
                true,
                "Device linked successfully to " + parent.getFullName(),
                deviceId,
                parent.getId(),
                parent.getFullName(),
                ""
        );
    }

    // ── Heartbeat (last-seen + status) ──────────────────────
    @Transactional
    public void updateHeartbeat(String deviceId, java.util.Map<String, Object> body) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(d -> {
            d.setLastSeen(Instant.now());
            if (body != null) {
                if (body.get("batteryLevel") instanceof Number n)
                    d.setBatteryLevel(n.intValue());
                if (body.get("isCharging") instanceof Boolean b)
                    d.setCharging(b);
                if (body.get("currentApp") instanceof String s)
                    d.setCurrentApp(s);
            }
            deviceRepository.save(d);
        });
    }

    // ── Update last seen ─────────────────────────────────────
    @Transactional
    public void updateLastSeen(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(d -> {
            d.setLastSeen(Instant.now());
            deviceRepository.save(d);
        });
    }

    // ── Scheduled cleanup ────────────────────────────────────
    @Scheduled(fixedDelay = 60_000) // every minute
    @Transactional
    public void cleanExpiredCodes() {
        pairingCodeRepository.deleteExpiredCodes(Instant.now());
    }

    // ── Helpers ──────────────────────────────────────────────
    private String generateUniqueCode() {
        String code;
        do {
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (pairingCodeRepository.findByCodeAndUsedFalse(code).isPresent());
        return code;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found: " + email));
    }
}
