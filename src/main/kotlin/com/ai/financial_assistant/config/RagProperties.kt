package com.ai.financial_assistant.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.rag")
data class RagProperties(
	val chunkSize: Int,
	val chunkOverlap: Int,
	val minChunkLength: Int,
	val maxChunkSize: Int,
	val keepSeparator: Boolean,
	val documentPattern: String,
	val systemPromptPath: String,
)
