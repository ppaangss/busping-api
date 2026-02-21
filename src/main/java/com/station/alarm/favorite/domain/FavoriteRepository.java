package com.station.alarm.favorite.domain;

import com.station.alarm.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndStationIdAndRouteId(
            User user,
            String stationId,
            String routeId
    );

    List<Favorite> findAllByUser(User user);

    Optional<Favorite> findByUserAndStationIdAndRouteId(
            User user,
            String stationId,
            String routeId
    );
}
