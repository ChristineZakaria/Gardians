package com.guardians.modules.device.controller;

import com.guardians.modules.alerts.service.FirebaseService;
import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.device.repository.DeviceRepository;
import com.guardians.shared.entity.Device;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Device Block", description = "Block/Unblock child devices")
@SecurityRequirement(name = "bearerAuth")
public class BlockController {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FirebaseService firebaseService;

    @PreAuthorize("hasRole('PARENT')")
    @PostMapping("/{deviceId}/block")
    public ResponseEntity<Map<String, Object>> blockDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Device device = findAndVerifyOwnership(deviceId, userDetails.getUsername());
        device.setBlocked(true);
        deviceRepository.save(device);
        firebaseService.sendDataMessage(device.getFcmToken(), "DEVICE_BLOCKED", Map.of());
        return ResponseEntity.ok(Map.of(
            "status", "blocked",
            "deviceId", deviceId
        ));
    }

    @PreAuthorize("hasRole('PARENT')")
    @PostMapping("/{deviceId}/unblock")
    public ResponseEntity<Map<String, Object>> unblockDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Device device = findAndVerifyOwnership(deviceId, userDetails.getUsername());
        device.setBlocked(false);
        deviceRepository.save(device);
        firebaseService.sendDataMessage(device.getFcmToken(), "DEVICE_UNBLOCKED", Map.of());
        return ResponseEntity.ok(Map.of(
            "status", "unblocked",
            "deviceId", deviceId
        ));
    }

    @GetMapping("/{deviceId}/status")
    public ResponseEntity<Map<String, Object>> getDeviceStatus(
            @PathVariable String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.notFound("Device not found"));
        return ResponseEntity.ok(Map.of(
            "deviceId", deviceId,
            "blocked", device.isBlocked()
        ));
    }

    @PostMapping("/{deviceId}/ping")
    public ResponseEntity<Void> pingDevice(@PathVariable String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setLastSeen(java.time.Instant.now());
            device.setActive(true);
            deviceRepository.save(device);
        });
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the authenticated parent is the linked parent of the device.
     */
    private Device findAndVerifyOwnership(String deviceId, String parentEmail) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.notFound("Device not found"));
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (device.getLinkedParent() == null
                || !device.getLinkedParent().getId().equals(parent.getId())) {
            throw ApiException.forbidden("This device is not linked to you");
        }
        return device;
    }
}