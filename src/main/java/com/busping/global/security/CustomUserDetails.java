package com.busping.global.security;

import com.busping.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    // private final Collection<? extends GrantedAuthority> authorities; // 권한 정보

    public CustomUserDetails(User user
                             //Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        // this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // 현재 권한 구조 없음
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;  // 로그인 기준이 이메일이면 email 반환
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // 추후 user.isActive()로 변경 가능
    }
}