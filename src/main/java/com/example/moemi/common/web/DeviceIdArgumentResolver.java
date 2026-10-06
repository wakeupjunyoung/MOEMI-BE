package com.example.moemi.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.moemi.common.error.ApiException;

/**
 * Lets controllers take a {@link DeviceId} parameter, read from the {@value DeviceId#HEADER} header.
 */
public class DeviceIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == DeviceId.class;
    }

    @Override
    public DeviceId resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(DeviceId.HEADER);
        String value = header == null ? "" : header.trim();
        if (value.isEmpty()) {
            throw ApiException.badRequest("DEVICE_ID_REQUIRED", DeviceId.HEADER + " 헤더가 필요합니다.");
        }
        if (value.length() > DeviceId.MAX_LENGTH) {
            throw ApiException.badRequest("INVALID_DEVICE_ID",
                    DeviceId.HEADER + " 헤더는 " + DeviceId.MAX_LENGTH + "자 이하여야 합니다.");
        }
        return new DeviceId(value);
    }
}
