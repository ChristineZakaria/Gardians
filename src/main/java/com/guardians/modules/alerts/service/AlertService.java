package com.guardians.modules.alerts.service;

import com.guardians.modules.alerts.dto.*;
import com.guardians.modules.alerts.repository.AlertRepository;
import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.device.repository.DeviceRepository;
import com.guardians.shared.entity.AlertEntity;
import com.guardians.shared.entity.Device;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final FirebaseService firebaseService;

    // ── Send Alert (called from child device) ────────────────
    @Transactional
    public AlertResponse sendAlert(SendAlertRequest req, String senderEmail) {
        // Find the sending device
        Device senderDevice = deviceRepository.findByDeviceId(req.deviceId())
                .orElseThrow(() -> ApiException.notFound("Device not found: " + req.deviceId()));

        // Verify caller owns this device
        User senderUser = findUserByEmail(senderEmail);
        if (!senderDevice.getOwner().getId().equals(senderUser.getId())) {
            throw ApiException.forbidden("You do not own this device");
        }

        // Get the linked parent
        if (senderDevice.getLinkedParent() == null) {
            throw ApiException.badRequest("Device is not paired with any parent");
        }

        User parent = senderDevice.getLinkedParent();
        AlertEntity.Severity severity = req.severity() != null
                ? req.severity() : AlertEntity.Severity.MEDIUM;

        AlertEntity alert = AlertEntity.builder()
                .parent(parent)
                .senderDevice(senderDevice)
                .type(req.type())
                .title(req.title())
                .message(req.message())
                .severity(severity)
                .metadata(req.metadata())
                .build();

        alertRepository.save(alert);

        // Fire FCM push to parent
        String fcmTitle = buildFcmTitle(severity, req.title());
        firebaseService.sendPushNotification(
                parent.getFcmToken(),
                fcmTitle,
                req.message() != null ? req.message() : req.title(),
                Map.of(
                    "alertId", String.valueOf(alert.getId()),
                    "type", req.type().name(),
                    "severity", severity.name(),
                    "deviceId", req.deviceId()
                )
        );

        log.info("Alert sent: type={} severity={} parent={} device={}",
                req.type(), severity, parent.getEmail(), req.deviceId());

        return AlertResponse.from(alert);
    }

    // ── SOS Emergency (no auth — child has no JWT) ───────────
    @Transactional
    public Map<String, Object> sendEmergency(Map<String, Object> body) {
        String deviceId = body.get("deviceId") instanceof String s ? s : null;
        if (deviceId == null || deviceId.isBlank())
            throw ApiException.badRequest("deviceId is required");

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.notFound("Device not found: " + deviceId));

        if (device.getLinkedParent() == null)
            throw ApiException.badRequest("Device is not paired with any parent");

        User parent = device.getLinkedParent();
        double lat = body.get("latitude")  instanceof Number n ? n.doubleValue() : 0;
        double lng = body.get("longitude") instanceof Number n ? n.doubleValue() : 0;

        AlertEntity alert = AlertEntity.builder()
                .parent(parent)
                .senderDevice(device)
                .type(AlertEntity.AlertType.SOS)
                .title("🆘 Emergency SOS from " + device.getDeviceName())
                .message("Child sent emergency alert! Location: " + lat + ", " + lng)
                .severity(AlertEntity.Severity.CRITICAL)
                .metadata(Map.of("latitude", lat, "longitude", lng,
                                 "timestamp", Instant.now().toString()))
                .build();

        alertRepository.save(alert);

        firebaseService.sendPushNotification(
                parent.getFcmToken(),
                "🆘 EMERGENCY SOS — " + device.getDeviceName(),
                "Your child needs help! Tap to see their location.",
                Map.of("alertId", String.valueOf(alert.getId()),
                       "type", "SOS", "severity", "CRITICAL",
                       "deviceId", deviceId,
                       "latitude", String.valueOf(lat),
                       "longitude", String.valueOf(lng))
        );

        log.warn("SOS EMERGENCY from device={} parent={} lat={} lng={}",
                deviceId, parent.getEmail(), lat, lng);
        return Map.of("success", true, "alertId", alert.getId());
    }

    // ── Get Alerts for Parent ────────────────────────────────
    @Transactional(readOnly = true)
    public AlertPageResponse getAlerts(String parentEmail, int page, int size, boolean unreadOnly) {
        User parent = findUserByEmail(parentEmail);
        Pageable pageable = PageRequest.of(page, size);

        Page<AlertEntity> alertPage = unreadOnly
                ? alertRepository.findByParentIdAndReadFalseOrderByCreatedAtDesc(parent.getId(), pageable)
                : alertRepository.findByParentIdOrderByCreatedAtDesc(parent.getId(), pageable);

        long unreadCount = alertRepository.countByParentIdAndReadFalse(parent.getId());

        return new AlertPageResponse(
                alertPage.getContent().stream().map(AlertResponse::from).toList(),
                alertPage.getTotalElements(),
                alertPage.getTotalPages(),
                page,
                unreadCount
        );
    }

    // ── Mark Alert as Read ───────────────────────────────────
    @Transactional
    public AlertResponse markRead(Long alertId, String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("Alert not found: " + alertId));

        if (!alert.getParent().getId().equals(parent.getId())) {
            throw ApiException.forbidden("Alert does not belong to you");
        }

        alert.setRead(true);
        return AlertResponse.from(alertRepository.save(alert));
    }

    // ── Mark All as Read ─────────────────────────────────────
    @Transactional
    public Map<String, Object> markAllRead(String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        int updated = alertRepository.markAllReadForParent(parent.getId());
        return Map.of("markedRead", updated);
    }

    // ── Delete Alert ─────────────────────────────────────────
    @Transactional
    public void deleteAlert(Long alertId, String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("Alert not found"));
        if (!alert.getParent().getId().equals(parent.getId())) {
            throw ApiException.forbidden("Alert does not belong to you");
        }
        alertRepository.delete(alert);
    }

    // ── Unread Count ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCount(String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        return Map.of("unreadCount", alertRepository.countByParentIdAndReadFalse(parent.getId()));
    }

    // ── Content Detection (TFLite on-device, no auth) ────────
    @Transactional
    public AlertResponse reportContentDetection(ContentDetectionRequest req) {
        Device device;
        if (req.deviceId() != null && !req.deviceId().isBlank()) {
            device = deviceRepository.findByDeviceId(req.deviceId())
                    .orElseThrow(() -> ApiException.notFound("Device not found: " + req.deviceId()));
        } else {
            // Game sent empty deviceId — fall back to most recently active child device
            var recent = deviceRepository.findMostRecentlyActiveChildDevices(
                    org.springframework.data.domain.PageRequest.of(0, 1));
            if (recent.isEmpty()) throw ApiException.badRequest("No active child device found");
            device = recent.get(0);
            log.info("Content detection: empty deviceId, using most recent device={}", device.getDeviceId());
        }

        if (device.getLinkedParent() == null) {
            throw ApiException.badRequest("Device is not paired with any parent");
        }

        User parent = device.getLinkedParent();
        AlertEntity.AlertType type = mapCategoryToType(req.detectedCategory());
        AlertEntity.Severity severity = mapCategoryToSeverity(req.detectedCategory());
        String childLabel = req.childName() != null ? req.childName() : device.getDeviceName();
        int pct = (int) Math.round(req.confidence() * 100);

        String humanTitle = buildHumanTitle(req.detectedCategory(), req.appName());
        String humanMsg   = buildHumanMessage(childLabel, req.appName(),
                                              req.detectedCategory(), pct, req.detail());

        java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("appName",          req.appName());
        meta.put("detectedCategory", req.detectedCategory());
        meta.put("confidence",       req.confidence());
        meta.put("childName",        childLabel);
        meta.put("detectedAt",       req.timestamp() != null ? req.timestamp() : Instant.now().toString());
        if (req.detail() != null && !req.detail().isBlank())
            meta.put("detectedText", req.detail());

        AlertEntity alert = AlertEntity.builder()
                .parent(parent)
                .senderDevice(device)
                .type(type)
                .title(humanTitle)
                .message(humanMsg)
                .severity(severity)
                .metadata(meta)
                .build();

        alertRepository.save(alert);

        firebaseService.sendPushNotification(
                parent.getFcmToken(),
                buildFcmTitle(severity, alert.getTitle()),
                alert.getMessage(),
                Map.of("alertId", String.valueOf(alert.getId()),
                       "type", type.name(),
                       "severity", severity.name(),
                       "deviceId", req.deviceId(),
                       "appName", req.appName())
        );

        log.info("Content detection alert: category={} app={} confidence={} device={}",
                req.detectedCategory(), req.appName(), req.confidence(), req.deviceId());

        return AlertResponse.from(alert);
    }

    // ── Get Alerts by Device ─────────────────────────────────
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByDevice(String deviceId, String parentEmail) {
        verifyParentOwnsDevice(deviceId, parentEmail);
        return alertRepository.findBySenderDeviceDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream().map(AlertResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getTodayAlertsByDevice(String deviceId, String parentEmail) {
        verifyParentOwnsDevice(deviceId, parentEmail);
        Instant startOfDay = ZonedDateTime.now(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        return alertRepository.findTodayByDeviceId(deviceId, startOfDay)
                .stream().map(AlertResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByDate(String deviceId, String parentEmail, String dateStr) {
        verifyParentOwnsDevice(deviceId, parentEmail);
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return alertRepository.findByDeviceIdAndDateRange(deviceId, start, end)
                .stream().map(AlertResponse::from).toList();
    }

    private AlertEntity.AlertType mapCategoryToType(String category) {
        return switch (category.toLowerCase()) {
            // Legacy TFLite categories
            case "adult content"                          -> AlertEntity.AlertType.INAPPROPRIATE_IMAGE;
            case "harmful content"                        -> AlertEntity.AlertType.INAPPROPRIATE_TEXT;
            case "suicide"                                -> AlertEntity.AlertType.SELF_HARM;
            // Image model categories
            case "image_unsafe", "image_weapon",
                 "image_drugs", "image_violence"         -> AlertEntity.AlertType.INAPPROPRIATE_IMAGE;
            // Video model categories
            case "video_unsafe"                           -> AlertEntity.AlertType.INAPPROPRIATE_VIDEO;
            case "video_harmful"                          -> AlertEntity.AlertType.INAPPROPRIATE_VIDEO;
            case "video_suicide"                          -> AlertEntity.AlertType.SELF_HARM;
            // Bear game
            case "game_inappropriate_touch"               -> AlertEntity.AlertType.INAPPROPRIATE_IMAGE;
            // URL monitoring
            case "url_threat"                             -> AlertEntity.AlertType.UNSAFE_URL;
            // Text / Perspective API
            case "text_threat", "text_toxicity",
                 "text_profanity", "text_sexually_explicit",
                 "text_mental_health"                       -> AlertEntity.AlertType.INAPPROPRIATE_TEXT;
            default                                       -> AlertEntity.AlertType.GENERAL;
        };
    }

    private AlertEntity.Severity mapCategoryToSeverity(String category) {
        return switch (category.toLowerCase()) {
            case "adult content", "suicide",
                 "image_unsafe", "video_unsafe",
                 "url_threat", "text_threat"              -> AlertEntity.Severity.CRITICAL;
            case "harmful content", "image_weapon",
                 "image_violence", "text_toxicity",
                 "text_sexually_explicit"                 -> AlertEntity.Severity.HIGH;
            case "image_drugs", "text_profanity",
                 "text_mental_health"                      -> AlertEntity.Severity.HIGH;
            case "video_harmful"                          -> AlertEntity.Severity.HIGH;
            case "video_suicide"                          -> AlertEntity.Severity.CRITICAL;
            case "game_inappropriate_touch"               -> AlertEntity.Severity.HIGH;
            default                                       -> AlertEntity.Severity.MEDIUM;
        };
    }

    private void verifyParentOwnsDevice(String deviceId, String parentEmail) {
        User parent = findUserByEmail(parentEmail);
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.notFound("Device not found"));
        if (device.getLinkedParent() == null || !device.getLinkedParent().getId().equals(parent.getId())) {
            throw ApiException.forbidden("This device is not linked to you");
        }
    }

    // ── Internal: create alert programmatically ──────────────
    @Transactional
    public void createSystemAlert(User parent, Device device,
                                  AlertEntity.AlertType type, String title,
                                  String message, AlertEntity.Severity severity) {
        AlertEntity alert = AlertEntity.builder()
                .parent(parent).senderDevice(device)
                .type(type).title(title).message(message).severity(severity)
                .build();
        alertRepository.save(alert);

        firebaseService.sendPushNotification(parent.getFcmToken(),
                buildFcmTitle(severity, title), message,
                Map.of("alertId", String.valueOf(alert.getId()),
                       "type", type.name(), "severity", severity.name()));
    }

    private String buildHumanTitle(String category, String appName) {
        String label = switch (category.toLowerCase()) {
            case "url_threat"              -> "Unsafe link opened";
            case "image_weapon"            -> "Weapon image detected";
            case "image_drugs"             -> "Drug-related image detected";
            case "image_violence"          -> "Violent image detected";
            case "image_unsafe"            -> "Unsafe image detected";
            case "video_unsafe"            -> "Unsafe video detected";
            case "game_inappropriate_touch" -> "⚠️ Inappropriate touch in Bear Game";
            case "text_threat"             -> "Threatening message detected";
            case "text_toxicity"           -> "Toxic content detected";
            case "text_profanity"          -> "Profanity detected";
            case "text_sexually_explicit"  -> "Explicit or grooming content detected";
            case "text_mental_health"      -> "Mental health concern detected";
            default                        -> category + " detected";
        };
        return label + " in " + appName;
    }

    private String buildHumanMessage(String childName, String appName,
                                     String category, int pct, String detail) {
        String action = switch (category.toLowerCase()) {
            case "url_threat"             -> "opened an unsafe link";
            case "image_weapon"           -> "viewed a weapon image";
            case "image_drugs"            -> "viewed drug-related content";
            case "image_violence"         -> "viewed violent content";
            case "video_unsafe"           -> "watched unsafe video content";
            case "text_threat",
                 "text_toxicity",
                 "text_sexually_explicit",
                 "text_profanity"         -> "sent or received harmful text";
            case "game_inappropriate_touch" -> "touched an inappropriate area in the Bear Game";
            case "text_mental_health"       -> "expressed concerning mental health content";
            default                       -> "triggered " + category;
        };
        StringBuilder sb = new StringBuilder();
        sb.append(childName).append(" ").append(action)
          .append(" on ").append(appName)
          .append(" (").append(pct).append("% confidence)");
        if (detail != null && !detail.isBlank()) {
            // Truncate long text for readability
            String snippet = detail.length() > 150 ? detail.substring(0, 150) + "…" : detail;
            sb.append("\n\n\"").append(snippet).append("\"");
        }
        return sb.toString();
    }

    private String buildFcmTitle(AlertEntity.Severity severity, String title) {
        return switch (severity) {
            case CRITICAL -> "🚨 URGENT: " + title;
            case HIGH     -> "⚠️ " + title;
            case MEDIUM   -> "🔔 " + title;
            case LOW      -> "ℹ️ " + title;
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
