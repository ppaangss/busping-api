package com.busping.alarm.domain;

/**
 * 알림 발송 대상 후보 객체
 *
 * - 아직 쿨타임 검사를 통과하지 않은 상태
 * - FCM 전송 및 알림 기록 저장에 사용
 */
public record AlarmCandidate(

        Long userId,

        String cityCode,
        String stationId,
        String stationName,

        String routeId,
        String busNumber,

        int remainingMinutes,
        int remainingStops

) {
}
