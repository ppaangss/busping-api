package com.busping.user.service;

import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.CommonErrorCode;
import com.busping.user.domain.User;
import com.busping.user.domain.UserRepository;
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
