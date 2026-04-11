package com.busping.global.exception;

import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.custom.ExternalApiException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import com.busping.global.exception.errorcode.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러에서 발생하는 예외를 전역에서 처리
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     *
     * 로그인 실패, 중복 이메일 등 서비스 계층에서 던지는 예외
     * → ErrorCode에 정의된 HTTP 상태코드와 메시지로 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e
    ) {
        log.warn("[Business Error] {}", e.getMessage());

        return handleExceptionInternal(e.getErrorCode());
    }

    /**
     * 외부 API 호출 중 발생한 예외 처리
     *
     * ExternalApiException은 내부적으로 ErrorCode를 가지고 있음.
     * → 해당 ErrorCode를 기반으로 ErrorResponse 생성
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiException(
            ExternalApiException e
    ) {
        log.error("[External API Error] {}", e.getMessage(), e);

        return handleExceptionInternal(e.getErrorCode());
    }

    /**
     * @Valid 검증 실패 시 처리
     *
     * DTO 유효성 검사 실패하면 MethodArgumentNotValidException 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        e.getBindingResult().getFieldErrors().forEach(error ->
                log.warn("[Validation Error] field={}, value={}, message={}",
                        error.getField(), error.getRejectedValue(), error.getDefaultMessage())
        );

        return handleExceptionInternal(
                CommonErrorCode.VALIDATION_FAILED
        );
    }

    /**
     * 처리되지 않은 모든 예외의 최종 방어선
     *
     * 위에서 정의하지 않은 예외가 발생하면 여기로 들어온다.
     * → 서버 내부 오류로 간주하고 500 응답 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ) {
        log.error("[Unhandled Exception]", e);

        return handleExceptionInternal(
                CommonErrorCode.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * 공통 에러 응답 생성 메서드
     *
     * 1. ErrorCode에서 HTTP 상태 코드 추출
     * 2. ErrorCode에서 code, message 추출
     * 3. ErrorResponse 생성
     */
    private ResponseEntity<ErrorResponse> handleExceptionInternal(
            ErrorCode errorCode
    ) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }


}