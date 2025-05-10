package com.example.spring_ai_tutorial.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * 문서 처리 관련 커스텀 예외
 *
 * 문서 처리 도중 발생하는 다양한 오류를 처리하기 위한 공통 예외 클래스입니다.
 */
class DocumentProcessingException(message: String, cause: Throwable? = null) :
    ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause)
