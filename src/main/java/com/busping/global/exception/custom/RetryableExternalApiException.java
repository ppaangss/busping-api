package com.busping.global.exception.custom;

import com.busping.global.exception.errorcode.ErrorCode;

public class RetryableExternalApiException extends ExternalApiException {
    public RetryableExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }
}
