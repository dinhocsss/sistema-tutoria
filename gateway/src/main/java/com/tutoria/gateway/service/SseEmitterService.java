package com.tutoria.gateway.service;

import com.tutoria.gateway.dto.RespostaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private static final int MAX_PENDING_PER_USER = 20;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Queue<RespostaMessage>> pendingRespostas = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(0L);
        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> {
            emitters.remove(userId, emitter);
            log.info("SSE encerrado para userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId, emitter);
            emitter.complete();
            log.info("SSE timeout para userId={}", userId);
        });
        emitter.onError(ex -> {
            emitters.remove(userId, emitter);
            log.debug("SSE erro de conexao para userId={}: {}", userId, ex.getMessage());
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "userId", userId,
                            "status", "CONNECTED",
                            "timestamp", Instant.now().toString()
                    )));
            log.info("SSE conectado para userId={}", userId);
            flushPending(userId, emitter);
        } catch (IOException ex) {
            emitters.remove(userId, emitter);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    public void sendResposta(RespostaMessage resposta) {
        String userId = resposta.userId();
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.warn("Nenhum SseEmitter ativo para userId={}; armazenando resposta correlationId={} no buffer",
                    userId, resposta.correlationId());
            enqueuePending(userId, resposta);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("resposta")
                    .id(resposta.correlationId())
                    .data(resposta));
            log.info("Resposta entregue via SSE para userId={}, correlationId={}", userId, resposta.correlationId());
        } catch (IOException | IllegalStateException ex) {
            log.warn("Falha ao enviar SSE para userId={}, correlationId={}: {}",
                    userId, resposta.correlationId(), ex.getMessage());
            emitters.remove(userId, emitter);
            enqueuePending(userId, resposta);
        }
    }

    private void enqueuePending(String userId, RespostaMessage resposta) {
        Queue<RespostaMessage> queue = pendingRespostas.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        while (queue.size() >= MAX_PENDING_PER_USER) {
            queue.poll();
        }
        queue.offer(resposta);
    }

    private void flushPending(String userId, SseEmitter emitter) throws IOException {
        Queue<RespostaMessage> queue = pendingRespostas.get(userId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        RespostaMessage msg;
        while ((msg = queue.poll()) != null) {
            emitter.send(SseEmitter.event()
                    .name("resposta")
                    .id(msg.correlationId())
                    .data(msg));
            log.info("Resposta pendente entregue via SSE para userId={}, correlationId={}",
                    userId, msg.correlationId());
        }
    }
}
