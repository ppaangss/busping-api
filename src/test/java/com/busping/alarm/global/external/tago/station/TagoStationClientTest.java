package com.busping.alarm.global.external.tago.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.busping.global.config.RestTemplateConfig;
import com.busping.global.exception.custom.ExternalApiException;
import com.busping.global.exception.errorcode.TagoErrorCode;
import com.busping.global.external.tago.TagoProperties;
import com.busping.global.external.tago.station.TagoStationClient;
import com.busping.global.external.tago.station.TagoStationParser;
import com.busping.station.domain.BusStation;
import java.util.List;
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
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void clear() {
        mockServer.reset();
    }

    @Test
    @DisplayName("성공: 좌표 기반 정류장 목록을 정상 반환한다")
    void fetchNearbyStationsSuccess() {
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
                    "nodenm": "상봉3리",
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
                .andExpect(method(GET))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        List<BusStation> stations = tagoStationClient.fetchNearbyStations(36.3, 127.3);

        assertThat(stations).hasSize(1);

        BusStation firstStation = stations.get(0);
        assertThat(firstStation.getName()).isEqualTo("상봉3리");
        assertThat(firstStation.getLatitude()).isEqualTo(36.293125);
        assertThat(firstStation.getLongitude()).isEqualTo(127.30067);

        mockServer.verify();
    }

    @Test
    @DisplayName("실패: 서버 오류가 3번 발생하면 최종 실패한다")
    void fetchNearbyStationsRetryThreeTimesThenFail() {
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> tagoStationClient.fetchNearbyStations(36.3, 127.3))
                .isExactlyInstanceOf(ExternalApiException.class)
                .extracting("errorCode")
                .isEqualTo(TagoErrorCode.RETRY_EXHAUSTED);

        mockServer.verify();
    }

    @Test
    @DisplayName("실패: 400 오류는 재시도 없이 즉시 예외를 던진다")
    void fetchNearbyStations4xxErrorNoRetry() {
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> tagoStationClient.fetchNearbyStations(36.3, 127.3))
                .hasRootCauseInstanceOf(ExternalApiException.class)
                .hasStackTraceContaining(TagoErrorCode.BAD_REQUEST.getMessage());

        mockServer.verify();
    }

    @Test
    @DisplayName("성공: 데이터가 없으면 빈 리스트를 반환한다")
    void fetchNearbyStationsNoDataReturnsEmptyList() {
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        List<BusStation> result = tagoStationClient.fetchNearbyStations(36.3, 127.3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("성공: 타임아웃 발생 후 재시도에서 성공한다")
    void fetchNearbyStationsTimeoutRetryAndSuccess() {
        mockServer.expect(requestTo(containsString("/getCrdntPrxmtSttnList")))
                .andRespond(withException(new java.net.SocketTimeoutException("timeout")));

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
                    "nodenm": "상봉3리",
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

        List<BusStation> result = tagoStationClient.fetchNearbyStations(36.3, 127.3);

        assertThat(result).isNotEmpty();
        mockServer.verify();
    }
}
