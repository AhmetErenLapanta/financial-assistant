package com.ai.financial_assistant.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class ChatService(
	private val chatClient: ChatClient,
) {
	private val logger = LoggerFactory.getLogger(ChatService::class.java)

	fun processMessage(
		conversationId: String,
		userMessage: String,
	): String? {
		logger.info("Handling prompt for conversation: $conversationId")

		return chatClient
			.prompt()
			.user(userMessage)
			.advisors { it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId) }
			.call()
			.content()
	}

	private companion object {
		private const val CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id"
	}
}
