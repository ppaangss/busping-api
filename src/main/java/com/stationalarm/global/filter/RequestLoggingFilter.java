package com.stationalarm.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] {} {} - {}ms",
                    response.getStatus(),
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsed);

//            if (elapsed > 500) {
//                log.warn("[SLOW API] [{}] {} {} - {}ms",
//                        response.getStatus(),
//                        request.getMethod(),
//                        request.getRequestURI(),
//                        elapsed);
//            } else {
//                log.debug("[{}] {} {} - {}ms",
//                        response.getStatus(),
//                        request.getMethod(),
//                        request.getRequestURI(),
//                        elapsed);
//            }
        }
    }
}
