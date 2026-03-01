package com.stationalarm.favorite.dto;

import com.stationalarm.favorite.domain.FavoriteFolder;
import lombok.Getter;

@Getter
public class FolderResponse {

    private Long id;
    private String name;

    public FolderResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static FolderResponse from(FavoriteFolder folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getName()
        );
    }
}
