package com.ai.financial_assistant.service

import com.ai.financial_assistant.config.RagProperties
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
open class DocumentEmbeddingService(
	private val vectorStore: VectorStore,
	private val ragProperties: RagProperties,
	private val jdbcTemplate: JdbcTemplate,
) : CommandLineRunner {
	private val logger = LoggerFactory.getLogger(DocumentEmbeddingService::class.java)
	private val documentFinder = PathMatchingResourcePatternResolver()
	private val tokenTextSplitter =
		TokenTextSplitter(
			ragProperties.chunkSize,
			ragProperties.chunkOverlap,
			ragProperties.minChunkLength,
			ragProperties.maxChunkSize,
			ragProperties.keepSeparator,
		)

	override fun run(vararg args: String?) {
		logger.info("Starting document embedding...")
		runCatching {
			embedDocuments()
		}.onFailure { exception ->
			logger.error("Failed to load and embed documents", exception)
		}
	}

	private fun embedDocuments() {
		val allPdfs = getPdfs()
		val alreadyEmbedded = getEmbeddedFileNames()
		val newPdfs = allPdfs.filterNot { it.filename in alreadyEmbedded }

		when {
			allPdfs.isEmpty() -> logger.warn("No documents found in pattern: ${ragProperties.documentPattern}")
			newPdfs.isEmpty() -> logger.info("All ${allPdfs.size} documents already embedded")
			else -> embedNewDocuments(newPdfs)
		}
	}

	private fun getPdfs() = documentFinder.getResources(ragProperties.documentPattern).toList()

	private fun getEmbeddedFileNames(): Set<String> =
		runCatching {
			jdbcTemplate
				.queryForList(
					"SELECT DISTINCT metadata->>'file_name' FROM vector_store",
					String::class.java,
				).filterNotNull()
				.toSet()
		}.getOrDefault(emptySet())

	private fun embedNewDocuments(pdfs: List<Resource>) {
		val chunks = processDocuments(pdfs)
		saveChunks(chunks)
	}

	private fun processDocuments(resources: List<Resource>): List<Document> = resources.flatMap { extractFromFile(it) }

	private fun extractFromFile(resource: Resource): List<Document> {
		val pages = readPdf(resource)
		val withMetadata = addMetadata(pages, resource)
		return chunk(withMetadata)
	}

	private fun readPdf(resource: Resource): List<Document> {
		val pdfReader = PagePdfDocumentReader(resource)
		return pdfReader.get()
	}

	private fun addMetadata(
		documents: List<Document>,
		resource: Resource,
	): List<Document> =
		documents.map { document ->
			val cleanContent = cleanText(document.text ?: EMPTY_STRING)

			val filename = resource.filename ?: UNKNOWN_SOURCE
			Document(
				cleanContent,
				document.metadata.apply {
					put("source", filename)
					put("type", DOCUMENT_TYPE)
					put("file_name", filename)
				},
			)
		}

	private fun chunk(documents: List<Document>): List<Document> = tokenTextSplitter.apply(documents)

	private fun cleanText(content: String): String =
		content
			.replace(NULL_BYTE, EMPTY_STRING)
			.replace(CONTROL_CHARACTERS_REGEX.toRegex(), SINGLE_SPACE)
			.replace(MULTIPLE_WHITESPACE_REGEX.toRegex(), SINGLE_SPACE)
			.trim()

	private fun saveChunks(chunks: List<Document>) {
		if (chunks.isEmpty()) {
			logger.warn("No documents extracted - nothing to store")
			return
		}

		logger.info("Storing ${chunks.size} chunks in vector store...")
		vectorStore.add(chunks)
		logger.info("Successfully stored ${chunks.size} chunks")
	}

	private companion object {
		private const val UNKNOWN_SOURCE = "unknown"
		private const val DOCUMENT_TYPE = "financial_document"
		private const val NULL_BYTE = "\u0000"
		private const val CONTROL_CHARACTERS_REGEX = "[\u0001-\u001F\u007F-\u009F]"
		private const val MULTIPLE_WHITESPACE_REGEX = "\\s+"
		private const val SINGLE_SPACE = " "
		private const val EMPTY_STRING = ""
	}
}
