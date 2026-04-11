package com.busping.favorite.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteFolderRepository extends JpaRepository<FavoriteFolder, Long> {

    Optional<FavoriteFolder> findByIdAndUserId(Long id, Long userId);

    List<FavoriteFolder> findAllByUser_IdOrderByIdAsc(Long userId);

    Optional<FavoriteFolder> findByIdAndUser_Id(Long folderId, Long userId);
}