package com.stationalarm.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FolderUpdateRequest {

    @NotBlank(message = "폴더 이름은 필수입니다.")
    private String name;
}
