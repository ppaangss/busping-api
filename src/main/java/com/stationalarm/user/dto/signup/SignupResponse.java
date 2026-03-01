package com.stationalarm.user.dto.signup;

import com.stationalarm.user.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignupResponse {

    private Long userId;
    private String email;

    /**
     * User 엔티티를 SignupResponse로 변환
     */
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail()
        );
    }
}
