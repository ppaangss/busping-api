package com.busping.favorite.dto;

import lombok.Getter;

import java.util.List;


@Getter
public class FolderArrivalResponse {

    private Long folderId;
    private String folderName;
    private List<StationGroup> stations;

    public FolderArrivalResponse(Long folderId,
                                 String folderName,
                                 List<StationGroup> stations) {
        this.folderId = folderId;
        this.folderName = folderName;
        this.stations = stations;
    }

    @Getter
    public static class StationGroup {

        private String stationId;
        private String stationName;
        private List<RouteArrival> routes;

        public StationGroup(String stationId,
                            String stationName,
                            List<RouteArrival> routes) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.routes = routes;
        }
    }

    @Getter
    public static class RouteArrival {

        private Long favoriteId;
        private String routeId;
        private String routeName;
        private List<ArrivalInfo> arrivals;

        public RouteArrival(Long favoriteId,
                            String routeId,
                            String routeName,
                            List<ArrivalInfo> arrivals) {
            this.favoriteId = favoriteId;
            this.routeId = routeId;
            this.routeName = routeName;
            this.arrivals = arrivals;
        }
    }

    @Getter
    public static class ArrivalInfo {

        private int remainingMinutes;
        private int remainingStops;

        public ArrivalInfo(int remainingMinutes,
                           int remainingStops) {
            this.remainingMinutes = remainingMinutes;
            this.remainingStops = remainingStops;
        }
    }
}