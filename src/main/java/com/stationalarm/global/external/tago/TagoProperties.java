package com.stationalarm.global.external.tago;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tago")
public class TagoProperties {

    private Api api = new Api();
    private Station station = new Station();
    private Arrival arrival = new Arrival();

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

    @Getter @Setter
    public static class Arrival {
        private String baseUrl;
    }
}
