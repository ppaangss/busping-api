package com.stationalarm.user.service;

import com.stationalarm.global.exception.custom.BusinessException;
import com.stationalarm.global.exception.errorcode.CommonErrorCode;
import com.stationalarm.user.domain.User;
import com.stationalarm.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLocationService {

    private final UserRepository userRepository;

    @Transactional
    public void updateLocation(Long userId, Double lat, Double lng) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        user.updateLocation(lat, lng);
    }
}
