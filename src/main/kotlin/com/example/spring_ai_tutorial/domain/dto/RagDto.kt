package com.example.spring_ai_tutorial.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * RAG 관련 DTO 클래스들
 */

/**
 * 문서 업로드 결과
 */
@Schema(description = "문서 업로드 결과")
data class DocumentUploadResultDto(
    @Schema(description = "생성된 문서 ID")
    val documentId: String,

    @Schema(description = "결과 메시지")
    val message: String
)

/**
 * 질의 요청 데이터 모델
 */
@Schema(description = "질의 요청 데이터 모델")
data class QueryRequestDto(
    @Schema(description = "사용자 질문", example = "인공지능이란 무엇인가요?")
    val query: String,

    @Schema(description = "최대 검색 결과 수", example = "3", defaultValue = "3")
    val maxResults: Int = 3,

    @Schema(description = "사용할 LLM 모델", example = "gpt-3.5-turbo", defaultValue = "gpt-3.5-turbo")
    val model: String = "gpt-3.5-turbo"
)

/**
 * 질의 응답 데이터
 */
@Schema(description = "질의 응답 데이터")
data class QueryResponseDto(
    @Schema(description = "원본 질의")
    val query: String,

    @Schema(description = "생성된 답변")
    val answer: String,

    @Schema(description = "관련 문서 목록")
    val relevantDocuments: List<DocumentResponseDto>
)
