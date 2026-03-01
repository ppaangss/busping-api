package com.stationalarm.global.exception.custom;

import com.stationalarm.global.exception.errorcode.ErrorCode;

public class RetryableExternalApiException extends ExternalApiException {
    public RetryableExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }
}
