package com.stationalarm.user.service;


import com.stationalarm.global.security.JwtTokenProvider;
import com.stationalarm.global.exception.custom.BusinessException;
import com.stationalarm.global.exception.errorcode.UserErrorCode;
import com.stationalarm.user.domain.User;
import com.stationalarm.user.domain.UserRepository;
import com.stationalarm.user.dto.login.LoginRequest;
import com.stationalarm.user.dto.login.LoginResponse;
import com.stationalarm.user.dto.signup.SignupRequest;
import com.stationalarm.user.dto.signup.SignupResponse;
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
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 사용자 생성 및 저장
        User user = User.create(
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
                        new BusinessException(UserErrorCode.INVALID_CREDENTIALS)
                );

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
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
