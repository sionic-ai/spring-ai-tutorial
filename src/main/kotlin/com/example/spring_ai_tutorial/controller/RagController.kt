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
import java.io.File
import java.io.IOException

/**
 * RAG(Retrieval-Augmented Generation) API 컨트롤러
 *
 * PDF 문서 업로드 및 질의응답 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG API", description = "Retrieval-Augmented Generation 기능을 위한 API")
class RagController(private val ragService: RagService) {
    private val logger = KotlinLogging.logger {}

    /**
     * PDF 문서를 업로드하여 벡터 스토어에 저장합니다.
     */
    @Operation(
        summary = "PDF 문서 업로드",
        description = "PDF 파일을 업로드하여 벡터 스토어에 저장합니다. 추후 질의에 활용됩니다."
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "문서 업로드 성공",
        content = [Content(schema = Schema(implementation = ApiResponseDto::class))]
    )
    @SwaggerResponse(responseCode = "400", description = "잘못된 요청 (빈 파일 또는 PDF가 아닌 파일)")
    @SwaggerResponse(responseCode = "500", description = "서버 오류")
    @PostMapping("/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadDocument(
        @Parameter(description = "업로드할 PDF 파일", required = true)
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponseDto<DocumentUploadResultDto>> {
        logger.info { "문서 업로드 요청 받음: ${file.originalFilename}" }

        // 유효성 검사
        if (file.isEmpty) {
            logger.warn { "빈 파일이 업로드됨" }
            return ResponseEntity.badRequest().body(
                ApiResponseDto(success = false, error = "파일이 비어있습니다.")
            )
        }
        file.originalFilename?.takeIf { it.lowercase().endsWith(".pdf") } ?: run {
            logger.warn { "지원하지 않는 파일 형식: ${file.originalFilename}" }
            return ResponseEntity.badRequest().body(
                ApiResponseDto(success = false, error = "PDF 파일만 업로드 가능합니다.")
            )
        }

        // File 객체 생성
        val tempFile = try {
            File.createTempFile("upload_", ".pdf").also {
                logger.debug { "임시 파일 생성됨: ${it.absolutePath}" }
                file.transferTo(it)
            }
        } catch (e: IOException) {
            logger.error(e) { "임시 파일 생성 실패" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto(success = false, error = "파일 처리 중 오류가 발생했습니다.")
            )
        }

        // 문서 처리 및 응답
        return try {
            val documentId = ragService.uploadPdfFile(tempFile, file.originalFilename)

            logger.info { "문서 업로드 성공: $documentId" }
            ResponseEntity.ok(
                ApiResponseDto(
                    success = true,
                    data = DocumentUploadResultDto(
                        documentId = documentId,
                        message = "문서가 성공적으로 업로드되었습니다."
                    )
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "문서 처리 중 오류 발생" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto(success = false, error = "문서 처리 중 오류가 발생했습니다: ${e.message}")
            )
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
                logger.debug { "임시 파일 삭제됨: ${tempFile.absolutePath}" }
            }
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
    fun queryWithRag(
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
            // 관련 문서 검색
            val relevantDocs = ragService.retrieve(request.query, request.maxResults)

            // RAG 기반 응답 생성
            val answer = ragService.generateAnswerWithContexts(
                request.query,
                relevantDocs,
                request.model
            )

            ResponseEntity.ok(
                ApiResponseDto(
                    success = true,
                    data = QueryResponseDto(
                        query = request.query,
                        answer = answer,
                        relevantDocuments = relevantDocs.map { it.toDocumentResponseDto() }
                    )
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