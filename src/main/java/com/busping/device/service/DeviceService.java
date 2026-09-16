package com.busping.device.service;

import com.busping.device.domain.Device;
import com.busping.device.domain.DeviceRepository;
import com.busping.device.dto.DeviceRegisterResponse;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.DeviceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;

    /**
     * 디바이스 등록 - 서버가 UUID를 생성해 반환 (무인증, NFR-5: UUID 로그 비노출)
     */
    public DeviceRegisterResponse register() {
        Device savedDevice = deviceRepository.save(Device.create());
        return DeviceRegisterResponse.from(savedDevice);
    }

    /**
     * FCM 토큰 갱신 - 인터셉터가 조회한 Device는 준영속 상태라 트랜잭션 안에서 다시 조회한다
     */
    public void updateFcmToken(UUID deviceId, String fcmToken) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(DeviceErrorCode.UNREGISTERED_DEVICE));

        device.updateFcmToken(fcmToken);
    }
}
