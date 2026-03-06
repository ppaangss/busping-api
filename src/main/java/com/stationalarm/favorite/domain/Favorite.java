package com.stationalarm.favorite.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_folder_station_route",
                        columnNames = {"folder_id", "station_id", "route_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정류장 정보
    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "region_code", nullable = false)
    private String cityCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // 노선 정보
    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "route_name", nullable = false)
    private String routeName;

    // 폴더 정보 (연관관계의 주인)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private FavoriteFolder folder;

    public static Favorite create(
            FavoriteFolder folder,
            String stationId,
            String stationName,
            String cityCode,
            Double latitude,
            Double longitude,
            String routeId,
            String routeName
    ) {
        return Favorite.builder()
                .folder(folder)
                .stationId(stationId)
                .stationName(stationName)
                .cityCode(cityCode)
                .latitude(latitude)
                .longitude(longitude)
                .routeId(routeId)
                .routeName(routeName)
                .build();
    }
}