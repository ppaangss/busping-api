package com.stationalarm.global.config;

import com.stationalarm.global.security.CustomAccessDeniedHandler;
import com.stationalarm.global.security.CustomAuthenticationEntryPoint;
import com.stationalarm.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Spring Security의 핵심 설정 클래스
     * 보안 필터 체인의 순서와 인증/인가 규칙을 정의함
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. REST API이므로 기본 설정 해제
                .httpBasic(HttpBasicConfigurer::disable)
                .csrf(CsrfConfigurer::disable)

                // 2. 세션을 사용하지 않음 (JWT의 Stateless 특성)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 요청에 대한 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/signup").permitAll()
                        .requestMatchers("/api/login").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .anyRequest().authenticated()                     // 나머지는 인증이 필요함
                )

                // 4. JWT 필터를 필터 체인에 등록
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호 암호화 도구 등록
        return new BCryptPasswordEncoder();
    }
}
