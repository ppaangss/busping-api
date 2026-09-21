package com.busping.device.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTest {

    @Test
    @DisplayName("새 디바이스는 알람이 켜져 있다")
    void newDeviceHasAlarmEnabled() {
        Device device = Device.create();

        assertThat(device.isAlarmEnabled()).isTrue();
    }

    @Test
    @DisplayName("새 디바이스는 생성 시각을 갖는다")
    void newDeviceHasCreatedAt() {
        Device device = Device.create();

        assertThat(device.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("새 디바이스는 FCM 토큰이 없다 - 토큰은 등록 후 별도 API로 받는다")
    void newDeviceHasNoFcmToken() {
        Device device = Device.create();

        assertThat(device.getFcmToken()).isNull();
    }

    @Test
    @DisplayName("알람을 끄면 꺼진 상태가 된다")
    void updateAlarmEnabledTurnsOff() {
        Device device = Device.create();

        device.updateAlarmEnabled(false);

        assertThat(device.isAlarmEnabled()).isFalse();
    }

    @Test
    @DisplayName("FCM 토큰을 갱신하면 새 토큰으로 바뀐다")
    void updateFcmTokenReplacesToken() {
        Device   device = Device.create();

        device.updateFcmToken("new-token");

        assertThat(device.getFcmToken()).isEqualTo("new-token");
    }
}
