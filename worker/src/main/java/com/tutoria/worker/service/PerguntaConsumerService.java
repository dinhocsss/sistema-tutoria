package com.tutoria.worker.service;

import com.tutoria.worker.dto.PerguntaMessage;
import com.tutoria.worker.dto.RespostaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PerguntaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(PerguntaConsumerService.class);

    private final TutorAiService tutorAiService;
    private final RabbitTemplate rabbitTemplate;
    private final String respostasExchange;
    private final String respostasRoutingKey;

    public PerguntaConsumerService(
            TutorAiService tutorAiService,
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.respostas-exchange}") String respostasExchange,
            @Value("${app.rabbitmq.respostas-routing-key}") String respostasRoutingKey
    ) {
        this.tutorAiService = tutorAiService;
        this.rabbitTemplate = rabbitTemplate;
        this.respostasExchange = respostasExchange;
        this.respostasRoutingKey = respostasRoutingKey;
    }

    @RabbitListener(queues = "${app.rabbitmq.perguntas-queue}")
    public void processarPergunta(PerguntaMessage pergunta) {
        log.info("Pergunta recebida de perguntas.queue: correlationId={}, userId={}",
                pergunta.correlationId(), pergunta.userId());

        RespostaMessage respostaPayload;
        try {
            String respostaIa = tutorAiService.consultarTutor(pergunta.correlationId(), pergunta.texto());
            respostaPayload = new RespostaMessage(
                    pergunta.correlationId(),
                    pergunta.userId(),
                    pergunta.texto(),
                    respostaIa,
                    "CONCLUIDO",
                    Instant.now()
            );
            log.info("Resposta gerada com sucesso para correlationId={}", pergunta.correlationId());
        } catch (Exception ex) {
            log.error("Falha apos tentativas de retry para correlationId={}: {}",
                    pergunta.correlationId(), ex.getMessage(), ex);
            respostaPayload = new RespostaMessage(
                    pergunta.correlationId(),
                    pergunta.userId(),
                    pergunta.texto(),
                    "Nao foi possivel obter resposta da IA no momento apos novas tentativas. "
                            + "Verifique sua chave GROQ_API_KEY ou tente novamente em instantes.",
                    "ERRO",
                    Instant.now()
            );
        }

        rabbitTemplate.convertAndSend(respostasExchange, respostasRoutingKey, respostaPayload, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setCorrelationId(pergunta.correlationId());
            return message;
        });

        log.info("Resposta publicada em respostas.exchange: correlationId={}, status={}",
                respostaPayload.correlationId(), respostaPayload.status());
    }
}
