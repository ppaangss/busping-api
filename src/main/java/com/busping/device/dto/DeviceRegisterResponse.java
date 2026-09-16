package com.busping.device.dto;

import com.busping.device.domain.Device;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceRegisterResponse {

    private UUID deviceId;

    /** Device 엔티티를 DeviceRegisterResponse로 변환 */
    public static DeviceRegisterResponse from(Device device) {
        return new DeviceRegisterResponse(device.getId());
    }
}
