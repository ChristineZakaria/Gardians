package com.guardians.modules.location.controller;

import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.device.repository.DeviceRepository;
import com.guardians.modules.location.entity.LocationEntry;
import com.guardians.modules.location.repository.LocationRepository;
import com.guardians.shared.entity.Device;
import com.guardians.shared.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
@Tag(name = "Location", description = "Child location tracking")
@SecurityRequirement(name = "bearerAuth")
public class LocationController {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    // ── Child sends its location ─────────────────────────────
    @PostMapping("/update")
    @Operation(summary = "Child sends current GPS location")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String deviceId = (String) body.get("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            throw ApiException.badRequest("deviceId is required");
        }

        Object latObj = body.get("latitude");
        Object lngObj = body.get("longitude");
        if (latObj == null || lngObj == null) {
            throw ApiException.badRequest("latitude and longitude are required");
        }
        if (!(latObj instanceof Number) || !(lngObj instanceof Number)) {
            throw ApiException.badRequest("latitude and longitude must be numbers");
        }

        double lat = ((Number) latObj).doubleValue();
        double lng = ((Number) lngObj).doubleValue();

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.notFound("Device not found"));

        Instant now = Instant.now();
        locationRepository.upsert(
                deviceId,
                device.getDeviceName(),
                lat,
                lng,
                device.getLinkedParent() != null ? device.getLinkedParent().getId() : null,
                now
        );

        return ResponseEntity.ok(Map.of("status", "updated", "timestamp", now.toString()));
    }

    // ── Parent gets child location ───────────────────────────
    @GetMapping("/{deviceId}")
    @Operation(summary = "Parent gets child current location")
    public ResponseEntity<Map<String, Object>> getLocation(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        LocationEntry entry = locationRepository.findById(deviceId)
                .orElseThrow(() -> ApiException.notFound("No location data yet for this device"));

        return ResponseEntity.ok(toMap(entry));
    }

    // ── Parent gets all children locations ───────────────────
    @GetMapping("/children")
    @Operation(summary = "Parent gets all children locations")
    public ResponseEntity<Map<String, Object>> getAllChildrenLocations(
            @AuthenticationPrincipal UserDetails userDetails) {

        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        List<LocationEntry> locations = locationRepository.findByLinkedParentId(user.getId());

        Map<String, Object> result = new HashMap<>();
        for (LocationEntry loc : locations) {
            result.put(loc.getDeviceId(), toMap(loc));
        }
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> toMap(LocationEntry entry) {
        Map<String, Object> m = new HashMap<>();
        m.put("deviceId", entry.getDeviceId());
        m.put("deviceName", entry.getDeviceName());
        m.put("latitude", entry.getLatitude());
        m.put("longitude", entry.getLongitude());
        m.put("linkedParentId", entry.getLinkedParentId());
        m.put("timestamp", entry.getUpdatedAt().toString());
        return m;
    }
}