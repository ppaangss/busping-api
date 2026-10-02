package com.busping.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplateBuilder 경유로 생성 - 외부 호출(TAGO 등)이 http_client_requests 메트릭으로 자동 계측된다.
     * 타임아웃 정책은 기존과 동일 (연결 3초, 읽기 3초)
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)) // 서버에 연결이 안될 경우 3초 안에 끊어버리도록 설정
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(3));

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}