package com.station.alarm.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class FavoriteCreateRequest {

    @NotBlank(message = "정류장 ID는 필수입니다.")
    @Size(max = 50)
    private String stationId;

    @NotBlank(message = "정류장 이름은 필수입니다.")
    @Size(max = 100)
    private String stationName;

    @NotBlank(message = "노선 ID는 필수입니다.")
    @Size(max = 50)
    private String routeId;

    @NotBlank(message = "버스 번호는 필수입니다.")
    @Size(max = 20)
    private String busNumber;

    @NotBlank(message = "지역 코드는 필수입니다.")
    @Size(max = 20)
    private String regionCode;
}