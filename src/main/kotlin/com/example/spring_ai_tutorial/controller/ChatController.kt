package com.example.spring_ai_tutorial.controller

import com.example.spring_ai_tutorial.service.ChatService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Chat API 컨트롤러
 *
 * LLM API를 통해 채팅 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat API", description = "OpenAI 및 Anthropic API를 통한 채팅 기능")
class ChatController(
    private val chatService: ChatService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자의 메시지를 받아 OpenAI API로 응답 생성
     */
    @Operation(
        summary = "LLM 채팅 메시지 전송",
        description = "사용자의 메시지를 받아 OpenAI 또는 Anthropic API를 통해 응답을 생성합니다."
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "LLM 응답 성공",
        content = [Content(schema = Schema(implementation = ApiResponse::class))]
    )
    @SwaggerResponse(responseCode = "400", description = "잘못된 요청")
    @SwaggerResponse(responseCode = "500", description = "서버 오류")
    @PostMapping("/query")
    suspend fun sendMessage(    //suspend 키워드 :  Kotlin를 활용하여 비동기 처리를 구현한다는 뜻 (Kotlin에서 suspend 함수는 일시 중단 가능한 함수를 의미. LLM과의 통신은 일반적으로 지연시간이 길기 때문에 어플리케이션의 응답성을 높이고자)
        @Parameter(description = "채팅 요청 객체", required = true)
        @RequestBody request: ChatRequest
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info { "Chat API 요청 받음: model=${request.model}" }

        // 유효성 검사
        if (request.query.isBlank()) {
            logger.warn { "빈 질의가 요청됨" }
            return ResponseEntity.badRequest().body(
                ApiResponse(success = false, error = "질의가 비어있습니다.")
            )
        }

        return try {
            // 시스템 프롬프트 지정
            val systemMessage = "You are a helpful AI assistant."

            // AI 응답 생성

            val response = if (request.model.startsWith("claude")){
                // Anthropic 호출 -> String 반환
                chatService.anthropicChat(
                    userInput = request.query,
                    systemMessage = systemMessage,
                    model = request.model
                )
            } else {
                // 기존 OpenAPI 호출 -> ChatResponse 반환
                chatService.openAiChat(
                    userInput = request.query,
                    systemMessage = systemMessage,
                    model = request.model
                )?.result?.output?.text
            }

            logger.debug { "LLM 응답 생성: $response" }

            // 이제 response는 String? 타입
            response?.let { answer ->
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        data = mapOf("answer" to answer)
                    )
                )
            } ?: run {
                logger.error { "LLM 응답 생성 실패" }
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse(
                        success = false,
                        error = "LLM 응답 생성 중 오류 발생"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Chat API 처리 중 오류 발생" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    error = e.message ?: "알 수 없는 오류 발생"
                )
            )
        }
    }
}

@Schema(description = "채팅 요청 데이터 모델")
data class ChatRequest(
    @Schema(description = "사용자 질문", example = "안녕하세요")
    val query: String,

    @Schema(description = "사용할 LLM 모델", example = "gpt-5.1-2025-11-13", defaultValue = "gpt-5.1-2025-11-13")
    val model: String = "gpt-5.1-2025-11-13"
)

@Schema(description = "API 응답 포맷")
data class ApiResponse<T>(
    @Schema(description = "요청 처리 성공 여부")
    val success: Boolean,

    @Schema(description = "성공 응답 데이터")
    val data: T? = null,

    @Schema(description = "실패 오류 메시지")
    val error: String? = null
)
