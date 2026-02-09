package com.station.alarm.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 응답에서 제외
public class ErrorResponse {
    private final int status;    // HTTP 상태 코드 값
    private final String code;      // ErrorCode에서 정의한 서비스 코드
    private final String message;   // 에러 상세 메시지

    // 유효성 검사 실패 시 어느 필드에서 에러가 났는지 담는 리스트
    private final List<FieldError> errors;

    @Getter
    @Builder
    @RequiredArgsConstructor
    public static class FieldError {
        private final String field;   // 에러가 발생한 필드명 (예: "email")
        private final String value;   // 사용자가 입력했던 값
        private final String reason;  // 에러 이유 (예: "이메일 형식이 아닙니다")

        // BindingResult를 FieldError 리스트로 변환하는 편의 메서드
        public static List<FieldError> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(error -> new FieldError(
                            error.getField(),
                            error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                            error.getDefaultMessage()))
                    .collect(Collectors.toList());
        }
    }
}