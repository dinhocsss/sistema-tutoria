import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useTheme } from '../hooks/useTheme';
import { useTutorChat, type SseStatus } from '../hooks/useTutorChat';
import type { AuthSession } from '../types';

interface TutorChatProps {
  session: AuthSession;
  onLogout: () => void;
}

function CodeBlock({ children }: { children?: ReactNode }) {
  const [copied, setCopied] = useState(false);
  const preRef = useRef<HTMLPreElement | null>(null);

  function handleCopy() {
    const text = preRef.current?.innerText ?? '';
    if (!text) return;
    void navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    });
  }

  return (
    <div className="code-block-wrapper">
      <button type="button" className="copy-code-btn" onClick={handleCopy}>
        {copied ? 'Copiado!' : 'Copiar'}
      </button>
      <pre ref={preRef}>{children}</pre>
    </div>
  );
}

export function TutorChat({ session, onLogout }: TutorChatProps) {
  const { theme, toggleTheme } = useTheme();
  const { items, sending, sendError, sseStatus, sendQuestion } = useTutorChat(
    session.token,
    onLogout
  );
  const [texto, setTexto] = useState('');
  const [showScrollBtn, setShowScrollBtn] = useState(false);
  const messagesRef = useRef<HTMLElement | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [items]);

  function handleScroll() {
    const el = messagesRef.current;
    if (!el) return;
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    setShowScrollBtn(distanceFromBottom > 120);
  }

  function scrollToBottom() {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }

  async function handleSend(e: FormEvent) {
    e.preventDefault();
    const sent = await sendQuestion(texto);
    if (sent) {
      setTexto('');
    }
  }

  const statusLabels: Record<SseStatus, string> = {
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
          <button
            type="button"
            className="btn-icon"
            onClick={toggleTheme}
            title={theme === 'dark' ? 'Alternar para tema claro' : 'Alternar para tema escuro'}
            aria-label="Alternar tema"
          >
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
          <button type="button" className="btn-secondary" onClick={onLogout}>
            Sair
          </button>
        </div>
      </header>

      <main className="chat-messages" ref={messagesRef} onScroll={handleScroll}>
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
                  <div className="answer-text markdown-body">
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      components={{
                        pre: ({ children }) => <CodeBlock>{children}</CodeBlock>,
                      }}
                    >
                      {item.resposta ?? ''}
                    </ReactMarkdown>
                  </div>
                )}
              </div>
            </article>
          ))
        )}
        <div ref={bottomRef} />
      </main>

      {showScrollBtn && (
        <button
          type="button"
          className="scroll-bottom-btn"
          onClick={scrollToBottom}
          title="Ir para a última resposta"
          aria-label="Ir para a última resposta"
        >
          ↓
        </button>
      )}

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
