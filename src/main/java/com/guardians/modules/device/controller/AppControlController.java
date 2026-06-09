package com.guardians.modules.device.controller;

import com.guardians.modules.alerts.service.FirebaseService;
import com.guardians.modules.apps.entity.AppBlock;
import com.guardians.modules.apps.entity.AppLimit;
import com.guardians.modules.apps.entity.AppUsage;
import com.guardians.modules.apps.repository.AppBlockRepository;
import com.guardians.modules.apps.repository.AppLimitRepository;
import com.guardians.modules.apps.repository.AppUsageRepository;
import com.guardians.modules.apps.repository.InstalledAppRepository;
import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.modules.device.repository.DeviceRepository;
import com.guardians.shared.entity.Device;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apps")
@RequiredArgsConstructor
@Tag(name = "App Control")
@SecurityRequirement(name = "bearerAuth")
public class AppControlController {

    private final AppUsageRepository appUsageRepository;
    private final InstalledAppRepository installedAppRepository;
    private final AppBlockRepository appBlockRepository;
    private final AppLimitRepository appLimitRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FirebaseService firebaseService;

    // ── App-level Block (Parent → blocks specific apps on child) ─────────────

    /**
     * Parent sends list of package names to block on a device.
     * Body: { "packages": ["com.example.app", ...] }
     * Replaces any existing blocks for this device.
     */
    @PostMapping("/{deviceId}/block")
    @Transactional
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Block specific apps on child device (replaces existing list)")
    public ResponseEntity<Map<String, Object>> blockApps(
            @PathVariable String deviceId,
            @RequestBody Map<String, List<String>> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());

