package com.example.spring_ai_tutorial.service

import com.example.spring_ai_tutorial.exception.DocumentProcessingException
import com.example.spring_ai_tutorial.repository.InMemoryDocumentVectorStore
import com.example.spring_ai_tutorial.domain.dto.DocumentSearchResultDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

/**
 * 문서 업로드, 검색, 그리고 검색 결과를 활용한 LLM 응답 생성을 담당합니다.
 */
@Service
class RagService(
    private val vectorStore: InMemoryDocumentVectorStore,
    private val chatService: ChatService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * PDF 파일을 업로드하여 벡터 스토어에 추가합니다.
     *
     * @param file PDF 파일
     * @param originalFilename 원본 파일명
     * @return 생성된 문서 ID
     */
    fun uploadPdfFile(
        file: File,
        originalFilename: String?
    ): String {
        val documentId = UUID.randomUUID().toString()
        logger.info { "PDF 문서 업로드 시작. 파일: $originalFilename, ID: $documentId" }

        // 메타데이터 준비
        val docMetadata = HashMap<String, Any>().apply {
            put("originalFilename", originalFilename ?: "")
            put("uploadTime", System.currentTimeMillis())
        }

        // 벡터 스토어에 문서 추가
        try {
            vectorStore.addDocumentFile(documentId, file, docMetadata)
            logger.info { "PDF 문서 업로드 완료. ID: $documentId" }
            return documentId
        } catch (e: Exception) {
            logger.error(e) { "문서 처리 중 오류 발생: ${e.message}" }
            throw DocumentProcessingException("문서 처리 중 오류: ${e.message}", e)
        }
    }

    /**
     * 질의와 관련된 문서를 검색합니다.
     *
     * @param question 사용자 질문
     * @param maxResults 최대 검색 결과 수
     * @return 유사도 순으로 정렬된 문서 목록
     */
    fun retrieve(question: String, maxResults: Int): List<DocumentSearchResultDto> {
        logger.debug { "검색 시작: '$question', 최대 결과 수: $maxResults" }
        return vectorStore.similaritySearch(question, maxResults)
    }

    /**
     * 질문에 대한 답변을 생성하며, 참고한 정보 출처도 함께 제공합니다.
     *
     * @param question 사용자 질문
     * @param relevantDocs 이미 검색된 관련 문서
     * @param model 사용할 LLM 모델명
     * @return 참고 출처가 포함된 응답
     */
    fun generateAnswerWithContexts(
        question: String,
        relevantDocs: List<DocumentSearchResultDto>,
        model: String = "gpt-3.5-turbo"
    ): String {
        logger.debug { "RAG 응답 생성 시작: '$question', 모델: $model" }

        // 관련 문서 검색 또는 사용
        if (relevantDocs.isEmpty()) {
            logger.info { "관련 정보를 찾을 수 없음: '$question'" }
            return "관련 정보를 찾을 수 없습니다. 다른 질문을 시도하거나 관련 문서를 업로드해 주세요."
        }

        // 문서 번호 부여 (응답에서 출처 표시를 위해)
        val numberedDocs = relevantDocs.mapIndexed { index, doc ->
            "[${index + 1}] ${doc.content}"
        }

        // 관련 문서의 내용을 컨텍스트로 결합
        val context = numberedDocs.joinToString("\n\n")
        logger.debug { "컨텍스트 크기: ${context.length} 문자" }

        // 컨텍스트를 포함하는 시스템 프롬프트 생성
        val systemPromptText = """
            당신은 지식 기반 Q&A 시스템입니다. 
            사용자의 질문에 대한 답변을 다음 정보를 바탕으로 생성해주세요.
            주어진 정보에 답이 없다면 모른다고 솔직히 말해주세요.
            답변 마지막에 사용한 정보의 출처 번호 [1], [2] 등을 반드시 포함해주세요.
            
            정보:
            $context
        """.trimIndent()

        // LLM을 통한 응답 생성
        try {
            val response = chatService.openAiChat(question, systemPromptText, model)
            logger.debug { "AI 응답 생성: ${response}" }
            val aiAnswer = response?.result?.output?.text ?: "응답을 생성할 수 없습니다."

            // 참고 문서 정보 추가
            val sourceInfo = buildString {
                appendLine("\n\n참고 문서:")
                relevantDocs.forEachIndexed { index, doc ->
                    val originalFilename = doc.metadata["originalFilename"]?.toString() ?: "Unknown file"
                    appendLine("[${index + 1}] $originalFilename")
                }
            }

            return aiAnswer + sourceInfo
        } catch (e: Exception) {
            logger.error(e) { "AI 모델 호출 중 오류 발생: ${e.message}" }
            return "AI 모델 호출 중 오류가 발생했습니다. 검색 결과만 제공합니다:\n\n" +
                    relevantDocs.joinToString("\n\n") { it.content }
        }
    }
}