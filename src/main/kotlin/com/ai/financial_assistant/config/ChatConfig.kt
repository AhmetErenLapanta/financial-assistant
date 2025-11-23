package com.ai.financial_assistant.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class ChatConfig {
	@Value("classpath:/prompts/system.st")
	private lateinit var systemResource: Resource

	@Bean
	fun chatClient(
		openAiChatModel: OpenAiChatModel,
		chatMemory: ChatMemory,
		vectorStore: VectorStore,
	): ChatClient =
		ChatClient
			.builder(openAiChatModel)
			.defaultAdvisors(
				MessageChatMemoryAdvisor.builder(chatMemory).build(),
				QuestionAnswerAdvisor.builder(vectorStore).build(),
				SimpleLoggerAdvisor(),
			).defaultSystem(systemResource)
			.build()
}
