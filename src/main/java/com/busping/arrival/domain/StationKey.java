package com.busping.arrival.domain;

/**
 * 정류장 식별을 위한 복합 키
 * (cityCode + stationId)
 *
 * TAGO API 호출의 고유 식별 단위
 */
public record StationKey(
        String cityCode,
        String stationId
) {
}