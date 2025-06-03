package com.example.spring_ai_tutorial.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 문서 관련 DTO 클래스들
 */

/**
 * 문서 검색 결과
 */
@Schema(description = "문서 검색 결과")
data class DocumentSearchResultDto(
    @Schema(description = "문서 ID")
    val id: String,
    
    @Schema(description = "문서 내용")
    val content: String,
    
    @Schema(description = "문서 메타데이터")
    val metadata: Map<String, Any>,
    
    @Schema(description = "유사도 점수")
    val score: Double
)

/**
 * 문서 응답 데이터
 */
@Schema(description = "문서 응답 데이터")
data class DocumentResponseDto(
    @Schema(description = "문서 ID")
    val id: String,

    @Schema(description = "유사도 점수")
    val score: Double,

    @Schema(description = "문서 내용 (일부)")
    val content: String,

    @Schema(description = "문서 메타데이터")
    val metadata: Map<String, Any>
)

/**
 * DocumentSearchResultDto의 확장 함수로 DocumentResponseDto 변환 기능
 */
fun DocumentSearchResultDto.toDocumentResponseDto(): DocumentResponseDto {
    return DocumentResponseDto(
        id = this.id,
        score = this.score,
        content = this.content.take(500) + if (this.content.length > 500) "..." else "",
        metadata = this.metadata
    )
}
