package com.example.spring_ai_tutorial.service

import org.springframework.ai.document.MetadataMode
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.retry.RetryUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * OpenAI의 임베딩 모델을 사용하여 텍스트를 벡터로 변환합니다.
 * Spring AI를 통해 임베딩 모델에 접근합니다.
 */
@Service
class EmbeddingService(
    private val openAiApi: OpenAiApi
) {
    @Value("\${spring.ai.openai.embedding.options.model}")
    private lateinit var embeddingModelName: String

    // OpenAI 임베딩 모델 설정
    val embeddingModel by lazy {
        OpenAiEmbeddingModel(
            openAiApi,
            MetadataMode.EMBED,
            OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build(),
            RetryUtils.DEFAULT_RETRY_TEMPLATE
        )
    }
}