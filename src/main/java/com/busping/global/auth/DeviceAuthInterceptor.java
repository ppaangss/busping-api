package com.busping.global.auth;

import com.busping.device.domain.Device;
import com.busping.device.domain.DeviceRepository;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.DeviceErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * X-Device-Id 헤더를 검증하는 인터셉터 - 등록된 디바이스만 통과시킨다.
 * 통과 시 조회한 Device를 request attribute에 담아 ArgumentResolver가 재사용한다 (조회 1회).
 */
@Component
@RequiredArgsConstructor
public class DeviceAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Device-Id";
    public static final String DEVICE_ATTRIBUTE = "authenticatedDevice";

    private final DeviceRepository deviceRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String headerValue = request.getHeader(HEADER_NAME);

        // 헤더 없음 / UUID 형식 아님 / 미등록 - 전부 같은 401 (NFR-5: UUID는 로그에 남기지 않는다)
        if (headerValue == null) {
            throw new BusinessException(DeviceErrorCode.UNREGISTERED_DEVICE);
        }

        UUID deviceId = parseUuid(headerValue);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(DeviceErrorCode.UNREGISTERED_DEVICE));

        request.setAttribute(DEVICE_ATTRIBUTE, device);
        return true;
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(DeviceErrorCode.UNREGISTERED_DEVICE);
        }
    }
}
