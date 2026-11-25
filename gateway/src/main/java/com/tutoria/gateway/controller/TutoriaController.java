package com.tutoria.gateway.controller;

import com.tutoria.gateway.dto.PerguntaRequest;
import com.tutoria.gateway.dto.PerguntaResponse;
import com.tutoria.gateway.service.PerguntaProducerService;
import com.tutoria.gateway.service.SseEmitterService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TutoriaController {

    private final PerguntaProducerService perguntaProducerService;
    private final SseEmitterService sseEmitterService;

    public TutoriaController(
            PerguntaProducerService perguntaProducerService,
            SseEmitterService sseEmitterService
    ) {
        this.perguntaProducerService = perguntaProducerService;
        this.sseEmitterService = sseEmitterService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "tutoria-gateway");
    }

    @GetMapping(value = "/respostas/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRespostas(Authentication authentication) {
        String userId = authentication.getName();
        return sseEmitterService.createEmitter(userId);
    }

    @PostMapping("/perguntas")
    public ResponseEntity<PerguntaResponse> enviarPergunta(
            @Valid @RequestBody PerguntaRequest request,
            Authentication authentication
    ) {
        String userId = authentication.getName();
        PerguntaResponse response = perguntaProducerService.publicarPergunta(userId, request.texto());
        return ResponseEntity.accepted().body(response);
    }
}
