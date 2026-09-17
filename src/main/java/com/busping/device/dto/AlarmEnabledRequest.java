package com.busping.device.dto;

import jakarta.validation.constraints.NotNull;

public record AlarmEnabledRequest(@NotNull Boolean alarmEnabled) {}
