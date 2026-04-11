package com.busping.alarm.user.service;

import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import com.busping.global.security.JwtTokenProvider;
import com.busping.user.domain.User;
import com.busping.user.dto.login.LoginRequest;
import com.busping.user.dto.login.LoginResponse;
import com.busping.user.dto.signup.SignupRequest;
import com.busping.user.dto.signup.SignupResponse;
import com.busping.user.domain.UserRepository;
import com.busping.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("회원가입")
    class SignupTest {

        @Test
        @DisplayName("정상 처리 테스트")
        void given_validRequest_when_signup_then_success() {

            // given
            SignupRequest request = new SignupRequest("test@test.com", "password123");

            given(userRepository.existsByEmail(request.getEmail()))
                    .willReturn(false);

            given(passwordEncoder.encode(request.getPassword()))
                    .willReturn("encodedPassword");

            User savedUser = User.create("test@test.com", "encodedPassword");

            // id 세팅 가정
            // private로 설정되어있지만 테스트 환경에서 값을 임시로 넣어 사용
            // 실제 환경에서는 자동으로 들어가지만 테스트 환경에서는 불가능하기 때문이다.
            ReflectionTestUtils.setField(savedUser, "id", 1L);

            given(userRepository.save(any(User.class)))
                    .willReturn(savedUser);

            // when
            SignupResponse response = userService.signup(request);

            // then
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@test.com");

            then(userRepository).should().existsByEmail("test@test.com");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("이메일 중복이면 예외 발생")
        void given_duplicateEmail_when_signup_then_throwException() {

            // given
            SignupRequest request = new SignupRequest("test@test.com", "password123");

            // 이메일이 존재한다고 가정
            given(userRepository.existsByEmail(request.getEmail()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(CommonErrorCode.DUPLICATE_RESOURCE.getMessage());

            then(userRepository).should().existsByEmail("test@test.com");
            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("요청값이 null이면 예외 발생")
        void given_nullRequest_when_signup_then_throwException() {

            // when & then
            assertThatThrownBy(() -> userService.signup(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ============================
    // 로그인 테스트
    // ============================
    @Nested
    @DisplayName("로그인")
    class LoginTest {

        @Test
        @DisplayName("정상 처리 테스트")
        void given_validRequest_when_login_then_returnLoginResponse() {

            // given
            LoginRequest request = new LoginRequest("test@test.com", "1234");

            User user = User.create("test@test.com", "encodedPassword");
            ReflectionTestUtils.setField(user, "id", 1L);

            // 이메일로 사용자 조회 성공
            given(userRepository.findByEmail(request.getEmail()))
                    .willReturn(Optional.of(user));

            // 비밀번호 일치
            given(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .willReturn(true);

            // JWT 토큰 발급
            given(jwtTokenProvider.createToken(user.getId(), user.getEmail()))
                    .willReturn("accessToken");

            // when
            LoginResponse response = userService.login(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@test.com");
        }


        @Test
        @DisplayName("이메일이 존재하지 않으면 예외 발생")
        void given_notExistEmail_when_login_then_throwBusinessException() {

            // given
            LoginRequest request = new LoginRequest("none@test.com", "1234");

            given(userRepository.findByEmail(request.getEmail()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED);
        }


        @Test
        @DisplayName("비밀번호 불일치하면 예외 발생")
        void given_wrongPassword_when_login_then_throwBusinessException() {

            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrong");

            User user = User.create("test@test.com", "encodedPassword");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByEmail(request.getEmail()))
                    .willReturn(Optional.of(user));

            given(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.UNAUTHORIZED);
        }


        @Test
        @DisplayName("요청값이 null이면 예외 발생")
        void given_nullRequest_when_login_then_throwException() {

            // when & then
            assertThatThrownBy(() -> userService.login(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}