        List<String> packages = body.get("packages");
        if (packages == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "packages list is required"));
        }

        // Replace all existing blocks for this device (empty list = unblock all)
        appBlockRepository.deleteAllByDeviceId(deviceId);

        if (!packages.isEmpty()) {
            List<AppBlock> blocks = packages.stream()
                    .map(pkg -> AppBlock.builder()
                            .deviceId(deviceId)
                            .packageName(pkg)
                            .build())
                    .collect(Collectors.toList());
            appBlockRepository.saveAll(blocks);
        }

        return ResponseEntity.ok(Map.of(
                "status", "blocked",
                "packages", packages
        ));
    }

    
    /**
     * Child device polls this every 30 seconds to get the current block list.
     * Returns: { "packages": ["com.example.app", ...] }
     
    */

    @GetMapping("/{deviceId}/blocked")
    @Operation(summary = "Get blocked apps for a device (child polls this)")
    public ResponseEntity<Map<String, Object>> getBlockedApps(
            @PathVariable String deviceId) {

        List<String> packages = appBlockRepository.findByDeviceId(deviceId)
                .stream()
                .map(AppBlock::getPackageName)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("packages", packages));
    }

    /**
     * Parent unblocks all apps on a device.
     */
    @DeleteMapping("/{deviceId}/unblock")
    @Transactional
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Unblock all apps on a child device")
    public ResponseEntity<Map<String, Object>> unblockAllApps(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        findAndVerifyOwnership(deviceId, userDetails.getUsername());
        appBlockRepository.deleteAllByDeviceId(deviceId);
        return ResponseEntity.ok(Map.of("status", "unblocked"));
    }

    /**
     * Parent unblocks a single specific app.
     * Use query param to avoid dot-truncation issues with package names in path:
     * DELETE /apps/{deviceId}/unblock/single?packageName=com.example.app
     */
    @DeleteMapping("/{deviceId}/unblock/single")
    @Transactional
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Unblock a single app on a child device")
    public ResponseEntity<Map<String, Object>> unblockSingleApp(
            @PathVariable String deviceId,
            @RequestParam String packageName,
            @AuthenticationPrincipal UserDetails userDetails) {
        findAndVerifyOwnership(deviceId, userDetails.getUsername());
        appBlockRepository.deleteByDeviceIdAndPackageName(deviceId, packageName);
        return ResponseEntity.ok(Map.of("status", "unblocked", "packageName", packageName));
    }

    // ── App Usage (Child → reports usage, Parent → reads it) ────────────────

    /**
     * Child reports app usage stats (called every 5 minutes by Flutter).
     * Body: { "apps": [{"packageName": "...", "appName": "...", "usageMillis": 12345}] }
     * Fetches all existing records in one query, then batch upserts.
     */
    @PostMapping("/{deviceId}/used")
    @Transactional
    @Operation(summary = "Child reports app usage stats")
    public ResponseEntity<Map<String, Object>> reportUsedApps(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> apps = (List<Map<String, Object>>) body.get("apps");
        if (apps == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "apps list is required"));
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // Fetch today's records only (one record per app per day)
        Map<String, AppUsage> existing = appUsageRepository.findByDeviceIdAndDate(deviceId, today)
                .stream()
                .collect(Collectors.toMap(AppUsage::getPackageName, u -> u));

        Instant now = Instant.now();
        List<AppUsage> toSave = apps.stream()
                .filter(app -> app.get("packageName") != null)
                .map(app -> {
                    String pkg  = (String) app.get("packageName");
                    String name = (String) app.get("appName");

                    // Accept both usageMillis (ms) and usageTimeMinutes (minutes)
                    long millis = 0L;
                    if (app.get("usageMillis") != null) {
                        millis = ((Number) app.get("usageMillis")).longValue();
                    } else if (app.get("usageTimeMinutes") != null) {
                        millis = ((Number) app.get("usageTimeMinutes")).longValue() * 60_000L;
                    }

                    AppUsage record = existing.getOrDefault(pkg,
                            AppUsage.builder().deviceId(deviceId).packageName(pkg).date(today).build());
                    record.setAppName(name);
                    record.setUsageMillis(millis);
                    record.setReportedAt(now);
                    return record;
                })
                .collect(Collectors.toList());

        appUsageRepository.saveAll(toSave);

        return ResponseEntity.ok(Map.of("status", "received", "count", toSave.size()));
    }

    /**
     * Parent gets the latest usage report for a child device.
     * Optional ?date=YYYY-MM-DD filters to a specific day; non-today dates return empty
     * (no per-day history stored yet — current snapshot is always "today").
     */
    @GetMapping("/{deviceId}/used")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Parent gets child app usage stats")
    public ResponseEntity<List<Map<String, Object>>> getUsedApps(
            @PathVariable String deviceId,
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());

        LocalDate queryDate = (date != null) ? LocalDate.parse(date) : LocalDate.now(ZoneOffset.UTC);

        List<String> blockedPackages = appBlockRepository.findByDeviceId(deviceId)
                .stream().map(AppBlock::getPackageName).collect(Collectors.toList());

        List<Map<String, Object>> result = appUsageRepository.findByDeviceIdAndDate(deviceId, queryDate)
                .stream()
                .map(u -> {
                    long millis  = u.getUsageMillis();
                    int  minutes = (int) (millis / 60_000L);
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("packageName",      u.getPackageName());
                    m.put("appName",          u.getAppName() != null ? u.getAppName() : "");
                    m.put("usageMillis",      millis);
                    m.put("usageTimeMinutes", minutes);
                    m.put("blocked",          blockedPackages.contains(u.getPackageName()));
                    m.put("reportedAt",       u.getReportedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 7-day screen-time summary for the parent reports bar chart.
     * Today = current usage snapshot; previous days = 0 (no per-day history yet).
     */
    @GetMapping("/{deviceId}/weekly")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get 7-day screen time totals for reports chart")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyUsage(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {
            LocalDate date = today.minusDays(daysAgo);
            long millis = appUsageRepository.sumUsageMillisByDeviceIdAndDate(deviceId, date);
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("date",         date.toString());
            entry.put("totalMinutes", millis / 60_000L);
            entry.put("totalMillis",  millis);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // ── Installed Apps (Child → reports full list, Parent → reads it) ────────

    /**
     * Child reports its full list of installed apps.
     * Body: { "apps": [{"packageName": "...", "appName": "..."}] }
     * Upserts each app (insert or update), removes apps no longer installed.
     */
    @PostMapping("/{deviceId}/installed")
    @Operation(summary = "Child reports installed apps list")
    public ResponseEntity<Map<String, Object>> reportInstalledApps(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> apps = (List<Map<String, Object>>) body.get("apps");
        if (apps == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "apps list is required"));
        }

        // Deduplicate by packageName, keeping first occurrence
        List<Map<String, Object>> deduped = new ArrayList<>(
                apps.stream()
                    .filter(a -> a.get("packageName") instanceof String s && !s.isBlank())
                    .collect(Collectors.toMap(
                            a -> (String) a.get("packageName"),
                            a -> a,
                            (a, b) -> a))
                    .values());

        List<String> currentPackages = deduped.stream()
                .map(a -> (String) a.get("packageName"))
                .collect(Collectors.toList());

        // Upsert each app — safe against concurrent calls (no delete race condition)
        for (Map<String, Object> app : deduped) {
            String pkg  = (String) app.get("packageName");
            String name = app.get("appName") instanceof String s ? s : "";
            installedAppRepository.upsert(deviceId, pkg, name);
        }

        // Remove apps that are no longer installed
        if (!currentPackages.isEmpty()) {
            installedAppRepository.deleteByDeviceIdAndPackageNameNotIn(deviceId, currentPackages);
        } else {
            installedAppRepository.deleteByDeviceId(deviceId);
        }

        return ResponseEntity.ok(Map.of("status", "received", "count", currentPackages.size()));
    }

    /**
     * Parent gets the full list of installed apps on a child device.
     * Also marks each app as blocked if it's in the block list.
     */
    @GetMapping("/{deviceId}/installed")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Parent gets child installed apps list")
    public ResponseEntity<List<Map<String, Object>>> getInstalledApps(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());

        List<String> blockedPackages = appBlockRepository.findByDeviceId(deviceId)
                .stream().map(AppBlock::getPackageName).collect(Collectors.toList());

        List<Map<String, Object>> result = installedAppRepository.findByDeviceId(deviceId)
                .stream()
                .map(a -> Map.<String, Object>of(
                        "packageName", a.getPackageName(),
                        "appName", a.getAppName() != null ? a.getAppName() : "",
                        "blocked", blockedPackages.contains(a.getPackageName()),
                        "updatedAt", a.getUpdatedAt().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── Summary (Parent dashboard — screen time + counts) ────────────────────

    /**
     * Single call for the parent dashboard card:
     * Returns total screen time, app count, and blocked app count.
     * GET /apps/{deviceId}/summary
     */
    @GetMapping("/{deviceId}/summary")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Parent gets child screen-time summary")
    public ResponseEntity<Map<String, Object>> getAppSummary(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());

        long totalUsageMillis = appUsageRepository.sumUsageMillisByDeviceId(deviceId);
        long totalApps        = installedAppRepository.findByDeviceId(deviceId).size();
        long blockedApps      = appBlockRepository.findByDeviceId(deviceId).size();

        // Convert millis to human-readable hours/minutes for convenience
        long totalMinutes = totalUsageMillis / 60_000;
        long hours        = totalMinutes / 60;
        long minutes      = totalMinutes % 60;

        return ResponseEntity.ok(Map.of(
                "deviceId",         deviceId,
                "totalUsageMillis", totalUsageMillis,
                "totalHours",       hours,
                "totalMinutes",     minutes,
                "screenTimeLabel",  hours + "h " + minutes + "m",
                "totalApps",        totalApps,
                "blockedApps",      blockedApps
        ));
    }

    // ── App Time Limits (Parent sets daily per-app limits) ───────────────────

    /**
     * Child polls this to get per-app daily limits.
     * Returns: { "com.example.app": 60, "com.game.xyz": 30 }
     */
    @GetMapping("/{deviceId}/app-limits")
    @Operation(summary = "Get per-app daily time limits (child polls this)")
    public ResponseEntity<Map<String, Integer>> getAppLimits(@PathVariable String deviceId) {
        Map<String, Integer> limits = appLimitRepository.findByDeviceId(deviceId)
                .stream()
                .collect(Collectors.toMap(AppLimit::getPackageName, AppLimit::getLimitMinutes));
        return ResponseEntity.ok(limits);
    }

    /**
     * Parent sets per-app daily limits. Merges with existing limits.
     * Body: { "com.example.app": 60, "com.game.xyz": 30 }
     */
    @PutMapping("/{deviceId}/app-limits")
    @Transactional
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Set per-app daily time limits (parent only)")
    public ResponseEntity<Map<String, Integer>> setAppLimits(
            @PathVariable String deviceId,
            @RequestBody Map<String, Integer> limits,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());

        Instant now = Instant.now();
        Map<String, AppLimit> existing = appLimitRepository.findByDeviceId(deviceId)
                .stream()
                .collect(Collectors.toMap(AppLimit::getPackageName, l -> l));

        List<AppLimit> toSave = limits.entrySet().stream()
                .map(e -> {
                    AppLimit limit = existing.getOrDefault(e.getKey(),
                            AppLimit.builder().deviceId(deviceId).packageName(e.getKey()).build());
                    limit.setLimitMinutes(e.getValue());
                    limit.setUpdatedAt(now);
                    return limit;
                })
                .collect(Collectors.toList());
        appLimitRepository.saveAll(toSave);

        deviceRepository.findByDeviceId(deviceId).ifPresent(device ->
            firebaseService.sendDataMessage(device.getFcmToken(), "APP_LIMITS_UPDATED", Map.of())
        );

        return ResponseEntity.ok(limits);
    }

    /**
     * Parent removes a daily limit for a single app (immediately unblocks it on child).
     * DELETE /apps/{deviceId}/app-limits?packageName=com.example.app
     */
    @DeleteMapping("/{deviceId}/app-limits")
    @Transactional
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Remove per-app daily limit (parent only)")
    public ResponseEntity<Map<String, Object>> removeAppLimit(
            @PathVariable String deviceId,
            @RequestParam String packageName,
            @AuthenticationPrincipal UserDetails userDetails) {

        findAndVerifyOwnership(deviceId, userDetails.getUsername());
        appLimitRepository.deleteByDeviceIdAndPackageName(deviceId, packageName);

        deviceRepository.findByDeviceId(deviceId).ifPresent(device ->
            firebaseService.sendDataMessage(device.getFcmToken(), "APP_LIMITS_UPDATED", Map.of())
        );

        return ResponseEntity.ok(Map.of("status", "removed", "packageName", packageName));
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
