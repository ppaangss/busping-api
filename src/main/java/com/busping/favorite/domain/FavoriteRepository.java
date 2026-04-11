package com.busping.favorite.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * userId 들을 이용해 Favorite 목록 조회
     * @param userIds
     * @return
     */
    @Query("""
    SELECT f
    FROM Favorite f
    JOIN FETCH f.folder fo
    JOIN FETCH fo.user
    WHERE fo.user.id IN :userIds
""")
    List<Favorite> findByUserIds(List<Long> userIds);
}