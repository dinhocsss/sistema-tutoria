import { useEffect, useRef, useState, type FormEvent } from 'react';
import { conectarStreamRespostas, enviarPergunta } from '../services/api';
import type { AuthSession, ChatItem } from '../types';

interface TutorChatProps {
  session: AuthSession;
  onLogout: () => void;
}

export function TutorChat({ session, onLogout }: TutorChatProps) {
  const [items, setItems] = useState<ChatItem[]>([]);
  const [texto, setTexto] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [sseStatus, setSseStatus] = useState<'conectando' | 'conectado' | 'reconectando' | 'erro'>(
    'conectando'
  );
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    conectarStreamRespostas(session.token, controller.signal, {
      onConnected: () => setSseStatus('conectado'),
      onStatusChange: (status) => setSseStatus(status),
      onUnauthorized: () => onLogout(),
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
  }, [session.token, onLogout]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [items]);

  async function handleSend(e: FormEvent) {
    e.preventDefault();
    const trimmed = texto.trim();
    if (!trimmed) return;

    setSending(true);
    setSendError(null);
    try {
      const ack = await enviarPergunta(session.token, trimmed);
      setItems((prev) => [
        ...prev,
        {
          correlationId: ack.correlationId,
          pergunta: trimmed,
          status: 'PROCESSANDO',
          createdAt: ack.timestamp,
        },
      ]);
      setTexto('');
    } catch (err) {
      setSendError(err instanceof Error ? err.message : 'Erro ao enviar pergunta');
    } finally {
      setSending(false);
    }
  }

  const statusLabels: Record<typeof sseStatus, string> = {
    conectando: 'Conectando SSE...',
    conectado: 'SSE Conectado',
    reconectando: 'Reconectando SSE...',
    erro: 'SSE Desconectado',
  };

  return (
    <div className="chat-layout">
      <header className="chat-header">
        <div>
          <h1>Tutor Acadêmico IA</h1>
          <span className="user-meta">
            Logado como <strong>{session.username}</strong> ({session.userId})
          </span>
        </div>

        <div className="header-actions">
          <span className={`sse-badge sse-${sseStatus}`}>
            <span className="dot" />
            {statusLabels[sseStatus]}
          </span>
          <button type="button" className="btn-secondary" onClick={onLogout}>
            Sair
          </button>
        </div>
      </header>

      <main className="chat-messages">
        {items.length === 0 ? (
          <div className="empty-state">
            <h2>Envie sua primeira dúvida de Computação</h2>
            <p>
              Sua pergunta entra na fila durável <code>perguntas.queue</code> no RabbitMQ, é
              processada pelo Worker com Spring AI (Groq) e retorna em tempo real pelo
              stream SSE exclusivo do seu usuário.
            </p>
            <div className="suggestions">
              {[
                'O que é escopo léxico em linguagens de programação?',
                'Qual a diferença entre processo e thread?',
                'Explique o algoritmo de Dijkstra de forma didática.',
              ].map((sugestao) => (
                <button
                  key={sugestao}
                  type="button"
                  className="suggestion-chip"
                  onClick={() => setTexto(sugestao)}
                >
                  {sugestao}
                </button>
              ))}
            </div>
          </div>
        ) : (
          items.map((item) => (
            <article key={item.correlationId} className="qa-card">
              <div className="qa-question">
                <div className="qa-meta">
                  <span className="role-tag">Você</span>
                  <span className="corr-id" title="Correlation ID">
                    ID: {item.correlationId.slice(0, 8)}
                  </span>
                </div>
                <p>{item.pergunta}</p>
              </div>

              <div className={`qa-answer status-${item.status.toLowerCase()}`}>
                <div className="qa-meta">
                  <span className="role-tag ai">Tutor IA (Spring AI)</span>
                  <span className={`status-pill ${item.status.toLowerCase()}`}>
                    {item.status === 'PROCESSANDO' && 'Processando na fila...'}
                    {item.status === 'CONCLUIDO' && 'Respondido via SSE'}
                    {item.status === 'ERRO' && 'Falha no processamento'}
                  </span>
                </div>

                {item.status === 'PROCESSANDO' ? (
                  <div className="thinking">
                    <span className="spinner" />
                    <span>
                      Mensagem publicada em <code>perguntas.queue</code>. Aguardando Worker +
                      LLM...
                    </span>
                  </div>
                ) : (
                  <div className="answer-text">{item.resposta}</div>
                )}
              </div>
            </article>
          ))
        )}
        <div ref={bottomRef} />
      </main>

      <footer className="chat-footer">
        {sendError && <div className="alert-error">{sendError}</div>}
        <form onSubmit={handleSend} className="composer">
          <textarea
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Digite sua pergunta acadêmica... (você pode enviar várias sem esperar a anterior)"
            rows={2}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                void handleSend(e);
              }
            }}
          />
          <button type="submit" className="btn-primary" disabled={sending || !texto.trim()}>
            {sending ? 'Enviando...' : 'Enviar'}
          </button>
        </form>
      </footer>
    </div>
  );
}
