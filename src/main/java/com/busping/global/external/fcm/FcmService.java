package com.busping.global.external.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void send(String fcmToken, String title, String body) {
        // Firebase 미초기화(자격증명 없는 로컬 환경)면 발송하지 않는다
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] Firebase 미초기화 - 발송 스킵 (title={})", title);
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
        try {
            FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] 발송 완료 - title={}, body={}", title, body);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 발송 실패 - {}", e.getMessage());
        }
    }
}
