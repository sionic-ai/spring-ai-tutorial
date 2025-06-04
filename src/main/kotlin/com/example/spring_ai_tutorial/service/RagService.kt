package com.example.spring_ai_tutorial.service

import com.example.spring_ai_tutorial.domain.dto.StormAnswerResponseDto
import com.example.spring_ai_tutorial.domain.dto.StormChatDto
import com.example.spring_ai_tutorial.domain.dto.StormContextDto
import com.example.spring_ai_tutorial.exception.DocumentProcessingException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * 문서 업로드, 검색, 그리고 검색 결과를 활용한 LLM 응답 생성을 담당합니다.
 */
@Service
class RagService(
    @Value("\${storm.api.key}")
    private val stormApiKey: String,
    
    @Value("\${storm.default.bucket.id}")
    private val defaultBucketId: String,

    @Value("\${webhook.url}")
    private val defaultWebhookUrl: String? = null
) {
    private val logger = KotlinLogging.logger {}
    private val webClient = WebClient.builder()
        .baseUrl("https://live-stargate.sionic.im/api/v2")
        .build()

    /**
     * Storm API를 통해 문서 등록을 요청합니다.
     *
     * @param file 업로드할 파일
     * @param bucketId 버킷 ID (기본값: application.properties의 설정값)
     * @param parserType 파서 타입 (기본값: "DEFAULT")
     * @param webhookUrl 웹훅 URL (선택사항)
     * @return 생성된 문서 ID
     */
    suspend fun uploadFile(
        file: MultipartFile,
        bucketId: String = defaultBucketId,
        parserType: String = "DEFAULT", // "DEFAULT" or "STORM_PARSE"
        webhookUrl: String? = defaultWebhookUrl,
    ): String {
        logger.info { "문서 등록 요청 시작: ${file.originalFilename}, bucketId: $bucketId" }

        val bodyBuilder = MultipartBodyBuilder()

        // 파일 업로드 (여러 확장자 지원)
        bodyBuilder.part("file", file.bytes)
            .headers { headers ->
                headers.setContentDispositionFormData("file", file.originalFilename ?: "document")
            }
        bodyBuilder.part("bucketId", bucketId)
        bodyBuilder.part("parserType", parserType)
        webhookUrl?.let { bodyBuilder.part("webhookUrl", it) }

        return try {
            logger.debug { "요청 데이터: bucketId=$bucketId, parserType=$parserType, webhookUrl=$webhookUrl" }
            logger.debug { "파일 정보: name=${file.originalFilename}, size=${file.size}, contentType=${file.contentType}" }

            val response = webClient.post()
                .uri("/documents/by-file")
                .header("storm-api-key", stormApiKey)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .awaitBody<Map<String, Any>>()

            logger.debug { "Storm API 응답: $response" }

            // 응답에서 documentId 추출
            val data = response["data"] as? Map<String, Any>
                ?: throw DocumentProcessingException("응답에 data 필드가 없습니다: $response")
            val documentId = data["documentId"]?.toString()
                ?: throw DocumentProcessingException("data에서 documentId를 찾을 수 없습니다: $data")

            logger.info { "문서 등록 요청 완료: documentId=$documentId" }
            documentId

        } catch (e: Exception) {
            logger.error(e) { "문서 등록 요청 실패: ${e.message}" }
            if (e is org.springframework.web.reactive.function.client.WebClientResponseException) {
                logger.error { "응답 상태: ${e.statusCode}" }
                logger.error { "응답 본문: ${e.responseBodyAsString}" }
            }
            throw DocumentProcessingException("문서 등록 요청 실패: ${e.message}", e)
        }
    }

    /**
     * 질문에 대한 답변을 생성하며, 참고한 정보 출처도 함께 제공합니다.
     *
     * @param question 사용자 질문
     * @param bucketIds 검색할 버킷 ID 목록 (null이면 모든 버킷에서 검색)
     * @return Storm AI 답변 응답
     */
    suspend fun generateAnswerWithContexts(
        question: String,
        bucketIds: List<String>?,
    ): StormAnswerResponseDto {
        logger.info { "답변 생성 시작: question=$question, bucketIds=$bucketIds" }
        
        val requestBody = mutableMapOf<String, Any>("question" to question)
        bucketIds?.let { requestBody["bucketIds"] = it }

        return try {
            val response = webClient.post()
                .uri("/answer")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("storm-api-key", stormApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .awaitBody<Map<String, Any>>()

            logger.debug { "Storm API 응답: $response" }

            // 응답 파싱
            val data = response["data"] as? Map<String, Any>
                ?: throw DocumentProcessingException("응답 데이터가 올바르지 않습니다")
            val chatData = data["chat"] as? Map<String, Any>
                ?: throw DocumentProcessingException("채팅 데이터가 올바르지 않습니다")
            val contextsData = data["contexts"] as? List<Map<String, Any>>
                ?: emptyList()

            val stormResponse = StormAnswerResponseDto(
                chat = StormChatDto(
                    question = chatData["question"]?.toString() ?: question,
                    answer = chatData["answer"]?.toString() 
                        ?: throw DocumentProcessingException("답변을 찾을 수 없습니다")
                ),
                contexts = contextsData.map { contextMap ->
                    StormContextDto(
                        documentId = contextMap["id"]?.toString() ?: "unknown",
                        content = contextMap["context"]?.toString() ?: "",
                        metadata = contextMap.filterKeys { it != "context" && it != "id" }
                    )
                }
            )

            logger.info { "답변 생성 완료: ${stormResponse.contexts.size}개 컨텍스트 참조" }
            stormResponse

        } catch (e: Exception) {
            logger.error(e) { "답변 생성 실패: ${e.message}" }
            throw DocumentProcessingException("답변 생성 실패: ${e.message}", e)
        }
    }
}
