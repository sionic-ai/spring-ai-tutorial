package com.example.spring_ai_tutorial.controller

import com.example.spring_ai_tutorial.domain.dto.*
import com.example.spring_ai_tutorial.service.RagService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * RAG(Retrieval-Augmented Generation) API 컨트롤러
 *
 * 문서 업로드 및 질의응답 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG API", description = "Retrieval-Augmented Generation 기능을 위한 API")
class RagController(private val ragService: RagService) {
    private val logger = KotlinLogging.logger {}

    /**
     * AI Agent 에게 문서를 등록합니다.
     */
    @Operation(
        summary = "문서 등록",
        description = "파일이 벡터 스토어에 저장되며, 추후 질의시 컨텍스트로 활용됩니다."
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "문서 등록 요청 성공",
        content = [Content(schema = Schema(implementation = ApiResponseDto::class))]
    )
    @SwaggerResponse(responseCode = "400", description = "잘못된 요청")
    @SwaggerResponse(responseCode = "500", description = "서버 오류")
    @PostMapping("/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadDocument(
        @Parameter(description = "업로드할 파일", required = true)
        @RequestParam("file") file: MultipartFile,
        
        @Parameter(description = "버킷 ID (선택사항, 기본값은 설정된 기본 버킷)")
        @RequestParam("bucketId", required = false) bucketId: String?
    ): ResponseEntity<ApiResponseDto<DocumentUploadResultDto>> {
        logger.info { "문서 등록 요청 받음: ${file.originalFilename}" }

        // 파일 유효성 검사
        if (file.isEmpty) {
            logger.warn { "빈 파일이 업로드됨" }
            return ResponseEntity.badRequest().body(
                ApiResponseDto(success = false, error = "업로드된 파일이 비어있습니다.")
            )
        }

        return try {
            val documentId = if (bucketId != null) {
                ragService.uploadFile(file, bucketId)
            } else {
                ragService.uploadFile(file)
            }

            logger.info { "문서 등록 요청 성공: $documentId" }
            ResponseEntity.ok(
                ApiResponseDto(
                    success = true,
                    data = DocumentUploadResultDto(
                        documentId = documentId,
                        message = "문서 등록이 성공적으로 요청되었습니다."
                    )
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "문서 처리 중 오류 발생" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto(success = false, error = "문서 처리 중 오류가 발생했습니다: ${e.message}")
            )
        }
    }

    /**
     * 사용자 질의에 대해 관련 문서를 검색하고 RAG 기반 응답을 생성합니다.
     */
    @Operation(
        summary = "RAG 질의 수행",
        description = "사용자 질문에 대해 관련 문서를 검색하고 RAG 기반 응답을 생성합니다."
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "질의 성공",
        content = [Content(schema = Schema(implementation = ApiResponseDto::class))]
    )
    @SwaggerResponse(responseCode = "400", description = "잘못된 요청")
    @SwaggerResponse(responseCode = "500", description = "서버 오류")
    @PostMapping("/query")
    suspend fun queryWithRag(
        @Parameter(description = "질의 요청 객체", required = true)
        @RequestBody request: QueryRequestDto
    ): ResponseEntity<ApiResponseDto<QueryResponseDto>> {
        logger.info { "RAG 질의 요청 받음: ${request.query}" }

        // 유효성 검사
        if (request.query.isBlank()) {
            logger.warn { "빈 질의가 요청됨" }
            return ResponseEntity.badRequest().body(
                ApiResponseDto(success = false, error = "질의가 비어있습니다.")
            )
        }

        return try {
            // RAG 기반 응답 생성
            val stormResponse = ragService.generateAnswerWithContexts(
                question = request.query,
                bucketIds = request.bucketIds
            )

            val queryResponse = QueryResponseDto(
                query = stormResponse.chat.question,
                answer = stormResponse.chat.answer,
                relevantDocuments = stormResponse.contexts.map { it.toDocumentResponseDto() }
            )

            ResponseEntity.ok(
                ApiResponseDto(
                    success = true,
                    data = queryResponse
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "RAG 질의 처리 중 오류 발생" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto(success = false, error = "질의 처리 중 오류가 발생했습니다: ${e.message}")
            )
        }
    }
}