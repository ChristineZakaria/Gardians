package com.guardians.modules.device.controller;

import com.guardians.modules.alerts.service.FirebaseService;
import com.guardians.modules.device.dto.*;
import com.guardians.modules.device.entity.DeviceSettings;
import com.guardians.modules.device.repository.DeviceRepository;
import com.guardians.modules.device.repository.DeviceSettingsRepository;
import com.guardians.modules.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Device registration and pairing")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceSettingsRepository deviceSettingsRepository;
    private final DeviceRepository deviceRepository;
    private final FirebaseService firebaseService;

    @PostMapping("/register")
    @Operation(summary = "Register a device (call after login to register device)")
    public ResponseEntity<DeviceResponse> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.registerDevice(req, user.getUsername()));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all devices owned by current user")
    public ResponseEntity<List<DeviceResponse>> myDevices(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deviceService.getMyDevices(user.getUsername()));
    }

    @GetMapping("/children")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get all child devices linked to this parent")
    public ResponseEntity<List<DeviceResponse>> linkedChildren(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deviceService.getLinkedChildren(user.getUsername()));
    }

    @PostMapping("/pairing/generate")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Generate 6-digit pairing code (Parent only)")
    public ResponseEntity<GeneratePairingCodeResponse> generatePairingCode(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deviceService.generatePairingCode(user.getUsername()));
    }

    @PostMapping("/pairing/link-by-birth-cert")
    @Operation(summary = "Link child device to parent using parent credentials + birth certificate (no auth required)")
    public ResponseEntity<LinkByBirthCertResponse> linkByBirthCert(
            @Valid @RequestBody LinkByBirthCertRequest req) {
        return ResponseEntity.ok(deviceService.linkByBirthCert(req));
    }

    @PostMapping("/pairing/register-child")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Parent creates a child account + device (no child device needed)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LinkByBirthCertResponse> registerChildForParent(
            @Valid @RequestBody RegisterChildRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.registerChildForParent(req, user.getUsername()));
    }

    @PostMapping("/pairing/connect")
    @Operation(summary = "Use pairing code to link child device to parent (auth optional)")
    public ResponseEntity<PairingResultResponse> usePairingCode(
            @Valid @RequestBody UsePairingCodeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        // user may be null if called without a JWT (child hasn't registered yet)
        String callerEmail = (user != null) ? user.getUsername() : null;
        return ResponseEntity.ok(deviceService.usePairingCode(req, callerEmail));
    }

    @PatchMapping("/{deviceId}/heartbeat")
    @Operation(summary = "Update device last-seen + status (heartbeat from child)")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String deviceId,
            @RequestBody(required = false) Map<String, Object> body) {
        deviceService.updateHeartbeat(deviceId, body);
        return ResponseEntity.ok().build();
    }

    // ── Device Settings ───────────────────────────────────────────────────────

    /**
     * Returns settings for a device — called by child (to enforce limits) and parent (to display).
     * Includes AI feature toggles so child can skip disabled models.
     */
    @GetMapping("/{deviceId}/settings")
    @Operation(summary = "Get device settings (screen time + AI feature toggles)")
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable String deviceId) {
        DeviceSettings s = deviceSettingsRepository.findById(deviceId)
                .orElse(DeviceSettings.builder().deviceId(deviceId).build());
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("deviceId",                deviceId);
        resp.put("screenTimeLimitMinutes",  s.getScreenTimeLimitMinutes());
        resp.put("contentScanEnabled",      s.getContentScanEnabled());
        resp.put("imageDetectionEnabled",   s.getImageDetectionEnabled());
        resp.put("videoDetectionEnabled",   s.getVideoDetectionEnabled());
        resp.put("urlDetectionEnabled",     s.getUrlDetectionEnabled());
        resp.put("textDetectionEnabled",    s.getTextDetectionEnabled());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{deviceId}/settings")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Update device settings (parent only)")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        DeviceSettings s = deviceSettingsRepository.findById(deviceId)
                .orElse(DeviceSettings.builder().deviceId(deviceId).build());

        if (body.containsKey("screenTimeLimitMinutes"))
            s.setScreenTimeLimitMinutes(((Number) body.get("screenTimeLimitMinutes")).intValue());
        if (body.containsKey("contentScanEnabled"))
            s.setContentScanEnabled((Boolean) body.get("contentScanEnabled"));
        if (body.containsKey("imageDetectionEnabled"))
            s.setImageDetectionEnabled((Boolean) body.get("imageDetectionEnabled"));
        if (body.containsKey("videoDetectionEnabled"))
            s.setVideoDetectionEnabled((Boolean) body.get("videoDetectionEnabled"));
        if (body.containsKey("urlDetectionEnabled"))
            s.setUrlDetectionEnabled((Boolean) body.get("urlDetectionEnabled"));
        if (body.containsKey("textDetectionEnabled"))
            s.setTextDetectionEnabled((Boolean) body.get("textDetectionEnabled"));

        s.setUpdatedAt(Instant.now());
        deviceSettingsRepository.save(s);

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("deviceId",                deviceId);
        resp.put("screenTimeLimitMinutes",  s.getScreenTimeLimitMinutes());
        resp.put("contentScanEnabled",      s.getContentScanEnabled());
        resp.put("imageDetectionEnabled",   s.getImageDetectionEnabled());
        resp.put("videoDetectionEnabled",   s.getVideoDetectionEnabled());
        resp.put("urlDetectionEnabled",     s.getUrlDetectionEnabled());
        resp.put("textDetectionEnabled",    s.getTextDetectionEnabled());
        return ResponseEntity.ok(resp);
    }
}
