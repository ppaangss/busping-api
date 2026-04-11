package com.busping.user.dto.fcm;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(@NotBlank String fcmToken) {}
