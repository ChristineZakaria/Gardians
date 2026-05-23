package com.guardians.modules.alerts.controller;

import com.guardians.modules.alerts.dto.*;
import com.guardians.modules.alerts.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Send and manage parental alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/send")
    @Operation(summary = "Send an alert from child device to parent (called by Child)")
    public ResponseEntity<AlertResponse> sendAlert(
            @Valid @RequestBody SendAlertRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.sendAlert(req, user.getUsername()));
    }

    @PostMapping("/content-detection")
    @Operation(summary = "Report TFLite unsafe content detection (public — no auth required)")
    public ResponseEntity<AlertResponse> reportContentDetection(
            @Valid @RequestBody ContentDetectionRequest req) {
        return ResponseEntity.ok(alertService.reportContentDetection(req));
    }

    @PostMapping("/emergency")
    @Operation(summary = "Child sends SOS emergency alert (public — no auth required)")
    public ResponseEntity<Map<String, Object>> sendEmergency(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(alertService.sendEmergency(body));
    }

    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get all alerts for a specific child device")
    public ResponseEntity<java.util.List<AlertResponse>> getDeviceAlerts(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.getAlertsByDevice(deviceId, user.getUsername()));
    }

    @GetMapping("/device/{deviceId}/today")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get today's alerts for a specific child device")
    public ResponseEntity<java.util.List<AlertResponse>> getTodayDeviceAlerts(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.getTodayAlertsByDevice(deviceId, user.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get all alerts for this parent (paginated)")
    public ResponseEntity<AlertPageResponse> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.getAlerts(user.getUsername(), page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get count of unread alerts")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.getUnreadCount(user.getUsername()));
    }

    @PatchMapping("/{alertId}/read")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Mark a single alert as read")
    public ResponseEntity<AlertResponse> markRead(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.markRead(alertId, user.getUsername()));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Mark all alerts as read")
    public ResponseEntity<Map<String, Object>> markAllRead(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.markAllRead(user.getUsername()));
    }

    @DeleteMapping("/{alertId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Delete an alert")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails user) {
        alertService.deleteAlert(alertId, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
