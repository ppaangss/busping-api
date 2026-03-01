package com.stationalarm.favorite.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FavoriteCreateRequest {

    // ===== 정류장 정보 =====

    @NotBlank(message = "정류장 ID는 필수입니다.")
    @Size(max = 50, message = "정류장 ID는 50자 이하여야 합니다.")
    private String stationId;

    @NotBlank(message = "정류장 이름은 필수입니다.")
    @Size(max = 100, message = "정류장 이름은 100자 이하여야 합니다.")
    private String stationName;

    @NotBlank(message = "지역 코드는 필수입니다.")
    @Size(max = 20, message = "지역 코드는 20자 이하여야 합니다.")
    private String regionCode;

    @DecimalMin(value = "-90.0", message = "위도 범위가 올바르지 않습니다.")
    @DecimalMax(value = "90.0", message = "위도 범위가 올바르지 않습니다.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도 범위가 올바르지 않습니다.")
    @DecimalMax(value = "180.0", message = "경도 범위가 올바르지 않습니다.")
    private Double longitude;

    // ===== 노선 정보 =====

    @NotBlank(message = "노선 ID는 필수입니다.")
    @Size(max = 50, message = "노선 ID는 50자 이하여야 합니다.")
    private String routeId;

    @NotBlank(message = "노선 이름은 필수입니다.")
    @Size(max = 50, message = "노선 이름은 50자 이하여야 합니다.")
    private String routeName;
}