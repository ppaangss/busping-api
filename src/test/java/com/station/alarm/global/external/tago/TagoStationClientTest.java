package com.station.alarm.global.external.tago;

import com.station.alarm.global.config.RestTemplateConfig;
import com.station.alarm.global.exception.ExternalApiException;
import com.station.alarm.global.exception.errorcode.TagoErrorCode;
import com.station.alarm.station.domain.Station;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(value = {TagoStationClient.class})
@Import({TagoStationParser.class, TagoProperties.class, RestTemplateConfig.class})
@TestPropertySource(properties = {
        "tago.api.key=test-key",
        "tago.api.type=json",
        "tago.api.default-num-of-rows=10",
        "tago.station.base-url=http://apis.data.go.kr/1613000/BusSttnInfoInqireService"
})
@EnableRetry
class TagoStationClientTest {

    @Autowired
    private TagoStationClient tagoStationClient;

    @Autowired
    private RestTemplate restTemplate; // 주입받은 RestTemplate

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // 주입받은 restTemplate을 사용하여 mockServer를 수동으로 생성(바인딩)
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void clear() {
        mockServer.reset();
    }

    @Test
    @DisplayName("성공: 좌표 기반 정류소 목록을 성공적으로 반환한다")
    void fetchNearbyStations_Success() {
        // given
        String mockResponseBody = """
        {
          "response": {
            "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE" },
            "body": {
              "items": {
                "item": [
                  {
                    "gpslati": 36.293125,
                    "gpslong": 127.30067,
                    "nodeid": "DJB8002011",
                    "nodenm": "성북3통",
                    "citycode": 25
                  }
                ]
              },
              "numOfRows": 10,
              "pageNo": 1,
              "totalCount": 1
            }
          }
        }
        """;

        // Mock 서버 설정: 특정 URL 요청 시 위 JSON을 반환하도록 함
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        // 2. When: 클라이언트 메서드 실행
        List<Station> stations = tagoStationClient.fetchNearbyStations(36.3, 127.3);

        // 3. Then: 결과 검증
        assertThat(stations).hasSize(1);

        // 첫 번째 데이터 상세 검증
        Station firstStation = stations.get(0);
        assertThat(firstStation.getName()).isEqualTo("성북3통");
        assertThat(firstStation.getLatitude()).isEqualTo(36.293125);
        assertThat(firstStation.getLongitude()).isEqualTo(127.30067);

        // Mock 서버에 설정된 기대사항이 모두 충족되었는지 확인
        mockServer.verify();
    }

    @Test
    @DisplayName("재시도 테스트: 서버 오류(500) 발생 시 3번 재시도 후 최종 실패한다")
    void fetchNearbyStations_RetryThreeTimes_ThenFail() {
        // 1. Given: 500 에러 응답을 3번 예약함
        // Mock 서버는 예약된 순서대로 응답을 던집니다.
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)); // 1번째 시도 실패

        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)); // 2번째 시도 실패

        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)); // 3번째 시도 실패

        // 2. When & 3. Then: 실행 및 예외 검증
        // 최종적으로 Recover 메서드가 실행되어 RETRY_EXHAUSTED 에러가 나야 함
        assertThatThrownBy(() -> tagoStationClient.fetchNearbyStations(36.3, 127.3))
                .isExactlyInstanceOf(ExternalApiException.class)
                .extracting("errorCode") // 예외 내부의 errorCode 필드를 추출
                .isEqualTo(TagoErrorCode.RETRY_EXHAUSTED);

        // Mock 서버에 예약된 3번의 요청이 모두 실제로 나갔는지 검증
        mockServer.verify();
    }

    @Test
    @DisplayName("실패: 400 오류 발생 시 재시도 없이 즉시 예외를 던진다")
    void fetchNearbyStations_4xxError_NoRetry() {
        // given
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withBadRequest()); // 400 에러

        // when & then
        assertThatThrownBy(() -> tagoStationClient.fetchNearbyStations(36.3, 127.3))
                .hasRootCauseInstanceOf(ExternalApiException.class)
                .hasStackTraceContaining(TagoErrorCode.BAD_REQUEST.getMessage());

        // verify: 재시도하지 않았으므로 1번만 호출되었는지 확인
        mockServer.verify();
    }

    @Test
    @DisplayName("성공: 데이터가 없는 경우 빈 리스트를 반환한다")
    void fetchNearbyStations_NoData_ReturnsEmptyList() {
        // given: 204 No Content 응답
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        // when
        List<Station> result = tagoStationClient.fetchNearbyStations(36.3, 127.3);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("재시도: 타임아웃 발생 시 재시도 후 성공한다")
    void fetchNearbyStations_Timeout_RetryAndSuccess() {
        // 1회차: 타임아웃 발생 (SocketTimeoutException은 RestClientException의 일종)
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andRespond(withException(new java.net.SocketTimeoutException("timeout")));

        // 2회차: 정상 응답
        String mockResponseBody = """
        {
          "response": {
            "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE" },
            "body": {
              "items": {
                "item": [
                  {
                    "gpslati": 36.293125,
                    "gpslong": 127.30067,
                    "nodeid": "DJB8002011",
                    "nodenm": "성북3통",
                    "citycode": 25
                  }
                ]
              },
              "numOfRows": 10,
              "pageNo": 1,
              "totalCount": 1
            }
          }
        }
        """;

        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        // when
        List<Station> result = tagoStationClient.fetchNearbyStations(36.3, 127.3);

        // then
        assertThat(result).isNotEmpty();
        mockServer.verify(); // 총 2번의 요청이 있었는지 확인
    }
}