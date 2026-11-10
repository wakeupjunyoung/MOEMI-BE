package com.example.moemi.common.config;

import java.util.Arrays;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.moemi.common.web.DeviceId;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;

/**
 * Swagger UI at {@code /swagger-ui/index.html}, spec at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    static {
        // DeviceId comes from a header via DeviceIdArgumentResolver; don't list it as a query/body parameter.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(DeviceId.class);
    }

    @Bean
    public OpenAPI moemiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MOEMI API")
                .description("생활 도우미 앱 MOEMI 백엔드 API. 사용자별 데이터는 " + DeviceId.HEADER + " 헤더로 구분한다.")
                .version("v1"));
    }

    /** Shows the X-Device-Id header input only on endpoints that take a {@link DeviceId}. */
    @Bean
    public OperationCustomizer deviceIdHeader() {
        return (operation, handlerMethod) -> {
            boolean needsDeviceId = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.getParameterType() == DeviceId.class);
            if (needsDeviceId) {
                operation.addParametersItem(new HeaderParameter()
                        .name(DeviceId.HEADER)
                        .required(true)
                        .description("앱 설치마다 하나인 기기 식별자 (최대 " + DeviceId.MAX_LENGTH + "자)")
                        .schema(new StringSchema().maxLength(DeviceId.MAX_LENGTH).example("swagger-test")));
            }
            return operation;
        };
    }
}
