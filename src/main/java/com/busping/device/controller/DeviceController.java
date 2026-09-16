package com.busping.device.controller;

import com.busping.device.domain.Device;
import com.busping.device.dto.DeviceRegisterResponse;
import com.busping.device.dto.FcmTokenRequest;
import com.busping.device.service.DeviceService;
import com.busping.global.common.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    /** 디바이스 등록 - 요청 본문 없음, 응답으로 UUID 발급 */
    @PostMapping
    public ResponseEntity<SuccessResponse<DeviceRegisterResponse>> register() {
        return SuccessResponse.of(
                HttpStatus.CREATED,
                "디바이스가 등록되었습니다.",
                deviceService.register()
        );
    }

    /** FCM 토큰 갱신 - 기존 PATCH /api/users/fcm-token에서 이동 */
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<SuccessResponse<Void>> updateFcmToken(
            Device device,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        deviceService.updateFcmToken(device.getId(), request.fcmToken());

        return SuccessResponse.of(
                HttpStatus.OK,
                "FCM 토큰이 등록되었습니다."
        );
    }
}
