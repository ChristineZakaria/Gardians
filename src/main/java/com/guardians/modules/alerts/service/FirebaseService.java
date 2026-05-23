package com.guardians.modules.alerts.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FirebaseService {

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    public void sendPushNotification(String fcmToken, String title,
                                     String body, Map<String, Object> data) {
        if (!firebaseEnabled || firebaseMessaging == null
                || fcmToken == null || fcmToken.isBlank()) {
            log.info("[FCM-MOCK] Push skipped — title='{}' body='{}'", title, body);
            return;
        }
        try {
            Message msg = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title).setBody(body).build())
                    .putAllData(toStringMap(data))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            String msgId = firebaseMessaging.send(msg);
            log.info("[FCM] Sent: {}", msgId);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Send failed: {}", e.getMessage());
        }
    }

    public void sendDataMessage(String fcmToken, String command, Map<String, Object> data) {
        if (!firebaseEnabled || firebaseMessaging == null
                || fcmToken == null || fcmToken.isBlank()) {
            log.info("[FCM-MOCK] Data message skipped — command='{}'", command);
            return;
        }
        try {
            Message msg = Message.builder()
                    .setToken(fcmToken)
                    .putData("command", command)
                    .putAllData(toStringMap(data))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            firebaseMessaging.send(msg);
            log.info("[FCM] Data message sent: command={}", command);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Data message failed: {}", e.getMessage());
        }
    }

    private Map<String, String> toStringMap(Map<String, Object> data) {
        if (data == null) return Map.of();
        return data.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> String.valueOf(e.getValue())));
    }
}
