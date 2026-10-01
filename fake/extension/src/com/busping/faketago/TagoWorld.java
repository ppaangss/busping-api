package com.busping.faketago;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * TAGO 세계의 물리법칙 - 2026-10-01 실측(n=300) 재현
 *
 * - 성공 97.3%: lognormal(median 612ms, sigma 1.0) 지연 + 매핑의 성공 본문 그대로
 * - 느린 에러 2.0%: 5.0~5.6초 지연 후 resultCode 99 (세션 고갈, 6/300)
 * - 빠른 에러 0.7%: 0.1~0.6초 지연 후 resultCode 99 (2/300)
 *
 * 본문 스키마는 매핑(arrival.json)이 담당하고, 여기는 지연과 에러율만 다룬다.
 */
public class TagoWorld implements ResponseDefinitionTransformerV2 {

    private static final double SLOW_ERROR_RATE = 0.020;
    private static final double TOTAL_ERROR_RATE = 0.027;

    private static final int MEDIAN_MS = 612;   // 실측 성공 p50
    private static final double SIGMA = 1.0;    // ln(p95/p50)/1.645 = ln(3121/612)/1.645
    private static final int MAX_DELAY_MS = 6000; // 실측 상한(5.7초) 밖 꼬리 차단 - mock 스레드 보호

    // 실측 그대로 - resultCode는 숫자 99, body는 빈 문자열(객체 아님)
    private static final String ERROR_BODY =
            "{\"response\":{\"header\":{\"resultCode\":99,"
                    + "\"resultMsg\":\"가용한 세션이 존재하지 않습니다. (30/30)\"},\"body\":\"\"}}";

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double r = rnd.nextDouble();

        if (r < SLOW_ERROR_RATE) {
            return error(rnd.nextInt(5000, 5601));
        }
        if (r < TOTAL_ERROR_RATE) {
            return error(rnd.nextInt(100, 601));
        }

        int delay = (int) Math.min(MAX_DELAY_MS, MEDIAN_MS * Math.exp(SIGMA * rnd.nextGaussian()));
        return ResponseDefinitionBuilder.like(serveEvent.getResponseDefinition())
                .withFixedDelay(delay)
                .build();
    }

    private ResponseDefinition error(int delayMs) {
        return new ResponseDefinitionBuilder()
                .withStatus(200) // 실측 그대로 - 세션 고갈도 HTTP 200으로 온다
                .withHeader("Content-Type", "application/json")
                .withBody(ERROR_BODY)
                .withFixedDelay(delayMs)
                .build();
    }

    @Override
    public String getName() {
        return "tago-world";
    }

    @Override
    public boolean applyGlobally() {
        return false; // arrival.json이 transformers로 명시적으로 연결한다
    }
}
