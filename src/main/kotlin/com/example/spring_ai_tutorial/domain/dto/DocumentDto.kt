package com.example.spring_ai_tutorial.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 문서 관련 DTO 클래스들
 */

/**
 * 문서 응답 데이터
 */
@Schema(description = "문서 응답 데이터")
data class DocumentResponseDto(
    @Schema(description = "문서 ID")
    val id: String,

    @Schema(description = "문서 내용 (일부)")
    val content: String,

    @Schema(description = "문서 메타데이터")
    val metadata: Map<String, Any>
)

/**
 * Storm API 응답 타입들
 */
@Schema(description = "Storm AI 채팅 응답")
data class StormChatDto(
    @Schema(description = "질문")
    val question: String,
    
    @Schema(description = "답변")
    val answer: String
)

@Schema(description = "Storm AI 컨텍스트 정보")
data class StormContextDto(
    @Schema(description = "문서 ID")
    val documentId: String,
    
    @Schema(description = "문서 내용")
    val content: String,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any> = emptyMap()
)

@Schema(description = "Storm AI 답변 생성 응답")
data class StormAnswerResponseDto(
    @Schema(description = "채팅 정보")
    val chat: StormChatDto,
    
    @Schema(description = "참조된 컨텍스트 목록")
    val contexts: List<StormContextDto>
)

/**
 * StormContextDto를 DocumentResponseDto로 변환
 */
fun StormContextDto.toDocumentResponseDto(): DocumentResponseDto {
    return DocumentResponseDto(
        id = this.documentId,
        content = this.content.take(1000) + if (this.content.length > 1000) "..." else "",
        metadata = this.metadata
    )
}