package com.ai.financial_assistant.controller

import com.ai.financial_assistant.domain.dto.request.ChatRequest
import com.ai.financial_assistant.service.ChatService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/chat")
class ChatController(
	private val chatService: ChatService,
) {
	private val logger = LoggerFactory.getLogger(ChatController::class.java)

	@PostMapping
	fun chat(
		@Valid @RequestBody request: ChatRequest,
	): String? {
		val conversationId = request.conversationId ?: UUID.randomUUID().toString()
		logger.info("Chat request - conversation: $conversationId")
		return chatService.processMessage(conversationId, request.message)
	}
}
