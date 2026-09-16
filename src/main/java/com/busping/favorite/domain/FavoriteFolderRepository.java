package com.busping.favorite.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteFolderRepository extends JpaRepository<FavoriteFolder, Long> {

    List<FavoriteFolder> findAllByDevice_IdOrderByIdAsc(UUID deviceId);

    /** 폴더 소유권 검증 겸 조회 - folderId와 deviceId가 모두 일치해야 반환 */
    Optional<FavoriteFolder> findByIdAndDevice_Id(Long folderId, UUID deviceId);
}
