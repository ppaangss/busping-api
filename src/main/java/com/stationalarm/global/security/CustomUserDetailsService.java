package com.stationalarm.global.security;

import com.stationalarm.user.domain.User;
import com.stationalarm.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * JWT 토큰의 subject(userId)를 기반으로 사용자를 조회한다.
     *
     * Spring Security에서 인증 객체(Authentication)를 생성할 때 호출되며,
     * 전달받은 userId 문자열을 Long으로 변환한 후 DB에서 사용자를 조회한다.
     *
     * 사용자가 존재하지 않거나 형식이 올바르지 않은 경우
     * UsernameNotFoundException을 발생시켜 인증을 실패시킨다.
     *
     * @param userIdString JWT의 subject 값 (문자열 형태의 userId)
     * @return SecurityContext에 저장될 CustomUserDetails 객체
     */
    @Override
    public UserDetails loadUserByUsername(String userIdString) {

        Long userId;
        try {
            userId = Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("잘못된 사용자 ID");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return new CustomUserDetails(user);
    }
}