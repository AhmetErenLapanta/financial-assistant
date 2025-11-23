package com.ai.financial_assistant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FinancialAssistantApplication

fun main(args: Array<String>) {
	runApplication<FinancialAssistantApplication>(*args)
}
