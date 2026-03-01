package com.stationalarm.global.exception.custom;

import com.stationalarm.global.exception.errorcode.ErrorCode;
import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ExternalApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}