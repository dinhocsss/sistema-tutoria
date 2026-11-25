package com.tutoria.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class TutorAiService {

    private static final Logger log = LoggerFactory.getLogger(TutorAiService.class);

    private final ChatClient tutorChatClient;

    public TutorAiService(ChatClient tutorChatClient) {
        this.tutorChatClient = tutorChatClient;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public String consultarTutor(String correlationId, String perguntaUsuario) {
        log.info("Consultando LLM (Groq via Spring AI) para correlationId={}", correlationId);

        String promptEnriquecido = """
                Levando em conta que você é um tutor acadêmico de Ciência da Computação, \
                responda de forma didática à seguinte pergunta do estudante:

                %s
                """.formatted(perguntaUsuario);

        long inicio = System.currentTimeMillis();
        String resposta = tutorChatClient.prompt()
                .user(promptEnriquecido)
                .call()
                .content();
        long duracaoMs = System.currentTimeMillis() - inicio;

        if (resposta == null || resposta.isBlank()) {
            throw new IllegalStateException("A LLM retornou uma resposta vazia.");
        }

        log.info("LLM respondeu correlationId={} em {} ms", correlationId, duracaoMs);
        return resposta;
    }
}
