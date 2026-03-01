package com.stationalarm.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    /**
     * 잘못된 요청 파라미터가 포함된 경우
     * - 필수값 누락
     * - 형식 오류
     * - 타입 불일치 등 일반적인 요청 오류
     */
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C001", "잘못된 파라미터가 포함되었습니다."),

    /**
     * 요청한 리소스를 찾을 수 없는 경우
     * - 존재하지 않는 ID 조회
     * - 삭제된 데이터 접근 등
     */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "리소스를 찾을 수 없습니다."),

    /**
     * 서버 내부 처리 중 예상하지 못한 예외 발생
     * - NullPointerException
     * - DB 예외
     * - 처리되지 않은 런타임 예외 등
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),

    /**
     * 허용되지 않은 HTTP 메서드 호출
     * - GET만 허용된 API에 POST 요청 등
     */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "허용되지 않은 HTTP 메서드입니다."),

    /**
     * 인증되지 않은 사용자가 접근한 경우
     * - JWT 없음
     * - 토큰 검증 실패 등
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C005", "인증되지 않은 사용자입니다."),

    /**
     * @Valid 검증 실패
     * - DTO 유효성 검사 실패 시 사용
     */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "C006", "요청 값 검증에 실패했습니다."),

    /**
     * 중복 데이터 생성 시도
     * - 이메일 중복
     * - 즐겨찾기 중복 등록 등
     */
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C007", "이미 존재하는 데이터입니다."),

    /**
     * 권한은 있으나 접근이 허용되지 않는 경우
     * - 다른 사용자의 리소스 접근 시도 등
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "C008", "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;  // HTTP 상태 코드
    private final String code;            // 서비스 내부 에러 코드
    private final String message;         // 기본 에러 메시지
}