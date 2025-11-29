package com.example.spring_ai_tutorial.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {   // Swagger 문서화를 위한 설정을 담당

    @Bean
    fun springOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Spring AI Tutorial API")
                    .version("1.0")
                    .description("Spring AI를 활용한 챗봇 API")
            )
    }
}
