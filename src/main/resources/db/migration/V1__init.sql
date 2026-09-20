-- V1: 1차 MVP 초기 스키마
-- devices 1─N favorite_folders 1─N favorites / bus_stations는 독립 참조 데이터 (FK 없음 - 재적재 대상)

CREATE TABLE devices (
    id            BINARY(16)   NOT NULL,
    alarm_enabled BIT(1)       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    fcm_token     VARCHAR(500) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE favorite_folders (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255) NOT NULL,
    device_id BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_favorite_folders_device
        FOREIGN KEY (device_id) REFERENCES devices (id) ON DELETE CASCADE
);

CREATE TABLE favorites (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    station_id   VARCHAR(255) NOT NULL,
    station_name VARCHAR(255) NOT NULL,
    city_code    VARCHAR(255) NOT NULL,
    latitude     DOUBLE       NOT NULL,
    longitude    DOUBLE       NOT NULL,
    route_id     VARCHAR(255) NOT NULL,
    route_name   VARCHAR(255) NOT NULL,
    folder_id    BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_folder_station_route (folder_id, station_id, route_id),
    CONSTRAINT fk_favorites_folder
        FOREIGN KEY (folder_id) REFERENCES favorite_folders (id) ON DELETE CASCADE
);

CREATE TABLE bus_stations (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    node_id           VARCHAR(50)  NOT NULL,
    name              VARCHAR(100) NOT NULL,
    latitude          DOUBLE       NOT NULL,
    longitude         DOUBLE       NOT NULL,
    city_code         VARCHAR(20)  NOT NULL,
    city_name         VARCHAR(50)  NULL,
    ars_id            VARCHAR(20)  NULL,
    managed_city_name VARCHAR(50)  NULL,
    collected_date    DATE         NULL,
    created_at        DATETIME(6)  NULL,
    PRIMARY KEY (id),
    -- 주변 정류장 검색(바운딩박스)용 - 좌표 풀스캔 방지
    KEY idx_bus_stations_lat_lng (latitude, longitude)
);
