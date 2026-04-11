package com.busping.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    /**
     * 이메일 또는 비밀번호가 일치하지 않는 경우
     */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U001", "이메일 또는 비밀번호가 올바르지 않습니다."),

    /**
     * 이미 가입된 이메일로 회원가입 시도
     */
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
