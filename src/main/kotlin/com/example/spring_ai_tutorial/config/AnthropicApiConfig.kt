package com.example.spring_ai_tutorial.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/*
    Anthropic API 설정
 */
@Configuration
 class AnthropicApiConfig {
    private val logger = KotlinLogging.logger {}

    @Value("\${spring.ai.anthropic.api-key}")   //application.properties에서 읽어오도록
    private lateinit var apiKey: String

    @Bean
    fun anthropicClient(): WebClient {
            return WebClient.builder()
                .baseUrl("https://api.anthropic.com/v1/messages")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build()
    }
}