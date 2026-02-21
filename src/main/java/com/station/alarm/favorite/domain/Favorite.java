package com.station.alarm.favorite.domain;

import com.station.alarm.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_station_route",
                        columnNames = {"user_id", "station_id", "route_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stationId;
    private String stationName;

    private String routeId;
    private String busNumber;

    private String regionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Favorite(User user,
                    String stationId,
                    String stationName,
                    String routeId,
                    String busNumber,
                    String regionCode) {
        this.user = user;
        this.stationId = stationId;
        this.stationName = stationName;
        this.routeId = routeId;
        this.busNumber = busNumber;
        this.regionCode = regionCode;
    }
}