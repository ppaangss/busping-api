package com.busping.device.service;

import com.busping.device.domain.Device;
import com.busping.device.domain.DeviceRepository;
import com.busping.device.dto.DeviceRegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
