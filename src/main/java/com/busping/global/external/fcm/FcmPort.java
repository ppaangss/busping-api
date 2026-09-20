package com.busping.global.external.fcm;

/**
 * 푸시 알림 발송 포트 - dev는 Fake, staging/prod는 Firebase 실 발송
 */
public interface FcmPort {

    void send(String fcmToken, String title, String body);
}
