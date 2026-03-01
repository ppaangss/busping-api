package com.stationalarm.favorite.domain;

import com.stationalarm.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    public static FavoriteFolder create(
        User user,
        String name
    ) {
        return FavoriteFolder.builder()
                .user(user)
                .name(name)
                .build();
    }

    public void updateName(String newName) {
        this.name = newName;
    }
}