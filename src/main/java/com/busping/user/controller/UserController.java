package com.busping.user.controller;


import com.busping.global.common.SuccessResponse;
import com.busping.global.security.CustomUserDetails;
import com.busping.user.dto.fcm.FcmTokenRequest;
import com.busping.user.dto.login.LoginRequest;
import com.busping.user.dto.login.LoginResponse;
import com.busping.user.dto.signup.SignupRequest;
import com.busping.user.dto.signup.SignupResponse;
import com.busping.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return SuccessResponse.of(
                HttpStatus.CREATED,
                "회원가입이 완료되었습니다.",
                userService.signup(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return SuccessResponse.of(
                HttpStatus.OK,
                "로그인이 완료되었습니다.",
                userService.login(request)
        );
    }

    @PatchMapping("/users/fcm-token")
    public ResponseEntity<SuccessResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        userService.updateFcmToken(userDetails.getId(), request.fcmToken());
        return SuccessResponse.of(HttpStatus.OK, "FCM 토큰이 등록되었습니다.", null);
    }
}
