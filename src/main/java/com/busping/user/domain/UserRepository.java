package com.busping.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /**
     * lastLocationUpdatedAt 값이 파라미터 time 이후인 유저를 조회
     * 활성 유저 기준 (최근 5분 이내 위치 갱신 유저) 조회에 사용
     */
    List<User> findByLastLocationUpdatedAtAfter(LocalDateTime time);
    // Spring Data는 엔티티 필드 타입과 메서드 파라미터 타입이 정확히 일치해야 한다..
    // lastLocationUpdatedAt의 타입과 파라미터 타입 LocalDataTime이 일치해야함.

}

