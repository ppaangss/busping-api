package com.busping.alarm.dto;

import jakarta.validation.constraints.NotNull;

public record LocationReportRequest(
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
