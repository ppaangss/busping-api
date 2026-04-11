package com.busping.favorite.dto;

import com.busping.favorite.domain.Favorite;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoriteResponse {
    private Long favoriteId;
    private String stationId;
    private String stationName;
    private String routeId;
    private String routeName;

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getStationId(),
                favorite.getStationName(),
                favorite.getRouteId(),
                favorite.getRouteName()
        );
    }
}
