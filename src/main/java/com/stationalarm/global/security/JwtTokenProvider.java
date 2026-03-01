package com.stationalarm.global.security;

import com.stationalarm.global.exception.custom.BusinessException;
import com.stationalarm.global.exception.errorcode.CommonErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

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

                // 암호화 알고리즘과 Secret Key 설정: 서버만 아는 키로 서명(Signature) 생성
                // 이 서명 덕분에 클라이언트가 토큰 내용을 변조해도 서버가 알아챌 수 있음
                .signWith(key, SignatureAlgorithm.HS512)

                // 만료 시간 설정: 보안을 위해 토큰의 유효 기간을 제한
                .setExpiration(validity)

                // 빌드 및 압축: 위 설정값들을 Header.Payload.Signature 형태로 인코딩하여 반환
                .compact();
    }

    /**
     * 토큰의 Subject를 추출하는 메서드
     */
    public String getSubject(String token) {

        // 1. 토큰 파싱 (복호화)
        Claims claims = parseClaims(token);

        // userId 추출
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return subject;
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


    /**
     * 서버가 가진 비밀키(key)를 이용해 토큰을 열고 내부 데이터(Claims)를 추출함
     * 만약 위조된 토큰이라면 이 단계에서 예외가 발생함
     * @param token
     * @return
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

}
