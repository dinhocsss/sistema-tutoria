package com.tutoria.worker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    private static final String SYSTEM_PROMPT = """
            Você é um tutor acadêmico de Ciência da Computação.
            Responda em português de forma clara, didática, concisa e bem estruturada.
            Priorize explicações diretas em poucos parágrafos ou tópicos curtos, usando um exemplo prático breve quando ajudar na compreensão.
            """;

    @Bean
    public ChatClient tutorChatClient(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:}") String model) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY nao configurada! Defina GROQ_API_KEY antes de enviar perguntas.");
        } else {
            String clean = apiKey.trim();
            String prefix = clean.length() >= 4 ? clean.substring(0, 4) : "***";
            log.info("Spring AI configurado com Groq (baseUrl={}, model={}, prefixoChave={}, tamanho={}).",
                    baseUrl, model, prefix, clean.length());
        }

        return chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
