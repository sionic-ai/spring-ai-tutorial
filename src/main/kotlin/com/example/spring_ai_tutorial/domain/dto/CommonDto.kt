package com.example.spring_ai_tutorial.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * API 표준 응답 포맷
 *
 * 모든 API 응답에 사용되는 공통 응답 포맷입니다.
 */
@Schema(description = "API 표준 응답 포맷")
data class ApiResponseDto<T>(
    @Schema(description = "요청 처리 성공 여부")
    val success: Boolean,

    @Schema(description = "응답 데이터 (성공 시)")
    val data: T? = null,

    @Schema(description = "오류 메시지 (실패 시)")
    val error: String? = null
)
