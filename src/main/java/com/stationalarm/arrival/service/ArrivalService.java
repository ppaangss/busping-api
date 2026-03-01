package com.stationalarm.arrival.service;

import com.stationalarm.arrival.domain.Arrival;
import com.stationalarm.global.external.tago.arrival.TagoArrivalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArrivalService {
    private final TagoArrivalClient tagoArrivalClient;

    /**
     * 정류소 별 도착 정보 조회
     * @param cityCode
     * @param stationId
     * @return
     */
    public List<Arrival> getRealtimeArrivals(String cityCode, String stationId) {

        // 간단한 방어 로직
        if (!StringUtils.hasText(cityCode) || !StringUtils.hasText(stationId)) {
            throw new IllegalArgumentException("cityCode와 stationId는 필수입니다.");
        }

        return tagoArrivalClient.fetchRealtimeArrivals(cityCode, stationId);
    }

    /**
     * 즐겨찾기 해놓은 근처 도착 정보
     */






}
