package com.tutoria.gateway.service;

import com.tutoria.gateway.dto.RespostaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RespostaListenerService {

    private static final Logger log = LoggerFactory.getLogger(RespostaListenerService.class);

    private final SseEmitterService sseEmitterService;

    public RespostaListenerService(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @RabbitListener(queues = "${app.rabbitmq.respostas-queue}")
    public void consumirResposta(RespostaMessage resposta) {
        log.info("Resposta consumida de respostas.queue: correlationId={}, userId={}, status={}",
                resposta.correlationId(), resposta.userId(), resposta.status());
        sseEmitterService.sendResposta(resposta);
    }
}
