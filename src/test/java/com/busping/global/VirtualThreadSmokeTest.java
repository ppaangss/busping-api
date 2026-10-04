package com.busping.global;

import com.busping.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * 가상 스레드 스모크 테스트 - MockMvc는 톰캣을 안 거치므로 실제 서버(RANDOM_PORT)로 요청을 보내
 * 요청 처리 스레드가 가상 스레드인지 확인한다.
 * spring.threads.virtual.enabled가 꺼지면 깨진다 - 설정 회귀 가드.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadSmokeTest.ThreadProbe.class)
class VirtualThreadSmokeTest extends IntegrationTestSupport {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void 요청은_가상_스레드에서_처리된다() {
        String body = restTemplate.getForObject("/test/thread-probe", String.class);

        assertThat(body).startsWith("virtual=true");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ThreadProbe {

        @Bean
        RouterFunction<ServerResponse> threadProbeRoute() {
            return route(GET("/test/thread-probe"), req -> {
                Thread t = Thread.currentThread();
                return ServerResponse.ok().body("virtual=" + t.isVirtual() + " thread=" + t);
            });
        }
    }
}
