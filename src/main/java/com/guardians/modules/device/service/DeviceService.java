package com.guardians.modules.device.service;

import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.auth.service.JwtService;
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
    private final JwtService jwtService;

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

        // Determine device owner — if child credentials provided, create/find child user
        User deviceOwner = parent;
        String childToken = "";
        boolean hasChildCredentials = req.childEmail() != null && !req.childEmail().isBlank()
                && req.childPassword() != null && !req.childPassword().isBlank();

        if (hasChildCredentials) {
            String childEmailLower = req.childEmail().toLowerCase().trim();
            deviceOwner = userRepository.findByEmail(childEmailLower).orElseGet(() -> {
                String name = (req.childName() != null && !req.childName().isBlank())
                        ? req.childName().trim() : "Child";
                User child = User.builder()
                        .email(childEmailLower)
                        .fullName(name)
                        .passwordHash(passwordEncoder.encode(req.childPassword()))
                        .role(User.Role.CHILD)
                        .build();
                return userRepository.save(child);
            });
            childToken = jwtService.generateToken(deviceOwner);
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
                    .owner(deviceOwner)
                    .fcmToken(req.fcmToken())
                    .build();
        }

        device.setLinkedParent(parent);
        device.setLastSeen(Instant.now());
        if (req.childGender() != null && !req.childGender().isBlank()) {
            device.setChildGender(req.childGender().toUpperCase());
        }
        deviceRepository.save(device);

        log.info("Device {} linked to parent {} via birth certificate (owner: {})",
                deviceId, parent.getEmail(), deviceOwner.getEmail());

        return new LinkByBirthCertResponse(
                true,
                "Device linked successfully to " + parent.getFullName(),
                deviceId,
                parent.getId(),
                parent.getFullName(),
                childToken
        );
    }

    // ── Register Child Account from Parent Side ─────────────
    @Transactional
    public LinkByBirthCertResponse registerChildForParent(RegisterChildRequest req, String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        if (parent.getRole() != User.Role.PARENT) {
            throw ApiException.forbidden("Only parents can register child accounts");
        }
        if (userRepository.existsByEmail(req.childEmail())) {
            throw ApiException.conflict("Email already registered: " + req.childEmail());
        }

        // Create child user account
        User child = User.builder()
                .email(req.childEmail().toLowerCase().trim())
                .fullName(req.childName().trim())
                .passwordHash(passwordEncoder.encode(req.childPassword()))
                .role(User.Role.CHILD)
                .build();
        userRepository.save(child);

        // Create device linked to parent
        String deviceId = UUID.randomUUID().toString();
        String deviceName = (req.deviceName() != null && !req.deviceName().isBlank())
                ? req.deviceName() : req.childName() + "'s Device";

        Device device = Device.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .type(Device.DeviceType.CHILD)
                .owner(child)
                .build();
        device.setLinkedParent(parent);
        if (req.childGender() != null && !req.childGender().isBlank()) {
            device.setChildGender(req.childGender().toUpperCase());
        }
        deviceRepository.save(device);

        log.info("Parent {} registered child account {} (device {})", parentEmail, req.childEmail(), deviceId);

        return new LinkByBirthCertResponse(
                true,
                "Child account created successfully",
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
