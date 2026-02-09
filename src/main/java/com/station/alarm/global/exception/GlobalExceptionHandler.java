package com.station.alarm.global.exception;

import com.station.alarm.global.exception.errorcode.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * API 에러 
     * @param e 예외종류
     * @return Object
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<Object> handleExternalApiException(
            ExternalApiException e
    ) {
        log.error("[External API Error] {}", e.getMessage(), e);

        ErrorCode errorCode = e.getErrorCode();

        return handleExceptionInternal(errorCode);
    }

    /**
     * 내부적으로 ErrorResponse를 만드는 공통 메서드
     * */
    private ResponseEntity<Object> handleExceptionInternal(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponse(errorCode));
    }

    private ErrorResponse makeErrorResponse(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}