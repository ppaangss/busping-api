package com.station.alarm.global.exception;

import com.station.alarm.global.exception.errorcode.ErrorCode;

public class RetryableExternalApiException extends ExternalApiException {
    public RetryableExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }
}
