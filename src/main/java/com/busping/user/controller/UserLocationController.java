package com.busping.user.controller;

import com.busping.global.common.SuccessResponse;
import com.busping.global.security.CustomUserDetails;
import com.busping.user.dto.location.LocationUpdateRequest;
import com.busping.user.service.UserLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserLocationController {

    private final UserLocationService userLocationService;

    @PostMapping("/location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SuccessResponse<Void>> updateLocation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LocationUpdateRequest request
    ) {

        userLocationService.updateLocation(
                userDetails.getId(),
                request.getLatitude(),
                request.getLongitude()
        );

        return SuccessResponse.of(
                HttpStatus.CREATED,
                "현재 위치 정보가 저장되었습니다."
        );
    }
}
