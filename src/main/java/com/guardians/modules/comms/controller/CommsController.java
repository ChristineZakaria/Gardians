package com.guardians.modules.comms.controller;

import com.guardians.modules.comms.entity.CallLogEntry;
import com.guardians.modules.comms.entity.SmsLogEntry;
import com.guardians.modules.comms.repository.CallLogRepository;
import com.guardians.modules.comms.repository.SmsLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comms")
@RequiredArgsConstructor
@Tag(name = "Communications")
@SecurityRequirement(name = "bearerAuth")
public class CommsController {

    private final CallLogRepository callLogRepository;
    private final SmsLogRepository  smsLogRepository;

    // ── Calls ────────────────────────────────────────────────────────────────

    /**
     * Child reports its call log (replaces existing data for this device).
     * Body: { "calls": [ { "number", "name", "callType", "durationSeconds", "timestamp" } ] }
     */
    @PostMapping("/{deviceId}/calls")
    @Transactional
    @Operation(summary = "Child reports call log (replaces existing)")
    public ResponseEntity<Map<String, Object>> reportCalls(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> calls = (List<Map<String, Object>>) body.get("calls");
        if (calls == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "calls list is required"));
        }

        callLogRepository.deleteByDeviceId(deviceId);

        Instant now = Instant.now();
        List<CallLogEntry> toSave = calls.stream()
                .map(c -> CallLogEntry.builder()
                        .deviceId(deviceId)
                        .number(str(c, "number"))
                        .name(str(c, "name"))
                        .callType(str(c, "callType", "UNKNOWN"))
                        .durationSeconds(num(c, "durationSeconds"))
                        .timestamp(numLong(c, "timestamp"))
                        .createdAt(now)
                        .build())
                .collect(Collectors.toList());

        callLogRepository.saveAll(toSave);

        return ResponseEntity.ok(Map.of("status", "received", "count", toSave.size()));
    }

    /**
     * Parent reads the child's call log.
     * Returns: { "calls": [ ... ] }
     */
    @GetMapping("/{deviceId}/calls")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Parent reads child call log")
    public ResponseEntity<Map<String, Object>> getCalls(@PathVariable String deviceId) {
        List<Map<String, Object>> calls = callLogRepository
                .findByDeviceIdOrderByTimestampDesc(deviceId)
                .stream()
                .map(c -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("number",          c.getNumber() != null ? c.getNumber() : "");
                    m.put("name",            c.getName() != null ? c.getName() : "");
                    m.put("callType",        c.getCallType() != null ? c.getCallType() : "UNKNOWN");
                    m.put("durationSeconds", c.getDurationSeconds());
                    m.put("timestamp",       c.getTimestamp());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("calls", calls));
    }

    // ── SMS ──────────────────────────────────────────────────────────────────

    /**
     * Child reports its SMS log (replaces existing data for this device).
     * Body: { "messages": [ { "address", "body", "smsType", "timestamp" } ] }
     */
    @PostMapping("/{deviceId}/sms")
    @Transactional
    @Operation(summary = "Child reports SMS log (replaces existing)")
    public ResponseEntity<Map<String, Object>> reportSms(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        if (messages == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "messages list is required"));
        }

        smsLogRepository.deleteByDeviceId(deviceId);

        Instant now = Instant.now();
        List<SmsLogEntry> toSave = messages.stream()
                .map(m -> SmsLogEntry.builder()
                        .deviceId(deviceId)
                        .address(str(m, "address"))
                        .body(str(m, "body"))
                        .smsType(str(m, "smsType", "OTHER"))
                        .timestamp(numLong(m, "timestamp"))
                        .createdAt(now)
                        .build())
                .collect(Collectors.toList());

        smsLogRepository.saveAll(toSave);

        return ResponseEntity.ok(Map.of("status", "received", "count", toSave.size()));
    }

    /**
     * Parent reads the child's SMS log.
     * Returns: { "messages": [ ... ] }
     */
    @GetMapping("/{deviceId}/sms")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Parent reads child SMS log")
    public ResponseEntity<Map<String, Object>> getSms(@PathVariable String deviceId) {
        List<Map<String, Object>> messages = smsLogRepository
                .findByDeviceIdOrderByTimestampDesc(deviceId)
                .stream()
                .map(m -> {
                    Map<String, Object> sm = new java.util.LinkedHashMap<>();
                    sm.put("address",   m.getAddress() != null ? m.getAddress() : "");
                    sm.put("body",      m.getBody() != null ? m.getBody() : "");
                    sm.put("smsType",   m.getSmsType() != null ? m.getSmsType() : "OTHER");
                    sm.put("timestamp", m.getTimestamp());
                    return sm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("messages", messages));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String str(Map<String, Object> m, String key) {
        return str(m, key, "");
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v instanceof String s ? s : def;
    }

    private static int num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static long numLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }
}
