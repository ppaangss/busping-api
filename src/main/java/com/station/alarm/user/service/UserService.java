package com.station.alarm.user.service;


import com.station.alarm.global.security.JwtTokenProvider;
import com.station.alarm.global.exception.custom.BusinessException;
import com.station.alarm.global.exception.errorcode.CommonErrorCode;
import com.station.alarm.user.domain.User;
import com.station.alarm.user.domain.UserRepository;
import com.station.alarm.user.dto.login.LoginRequest;
import com.station.alarm.user.dto.login.LoginResponse;
import com.station.alarm.user.dto.signup.SignupRequest;
import com.station.alarm.user.dto.signup.SignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입 처리
     *
     * 1. 이메일 중복 검사
     * 2. 비밀번호 암호화
     * 3. 사용자 저장
     * 4. SignupResponse 반환
     */
    public SignupResponse signup(SignupRequest request) {

        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 사용자 생성 및 저장
        User user = new User(
                request.getEmail(),
                encodedPassword
        );

        User savedUser = userRepository.save(user);

        // 4. 응답 DTO 반환
        return SignupResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BusinessException(CommonErrorCode.UNAUTHORIZED)
                );

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        String accessToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(
                accessToken,
                user.getId(),
                user.getEmail()
        );
    }



}
