package com.busping.global.auth;

import com.busping.device.domain.Device;
import com.busping.global.exception.custom.BusinessException;
import com.busping.global.exception.errorcode.DeviceErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 파라미터의 Device 타입에 인터셉터가 담아둔 인증 디바이스를 주입한다.
 */
@Component
public class DeviceArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Device.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object device = webRequest.getAttribute(
                DeviceAuthInterceptor.DEVICE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        // 인터셉터를 거치지 않은 경로에서 Device 파라미터를 쓴 설정 오류 방어
        if (device == null) {
            throw new BusinessException(DeviceErrorCode.UNREGISTERED_DEVICE);
        }
        return device;
    }
}
