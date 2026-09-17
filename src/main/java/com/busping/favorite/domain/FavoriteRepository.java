package com.busping.favorite.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findAllByFolder_IdOrderByIdAsc(Long folderId);

    Optional<Favorite> findByIdAndFolder_Id(Long favoriteId, Long folderId);

    boolean existsByFolder_IdAndStationIdAndRouteId(
            Long folderId,
            String stationId,
            String routeId
    );

    List<Favorite> findAllByFolder_Id(Long folderId);

    /** 디바이스의 전체 즐겨찾기 조회 - 알람 평가용 (폴더 경유 조인) */
    List<Favorite> findAllByFolder_Device_Id(UUID deviceId);
}