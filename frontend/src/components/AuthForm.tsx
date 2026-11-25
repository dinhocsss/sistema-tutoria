import { useState, type FormEvent } from 'react';
import { login, register } from '../services/api';
import type { AuthSession } from '../types';

interface AuthFormProps {
  onAuthenticated: (session: AuthSession) => void;
}

export function AuthForm({ onAuthenticated }: AuthFormProps) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('aluno1');
  const [password, setPassword] = useState('123456');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const session =
        mode === 'login'
          ? await login(username, password)
          : await register(username, password);
      onAuthenticated(session);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao autenticar');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <span className="pill">Spring Boot + RabbitMQ + Spring AI + SSE</span>
          <h1>Tutoria Assíncrona com IA</h1>
          <p>
            Faça login para abrir seu stream SSE autenticado com JWT e enviar perguntas
            para o tutor acadêmico com IA.
          </p>
        </div>

        <div className="tabs">
          <button
            type="button"
            className={mode === 'login' ? 'tab active' : 'tab'}
            onClick={() => {
              setMode('login');
              setError(null);
            }}
          >
            Entrar
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'tab active' : 'tab'}
            onClick={() => {
              setMode('register');
              setUsername('');
              setPassword('');
              setError(null);
            }}
          >
            Criar conta
          </button>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <label>
            Usuário
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="ex.: aluno1"
              required
              minLength={3}
            />
          </label>

          <label>
            Senha
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="mínimo 4 caracteres"
              required
              minLength={4}
            />
          </label>

          {error && <div className="alert-error">{error}</div>}

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading
              ? 'Autenticando...'
              : mode === 'login'
                ? 'Entrar com JWT'
                : 'Cadastrar e Entrar'}
          </button>
        </form>

        <div className="demo-hint">
          <strong>Contas de teste pré-cadastradas:</strong>
          <div>
            <code>aluno1</code> / <code>123456</code> &nbsp;•&nbsp; <code>aluno2</code> /{' '}
            <code>123456</code>
          </div>
          <small>
            Abra duas abas com usuários diferentes para demonstrar o isolamento de respostas
            por JWT + SSE.
          </small>
        </div>
      </div>
    </div>
  );
}
