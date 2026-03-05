package com.stationalarm.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private LocalDateTime lastLocationUpdatedAt;

    public static User create(
            String email,
            String password
    ) {
        return User.builder()
                .email(email)
                .password(password)
                .build();
    }

    public void updateLocation(Double lat, Double lng) {
        this.latitude = lat;
        this.longitude = lng;
        this.lastLocationUpdatedAt = LocalDateTime.now();
    }
}
