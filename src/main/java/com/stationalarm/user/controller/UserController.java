package com.stationalarm.user.controller;


import com.stationalarm.global.common.SuccessResponse;
import com.stationalarm.user.dto.login.LoginRequest;
import com.stationalarm.user.dto.login.LoginResponse;
import com.stationalarm.user.dto.signup.SignupRequest;
import com.stationalarm.user.dto.signup.SignupResponse;
import com.stationalarm.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(userService.login(request));
    }
}
