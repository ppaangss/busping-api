package com.stationalarm.favorite.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findAllByFolder_IdOrderByIdAsc(Long folderId);

    Optional<Favorite> findByIdAndFolder_Id(Long favoriteId, Long folderId);

    boolean existsByFolder_IdAndStationIdAndRouteId(
            Long folderId,
            String stationId,
            String routeId
    );

    List<Favorite> findAllByFolder_Id(Long folderId);

}