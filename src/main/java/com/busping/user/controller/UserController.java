package com.busping.user.controller;


import com.busping.global.common.SuccessResponse;
import com.busping.user.dto.login.LoginRequest;
import com.busping.user.dto.login.LoginResponse;
import com.busping.user.dto.signup.SignupRequest;
import com.busping.user.dto.signup.SignupResponse;
import com.busping.user.service.UserService;
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
    public ResponseEntity<SuccessResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return SuccessResponse.of(
                HttpStatus.OK,
                "로그인이 완료되었습니다.",
                userService.login(request)
        );
    }
}
