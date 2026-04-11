package com.busping.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.busping.global.exception.errorcode.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;     // HTTP 상태코드
    private String message; // 사용자에게 보여줄 메시지

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getMessage()
        );
    }
}
