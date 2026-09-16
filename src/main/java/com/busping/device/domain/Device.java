package com.busping.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 익명 디바이스 - 회원가입/로그인 없이 앱 설치 단위로 식별한다. 위치는 저장하지 않는다.
 */
@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Device {

    /** 서버가 생성하는 UUID - 클라이언트는 등록 응답으로 받아 보관 */
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** FCM 푸시 토큰 - 등록 이후 별도 API로 갱신되므로 null 허용 */
    @Column(length = 500)
    private String fcmToken;

    /** 알람 수신 여부 - 생성 시 기본 on */
    @Column(nullable = false)
    private boolean alarmEnabled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 유일한 생성 경로 - 알람 기본 on */
    public static Device create() {
        return Device.builder()
                .alarmEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateAlarmEnabled(boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
    }
}
