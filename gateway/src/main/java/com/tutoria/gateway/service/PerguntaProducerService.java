package com.tutoria.gateway.service;

import com.tutoria.gateway.dto.PerguntaMessage;
import com.tutoria.gateway.dto.PerguntaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PerguntaProducerService {

    private static final Logger log = LoggerFactory.getLogger(PerguntaProducerService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String perguntasExchange;
    private final String perguntasRoutingKey;

    public PerguntaProducerService(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.perguntas-exchange}") String perguntasExchange,
            @Value("${app.rabbitmq.perguntas-routing-key}") String perguntasRoutingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.perguntasExchange = perguntasExchange;
        this.perguntasRoutingKey = perguntasRoutingKey;
    }

    public PerguntaResponse publicarPergunta(String userId, String texto) {
        String correlationId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        PerguntaMessage payload = new PerguntaMessage(correlationId, userId, texto.trim(), now);

        rabbitTemplate.convertAndSend(perguntasExchange, perguntasRoutingKey, payload, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setCorrelationId(correlationId);
            return message;
        });

        log.info("Pergunta publicada na fila: correlationId={}, userId={}", correlationId, userId);
        return new PerguntaResponse(correlationId, "PROCESSANDO", now);
    }
}
