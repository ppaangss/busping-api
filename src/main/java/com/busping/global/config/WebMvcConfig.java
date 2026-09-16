package com.busping.global.config;

import com.busping.global.auth.DeviceArgumentResolver;
import com.busping.global.auth.DeviceAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DeviceAuthInterceptor deviceAuthInterceptor;
    private final DeviceArgumentResolver deviceArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deviceAuthInterceptor)
                .addPathPatterns("/api/**")
                // 디바이스 등록은 UUID 발급 전이므로 무인증
                .excludePathPatterns("/api/devices")
                // 레거시 인증 경로 - Security 철거 시 엔드포인트와 함께 삭제
                .excludePathPatterns("/api/signup", "/api/login", "/api/auth/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(deviceArgumentResolver);
    }
}
