package com.station.alarm.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagoErrorCode implements ErrorCode {
    COMMUNICATION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "TAGO_001", "외부 API 통신 실패"),
    PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TAGO_002", "정류장 정보 파싱 실패"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "TAGO_003", "외부 API 요청 오류"),
    SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "TAGO_004", "외부 API 서버 오류"),
    RETRY_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "TAGO_005", "외부 API 재시도 후 최종 실패");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
