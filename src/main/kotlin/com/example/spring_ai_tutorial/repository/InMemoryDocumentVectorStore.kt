package com.example.spring_ai_tutorial.repository

import com.example.spring_ai_tutorial.exception.DocumentProcessingException
import com.example.spring_ai_tutorial.service.DocumentProcessingService
import com.example.spring_ai_tutorial.domain.dto.DocumentSearchResultDto
import com.example.spring_ai_tutorial.service.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Repository
import java.io.File
import org.springframework.ai.vectorstore.SearchRequest

/**
 * 문서를 벡터화하여 저장하고, 벡터 유사도 검색을 제공합니다.
 * Spring AI의 SimpleVectorStore를 활용합니다.
 */
@Repository
class InMemoryDocumentVectorStore(
    private val embeddingService: EmbeddingService,
    private val documentProcessingService: DocumentProcessingService,
) {
    private val logger = KotlinLogging.logger {}

    // Spring AI의 인메모리 SimpleVectorStore 생성
    private val vectorStore: VectorStore = SimpleVectorStore.builder(embeddingService.embeddingModel).build()

    /**
     * 문서를 벡터 스토어에 추가합니다.
     *
     * @param id 문서 식별자
     * @param fileText 문서 내용
     * @param metadata 문서 메타데이터
     */
    fun addDocument(id: String, fileText: String, metadata: Map<String, Any>) {
        logger.debug { "문서 추가 시작 - ID: $id, 내용 길이: ${fileText.length}" }

        try {
            // Spring AI Document 객체 생성
            val document = Document(fileText, metadata + mapOf("id" to id))
            val textSplitter = TokenTextSplitter.builder()
                .withChunkSize(512)           // 원하는 청크 크기
                .withMinChunkSizeChars(350)   // 최소 청크 크기
                .withMinChunkLengthToEmbed(5) // 임베딩할 최소 청크 길이
                .withMaxNumChunks(10000)      // 최대 청크 수
                .withKeepSeparator(true)      // 구분자 유지 여부
                .build()
            val chunks = textSplitter.split(document)

            // 벡터 스토어에 문서 청크 추가 (내부적으로 임베딩 변환 수행)
            vectorStore.add(chunks)

            logger.info { "문서 추가 완료 - ID: $id" }
        } catch (e: Exception) {
            logger.error(e) { "문서 추가 실패 - ID: $id" }
            throw DocumentProcessingException("문서 임베딩 및 저장 실패: ${e.message}", e)
        }
    }

    /**
     * 파일을 처리하여 벡터 스토어에 추가합니다.
     *
     * @param id 문서 식별자
     * @param file 파일 객체
     * @param metadata 문서 메타데이터
     */
    fun addDocumentFile(id: String, file: File, metadata: Map<String, Any>) {
        logger.debug { "파일 문서 추가 시작 - ID: $id, 파일: ${file.name}" }

        try {
            // 텍스트 추출
            val fileText = if (file.extension.lowercase() == "pdf") {
                documentProcessingService.extractTextFromPdf(file)
            } else {
                file.readText()
            }

            logger.debug { "파일 텍스트 추출 완료 - 길이: ${fileText.length}" }
            addDocument(id, fileText, metadata)
        } catch (e: Exception) {
            logger.error(e) { "파일 처리 실패 - ID: $id, 파일: ${file.name}" }
            throw DocumentProcessingException("파일 처리 실패: ${e.message}", e)
        }
    }

    /**
     * 질의와 유사한 문서를 검색합니다.
     *
     * @param query 검색 질의
     * @param maxResults 최대 결과 수
     * @return 유사도 순으로 정렬된 검색 결과 목록
     */
    fun similaritySearch(query: String, maxResults: Int): List<DocumentSearchResultDto> {
        logger.debug { "유사도 검색 시작 - 질의: '$query', 최대 결과: $maxResults" }

        try {
            // 검색 요청 구성
            val request = SearchRequest.builder()
                .query(query)
                .topK(maxResults)
                .build()

            // 유사성 검색 실행
            val results = vectorStore.similaritySearch(request) ?: emptyList()

            logger.debug { "유사도 검색 완료 - 결과 수: ${results.size}" }

            // 결과 매핑
            return results.map { result ->
                DocumentSearchResultDto(
                    id = (result.metadata["id"] ?: "unknown").toString(),
                    content = result.text ?: "",
                    metadata = result.metadata.filter { it.key != "id" },
                    score = result.score ?: 0.0
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "유사도 검색 실패 - 질의: '$query'" }
            throw DocumentProcessingException("유사도 검색 중 오류 발생: ${e.message}", e)
        }
    }
}