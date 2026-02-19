package com.station.alarm.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    // JWT를 암호화/복호화할 때 쓰는 비밀키
    // 유출되면 안 됨으로 데이터 숨겨야함.
    private final Key key;

    // 토큰 유지 기간
    private final long tokenValidityInMilliseconds;

    // 환경변수에서 키와 유효기간을 모두 주입받음
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity}") long tokenValidityInMilliseconds
    ) {
        // HMAC SHA256 서명용 Key 객체로 만들어줌.
        // key를 이용해 JWT의 서명과 검증 수행
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
    }

    /**
     * 인증 성공 시 토큰 생성 (이론: Authentication 객체를 Payload로 변환)
     */
    public String createToken(Long userId, String email) {

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        // 최종적인 JWT 토큰 생성 및 빌드
        return Jwts.builder()
                // 토큰의 제목(Subject) 설정: 보통 유저의 ID나 이메일 등 식별할 수 있는 값을 넣음
                .setSubject(String.valueOf(userId))

                // 커스텀 데이터(Claim) 추가: 추출한 권한 문자열을 "auth"라는 키로 저장
                // 나중에 토큰을 파싱할 때 이 "auth" 키를 보고 사용자의 권한을 복구함
                .claim("email", email)

                // 암호화 알고리즘과 Secret Key 설정: 서버만 아는 키로 서명(Signature) 생성
                // 이 서명 덕분에 클라이언트가 토큰 내용을 변조해도 서버가 알아챌 수 있음
                .signWith(key, SignatureAlgorithm.HS512)

                // 만료 시간 설정: 보안을 위해 토큰의 유효 기간을 제한
                .setExpiration(validity)

                // 빌드 및 압축: 위 설정값들을 Header.Payload.Signature 형태로 인코딩하여 반환
                .compact();
    }

    /**
     * JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내고,
     * 이를 바탕으로 Spring Security의 인증 객체(Authentication)를 생성하는 메서드
     */
    public Authentication getAuthentication(String token) {

        // 1. 토큰 파싱 (복호화)
        // 서버가 가진 비밀키(key)를 이용해 토큰을 열고 내부 데이터(Claims)를 추출함
        // 만약 위조된 토큰이라면 이 단계에서 예외가 발생함
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // 2. 권한 정보 추출 및 복구 (역직렬화)
        // "ROLE_USER,ROLE_ADMIN" 형태의 문자열을 다시 객체 리스트(Collection)로 변환
        // Arrays.stream(...) 등을 사용하여 문자열을 잘라 SimpleGrantedAuthority 객체로 재생성함
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // 3. UserDetails 객체 생성 (사용자 식별 정보)
        // Spring Security가 제공하는 User 객체를 생성하여 유저 ID(Subject)와 권한을 담음
        // 비밀번호는 이미 토큰으로 인증을 대신하므로 빈 문자열("")로 처리
        User principal = new User(claims.getSubject(), "", authorities);

        // 4. 최종 인증 토큰 반환
        // 인증된 사용자 정보(principal)와 권한(authorities)을 포함한 인증 객체를 리턴함
        // 이 객체는 이후 SecurityContextHolder에 저장되어 전역적으로 사용됨
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * 토큰의 유효성을 검증하는 메서드
     * @param token 검증할 JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            // 1. 토큰을 파싱하며 서명과 유효 기간을 검증
            // 서버의 key를 사용해 토큰의 서명을 대조함
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 아무 예외도 발생하지 않으면 유효한 토큰

        } catch (SecurityException | MalformedJwtException e) {
            // 위조되거나 형식이 잘못된 경우
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            // 유효 기간(exp)이 끝난 경우
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 구조의 토큰인 경우
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            // 토큰이 비어있거나 잘못된 인자가 전달된 경우
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false; // 위 케이스 중 하나라도 걸리면 유효하지 않음
    }


}
