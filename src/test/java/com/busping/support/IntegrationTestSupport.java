package com.busping.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * 통합 테스트 공통 베이스.
 * - Testcontainers로 실 MySQL·Redis를 띄운다 (로컬 개발용 컨테이너와 별개, 테스트 후 자동 폐기)
 * - static 싱글톤 컨테이너 - 모든 테스트 클래스가 공유해서 기동 비용을 1회만 낸다
 * - dev 프로파일 - TAGO·FCM이 fake라 외부 호출 없이 전체 플로우가 돈다
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }
}
