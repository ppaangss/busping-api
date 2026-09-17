package com.busping.favorite.domain;

import com.busping.device.domain.Device;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "favorite_folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FavoriteFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 디바이스 삭제 시 폴더도 DB 레벨에서 함께 삭제 (ON DELETE CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Device device;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();

    public static FavoriteFolder create(
        Device device,
        String name
    ) {
        return FavoriteFolder.builder()
                .device(device)
                .name(name)
                .build();
    }

    public void updateName(String newName) {
        this.name = newName;
    }
}