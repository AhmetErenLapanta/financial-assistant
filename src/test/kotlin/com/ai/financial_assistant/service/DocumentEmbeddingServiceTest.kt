package com.ai.financial_assistant.service

import com.ai.financial_assistant.config.RagProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.jdbc.core.JdbcTemplate

class DocumentEmbeddingServiceTest {
	private val vectorStore = mockk<VectorStore>(relaxed = true)
	private val jdbcTemplate = mockk<JdbcTemplate>(relaxed = true)
	private val ragProperties =
		RagProperties(
			chunkSize = 1000,
			chunkOverlap = 200,
			minChunkLength = 20,
			maxChunkSize = 10000,
			keepSeparator = true,
			documentPattern = "classpath:/test-documents/*.pdf",
			systemPromptPath = "classpath:/prompts/system.st",
		)
	private val documentEmbeddingService = DocumentEmbeddingService(vectorStore, ragProperties, jdbcTemplate)

	@Test
	fun `should not add documents to vector store when all documents already embedded`() {
		// Given
		every {
			jdbcTemplate.queryForList(
				"SELECT DISTINCT metadata->>'file_name' FROM vector_store",
				String::class.java,
			)
		} returns listOf("2025-kira-geliri-rehberi-gib.pdf")

		// When
		documentEmbeddingService.run()

		// Then
		verify(exactly = 0) { vectorStore.add(any()) }
	}

	@Test
	fun `should embed new documents that are not in database`() {
		// Given
		every {
			jdbcTemplate.queryForList(
				"SELECT DISTINCT metadata->>'file_name' FROM vector_store",
				String::class.java,
			)
		} returns emptyList()

		// When
		documentEmbeddingService.run()

		// Then
		verify(exactly = 1) { vectorStore.add(any()) }
	}

	@Test
	fun `should embed new documents when some are already embedded`() {
		// Given
		every {
			jdbcTemplate.queryForList(
				"SELECT DISTINCT metadata->>'file_name' FROM vector_store",
				String::class.java,
			)
		} returns listOf("other-document.pdf", "already-embedded.pdf")

		// When
		documentEmbeddingService.run()

		// Then
		verify(exactly = 1) { vectorStore.add(any()) }
	}
}
