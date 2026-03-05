package com.stationalarm.user.controller;

import com.stationalarm.global.common.SuccessResponse;
import com.stationalarm.global.security.CustomUserDetails;
import com.stationalarm.user.dto.location.LocationUpdateRequest;
import com.stationalarm.user.service.UserLocationService;
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
                "ㅁㄴㅇ"
        );
    }
}
