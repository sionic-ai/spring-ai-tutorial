package com.example.spring_ai_tutorial.service

import com.example.spring_ai_tutorial.exception.DocumentProcessingException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException

/**
 * 다양한 형식의 문서에서 텍스트를 추출하는 서비스입니다.
 * 현재는 PDF 파일 지원, 향후 다른 형식도 추가 가능합니다.
 */
@Service
class DocumentProcessingService {
    private val logger = KotlinLogging.logger {}

    /**
     * PDF 파일로부터 텍스트를 추출합니다.
     *
     * @param pdfFile PDF 파일 객체
     * @return 추출된 텍스트
     * @throws DocumentProcessingException 텍스트 추출 실패 시
     */
    fun extractTextFromPdf(pdfFile: File): String {
        logger.debug { "PDF 텍스트 추출 시작: ${pdfFile.name}" }

        return try {
            // Apache PDFBox를 사용하여 PDF에서 텍스트 추출
            PDDocument.load(pdfFile).use { document ->
                logger.debug { "PDF 문서 로드 성공: ${document.numberOfPages}페이지" }
                PDFTextStripper().getText(document)
            }.also {
                logger.debug { "PDF 텍스트 추출 완료: ${it.length} 문자" }
            }
        } catch (e: IOException) {
            logger.error(e) { "PDF 텍스트 추출 실패" }
            throw DocumentProcessingException("PDF에서 텍스트 추출 실패: ${e.message}", e)
        }
    }

    // 향후 다른 문서 형식 지원을 위한 메서드 추가 가능
    // fun extractTextFromDocx(docxFile: File): String { ... }
    // fun extractTextFromTxt(txtFile: File): String { ... }
}