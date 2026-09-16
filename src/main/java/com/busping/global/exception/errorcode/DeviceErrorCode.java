package com.busping.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeviceErrorCode implements ErrorCode {

    /**
     * X-Device-Id 헤더가 없거나, UUID 형식이 아니거나, 등록되지 않은 디바이스인 경우
     * (원인을 구분해 노출하지 않는다 - UUID 추측 시도에 힌트를 주지 않기 위함)
     */
    UNREGISTERED_DEVICE(HttpStatus.UNAUTHORIZED, "D001", "등록되지 않은 디바이스입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
