package com.busping.global.external.fcm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * FCM 실 발송 대체 - 발송 내용을 로그로만 남긴다 (Firebase 키 불필요)
 */
@Slf4j
@Profile("dev")
@Primary
@Service
public class FakeFcmService implements FcmPort {

    @Override
    public void send(String fcmToken, String title, String body) {
        log.info("[FCM Fake] 발송 - title={}, body={}", title, body);
    }
}
