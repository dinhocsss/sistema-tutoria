import { useCallback, useEffect, useState } from 'react';
import { conectarStreamRespostas, enviarPergunta } from '../services/api';
import type { ChatItem } from '../types';

export type SseStatus = 'conectando' | 'conectado' | 'reconectando' | 'erro';

export function useTutorChat(token: string, onUnauthorized: () => void) {
  const [items, setItems] = useState<ChatItem[]>([]);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [sseStatus, setSseStatus] = useState<SseStatus>('conectando');

  // Abre o stream SSE autenticado com JWT assim que o componente monta (apos o login)
  useEffect(() => {
    const controller = new AbortController();

    conectarStreamRespostas(token, controller.signal, {
      onConnected: () => setSseStatus('conectado'),
      onStatusChange: (status) => setSseStatus(status),
      onUnauthorized,
      onResposta: (msg) => {
        setItems((prev) => {
          const exists = prev.some((item) => item.correlationId === msg.correlationId);
          if (exists) {
            return prev.map((item) =>
              item.correlationId === msg.correlationId
                ? {
                    ...item,
                    resposta: msg.resposta,
                    status: msg.status,
                    answeredAt: msg.timestamp,
                  }
                : item
            );
          }
          return [
            ...prev,
            {
              correlationId: msg.correlationId,
              pergunta: msg.pergunta,
              resposta: msg.resposta,
              status: msg.status,
              createdAt: msg.timestamp,
              answeredAt: msg.timestamp,
            },
          ];
        });
      },
    });

    return () => {
      controller.abort();
    };
  }, [token, onUnauthorized]);

  // Envia a pergunta ao Gateway (POST /api/perguntas) e registra o card com correlationId
  const sendQuestion = useCallback(
    async (texto: string): Promise<boolean> => {
      const trimmed = texto.trim();
      if (!trimmed) return false;

      setSending(true);
      setSendError(null);
      try {
        const ack = await enviarPergunta(token, trimmed);
        setItems((prev) => [
          ...prev,
          {
            correlationId: ack.correlationId,
            pergunta: trimmed,
            status: 'PROCESSANDO',
            createdAt: ack.timestamp,
          },
        ]);
        return true;
      } catch (err) {
        setSendError(err instanceof Error ? err.message : 'Erro ao enviar pergunta');
        return false;
      } finally {
        setSending(false);
      }
    },
    [token]
  );

  return {
    items,
    sending,
    sendError,
    sseStatus,
    sendQuestion,
  };
}
