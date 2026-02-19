package com.station.alarm.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> {

    private int status;
    private String message;
    private T data;

    public static <T> ResponseEntity<SuccessResponse<T>> of(
            HttpStatus status,
            String message,
            T data
    ) {
        SuccessResponse<T> body = new SuccessResponse<>(
                status.value(),
                message,
                data
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}