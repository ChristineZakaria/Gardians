package com.guardians.modules.reports;

import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import com.guardians.shared.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendReportEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        User parent = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        String toEmail = parent.getEmail();
        String deviceName = body.getOrDefault("deviceName", "Child Device").toString();
        String dateStr    = body.getOrDefault("date", "").toString();
        String todayTime  = body.getOrDefault("todayTime", "0h 0m").toString();
        String weekTime   = body.getOrDefault("weekTime", "0h 0m").toString();
        int urlThreats    = toInt(body.get("urlThreats"));
        int textThreats   = toInt(body.get("textThreats"));
        int imageThreats  = toInt(body.get("imageThreats"));
        int videoThreats  = toInt(body.get("videoThreats"));
        int gameThreats   = toInt(body.get("gameThreats"));
        int totalThreats  = urlThreats + textThreats + imageThreats + videoThreats + gameThreats;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> apps = body.get("apps") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();

        String appLines = apps.isEmpty()
                ? "  No app data for today."
                : apps.stream().limit(10).map(a -> {
                    String name = a.getOrDefault("appName", a.getOrDefault("packageName", "?")).toString();
                    int mins = toInt(a.get("usageTimeMinutes"));
                    return "  • " + name + " — " + mins + "m";
                  }).reduce("", (a, b) -> a + "\n" + b);

        String subject = "Guardians Report – " + deviceName + " – " + dateStr;
        String emailBody = """
Guardians Daily Safety Report
Device: %s
Date: %s
━━━━━━━━━━━━━━━━━━━━━━

📱 Screen Time
  Today:      %s
  This week:  %s

🔒 Threats Detected Today
  🌐 Unsafe URLs:   %d
  💬 Text threats:  %d
  🖼 Image alerts:  %d
  🎬 Video alerts:  %d
  🎮 Game alerts:   %d
  Total:            %d

📊 Top Apps Used Today
%s

━━━━━━━━━━━━━━━━━━━━━━
Sent automatically by Guardians Parental Control
""".formatted(deviceName, dateStr, todayTime, weekTime,
              urlThreats, textThreats, imageThreats, videoThreats, gameThreats, totalThreats, appLines);

        emailService.sendSimpleEmail(toEmail, subject, emailBody);
        return ResponseEntity.ok(Map.of("sent", true, "to", toEmail));
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return 0;
    }
}
