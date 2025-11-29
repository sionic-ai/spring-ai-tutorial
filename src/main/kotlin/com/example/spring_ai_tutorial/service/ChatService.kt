package com.example.spring_ai_tutorial.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * OpenAI API를 사용하여 질의응답을 수행하는 서비스
 */
@Service
class ChatService(
    private val openAiApi: OpenAiApi,
    private val anthropicClient: WebClient
) {
    private val logger = KotlinLogging.logger {}

    /**
     * OpenAI 챗 API를 이용하여 응답을 생성합니다.
     *
     * @param userInput 사용자 입력 메시지
     * @param systemMessage 시스템 프롬프트
     * @param model 사용할 LLM 모델명
     * @return 챗 응답 객체, 오류 시 null
     */
    suspend fun openAiChat(
        userInput: String,
        systemMessage: String,
        model: String = "gpt-5.1-2025-11-13"
    ): ChatResponse? = withContext(Dispatchers.IO) {
        logger.debug { "OpenAI 챗 호출 시작 - 모델: $model" }
        try {
            // 메시지 구성
            val messages = listOf(
                SystemMessage(systemMessage),
                UserMessage(userInput)
            )

            // 챗 옵션 설정
            val chatOptions = ChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .build()

            // 프롬프트 생성
            val prompt = Prompt(messages, chatOptions)

            // 챗 모델 생성 및 호출
            val chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build()

            return@withContext chatModel.call(prompt)

        } catch (e: Exception) {
            logger.error(e) { "OpenAI 챗 호출 중 오류 발생: ${e.message}" }
            return@withContext null
        }
    }

    suspend fun anthropicChat(
        userInput: String,
        systemMessage: String,
        model: String = "claude-sonnet-4-5-20250929"
    ): String? = withContext(Dispatchers.IO) {
        logger.debug { "Anthropic 챗 호출 시작 - 모델: $model" }
        try {
            // 메시지 구성
            val requestBody = mapOf(
                "model" to model,
                "max_tokens" to 1024,
                "system" to systemMessage,  // ✅ 별도 파라미터
                "messages" to listOf(
                    mapOf("role" to "user", "content" to userInput)
                )
            )

            val response = anthropicClient
                .post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map::class.java)
                .awaitSingle()

            val text = ((response["content"] as List<*>)[0] as Map<*,*>)["text"]
            return@withContext text as String

        } catch (e: Exception) {
            logger.error(e) { "OpenAI 챗 호출 중 오류 발생: ${e.message}" }
            return@withContext null
        }
    }
}