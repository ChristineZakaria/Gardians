package com.guardians.modules.location.controller;

import com.guardians.modules.location.entity.Geofence;
import com.guardians.modules.location.repository.GeofenceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/geofence")
@RequiredArgsConstructor
@Tag(name = "Geofence", description = "Geofence management for child devices")
@SecurityRequirement(name = "bearerAuth")
public class GeofenceController {

    private final GeofenceRepository geofenceRepository;

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get geofence for a device")
    public ResponseEntity<Map<String, Object>> getGeofence(@PathVariable String deviceId) {
        return geofenceRepository.findByDeviceId(deviceId)
                .map(g -> ResponseEntity.ok(toMap(g)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{deviceId}")
    @Operation(summary = "Create or update geofence for a device")
    @Transactional
    public ResponseEntity<Map<String, Object>> setGeofence(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        double centerLat = ((Number) body.get("centerLat")).doubleValue();
        double centerLng = ((Number) body.get("centerLng")).doubleValue();
        double radiusMeters = ((Number) body.get("radiusMeters")).doubleValue();
        String name = body.containsKey("name") ? (String) body.get("name") : null;

        Geofence g = geofenceRepository.findByDeviceId(deviceId)
                .orElse(Geofence.builder().deviceId(deviceId).build());

        g.setCenterLat(centerLat);
        g.setCenterLng(centerLng);
        g.setRadiusMeters(radiusMeters);
        if (name != null) g.setName(name);
        g.setUpdatedAt(Instant.now());

        geofenceRepository.save(g);
        return ResponseEntity.ok(toMap(g));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete geofence for a device")
    @Transactional
    public ResponseEntity<Void> deleteGeofence(@PathVariable String deviceId) {
        geofenceRepository.deleteByDeviceId(deviceId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(Geofence g) {
        Map<String, Object> m = new HashMap<>();
        m.put("deviceId", g.getDeviceId());
        m.put("name", g.getName());
        m.put("centerLat", g.getCenterLat());
        m.put("centerLng", g.getCenterLng());
        m.put("radiusMeters", g.getRadiusMeters());
        m.put("updatedAt", g.getUpdatedAt().toString());
        return m;
    }
}
