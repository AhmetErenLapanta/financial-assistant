package com.ai.financial_assistant.domain.dto.request

import jakarta.validation.constraints.NotBlank

data class ChatRequest(
	@field:NotBlank(message = "error.missing.field")
	val message: String,
	val conversationId: String? = null,
)
