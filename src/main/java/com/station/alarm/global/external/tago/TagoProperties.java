package com.station.alarm.global.external.tago;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tago")
public class TagoProperties {

    private Api api = new Api();
    private Station station = new Station();

    @Getter @Setter
    public static class Api {
        private String key;
        private String type;
        private int defaultNumOfRows;
    }

    @Getter @Setter
    public static class Station {
        private String baseUrl;
    }
}
