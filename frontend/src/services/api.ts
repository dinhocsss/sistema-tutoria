import { fetchEventSource } from '@microsoft/fetch-event-source';
import type { AuthSession, PerguntaResponse, RespostaMessage } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function parseJsonOrError<T>(response: Response): Promise<T> {
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || data.message || `Erro HTTP ${response.status}`);
  }
  return data as T;
}

export async function login(username: string, password: string): Promise<AuthSession> {
  const response = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  return parseJsonOrError<AuthSession>(response);
}

export async function register(username: string, password: string): Promise<AuthSession> {
  const response = await fetch(`${API_BASE}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  return parseJsonOrError<AuthSession>(response);
}

export async function enviarPergunta(token: string, texto: string): Promise<PerguntaResponse> {
  const response = await fetch(`${API_BASE}/api/perguntas`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ texto }),
  });
  return parseJsonOrError<PerguntaResponse>(response);
}

export interface StreamHandlers {
  onConnected: () => void;
  onResposta: (msg: RespostaMessage) => void;
  onStatusChange: (status: 'conectando' | 'conectado' | 'reconectando' | 'erro') => void;
  onUnauthorized: () => void;
}

export function conectarStreamRespostas(
  token: string,
  signal: AbortSignal,
  handlers: StreamHandlers
): void {
  handlers.onStatusChange('conectando');

  void fetchEventSource(`${API_BASE}/api/respostas/stream`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream',
    },
    signal,
    openWhenHidden: true,
    async onopen(response) {
      if (response.ok) {
        handlers.onStatusChange('conectado');
        handlers.onConnected();
        return;
      }
      if (response.status === 401 || response.status === 403) {
        handlers.onUnauthorized();
        throw new Error('Sessao expirada ou token invalido');
      }
      throw new Error(`Falha ao abrir SSE (HTTP ${response.status})`);
    },
    onmessage(event) {
      if (event.event === 'connected') {
        handlers.onStatusChange('conectado');
        return;
      }
      if (event.event === 'resposta' && event.data) {
        try {
          const payload = JSON.parse(event.data) as RespostaMessage;
          handlers.onResposta(payload);
        } catch (err) {
          console.error('Erro ao fazer parse do evento SSE:', err);
        }
      }
    },
    onerror(err) {
      if (signal.aborted) {
        throw err;
      }
      handlers.onStatusChange('reconectando');
      return 3000; // tenta reconectar apos 3s
    },
  }).catch((err) => {
    if (!signal.aborted) {
      console.error('Stream SSE encerrado com erro:', err);
      handlers.onStatusChange('erro');
    }
  });
}
