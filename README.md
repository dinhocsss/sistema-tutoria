# Sistema de Tutoria Assíncrono com IA

Sistema distribuído de perguntas e respostas acadêmicas em tempo real, utilizando:

- **Front-end:** React + Vite + TypeScript com **SSE** (`@microsoft/fetch-event-source`) e autenticação **JWT**.
- **Gateway (`gateway/`):** Spring Boot 3.5 (Web + Security JWT + AMQP) — recebe perguntas, publica no RabbitMQ, escuta respostas e envia via `SseEmitter`. **Não chama a LLM diretamente.**
- **Message Broker:** RabbitMQ (Docker Compose) com filas e exchanges **duráveis** e serialização **Jackson JSON**.
- **Worker (`worker/`):** Spring Boot 3.5 **sem Tomcat / Starter Web** (`web-application-type: none`) — consome `perguntas.queue`, aplica engenharia de prompt de tutor acadêmico, consulta a LLM (**Groq / Llama 3.3**) via **Spring AI** com retry/backoff e publica em `respostas.exchange` / `respostas.queue`.

---

## Arquitetura do Fluxo

```text
React (JWT + SSE)
   │  POST /api/perguntas (Authorization: Bearer <jwt>)
   ▼
Gateway (Spring Boot Web :8080)
   │  AMQP (JSON, persistente) ──► perguntas.exchange ──► perguntas.queue (durável)
   ▼
Worker (Spring Boot SEM Tomcat)
   │  Prompt Engineering + Spring AI (Google Gemini) + Retry (2s backoff)
   │  AMQP (JSON, persistente) ──► respostas.exchange ──► respostas.queue (durável)
   ▼
Gateway (@RabbitListener)
   │  SseEmitter vinculado ao userId do JWT (GET /api/respostas/stream)
   ▼
React (@microsoft/fetch-event-source) ──► Renderiza a resposta em tempo real
```

---

## Pré-requisitos

1. **Java JDK 21** (LTS) — recomendado [Eclipse Temurin 21](https://adoptium.net/) *(o Maven já vem embutido via Maven Wrapper `mvnw.cmd`, não precisa instalar Maven manualmente)*
2. **Node.js (LTS)** e `npm`
3. **Docker Desktop** (para subir o RabbitMQ)
4. **Chave gratuita do Groq** — crie em [Groq Console (API Keys)](https://console.groq.com/keys) *(sem cartão, resposta em < 1s e milhares de requisições grátis por dia)*

### Instalação rápida no Windows (PowerShell como Administrador)

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK --source winget --accept-package-agreements --accept-source-agreements
winget install --id OpenJS.NodeJS.LTS --source winget --accept-package-agreements --accept-source-agreements
winget install --id Docker.DockerDesktop --source winget --accept-package-agreements --accept-source-agreements
```

Depois de instalar, **feche e reabra o terminal** e valide:

```powershell
java -version
node -v
npm -v
docker --version
```

---

## Como Executar o Projeto

### 1. Subir o RabbitMQ (Docker)

Na raiz do projeto:

```powershell
docker compose up -d
```

- Painel RabbitMQ Management: `http://localhost:15672`
- Usuário / Senha padrão: `tutoria` / `tutoria123`

### 2. Rodar o Worker (com sua chave do Groq)

No **Prompt de Comando (`cmd`)**:

```cmd
cd worker
set GROQ_API_KEY=SUA_CHAVE_DO_GROQ
mvnw.cmd spring-boot:run
```

*(Ou no PowerShell: `$env:GROQ_API_KEY="SUA_CHAVE_DO_GROQ"; .\mvnw.cmd spring-boot:run`)*

> O Worker sobe **sem servidor web/Tomcat** e fica escutando `perguntas.queue`. Para ainda mais cota diária (até 14.400 req/dia), você pode definir `set GROQ_MODEL=llama-3.1-8b-instant`.

### 3. Rodar o Gateway

Em um segundo terminal:

```powershell
cd gateway
.\mvnw.cmd spring-boot:run
```

> API disponível em `http://localhost:8080`.

### 4. Rodar o Front-end React

Em um terceiro terminal:

```powershell
cd frontend
npm install
npm run dev
```

> Acesse `http://localhost:5173`.

---

## Como Testar o Isolamento entre Usuários (JWT + SSE)

O Gateway já inicia com duas contas de teste pré-cadastradas (e você também pode criar novas na aba **Criar conta**):

- `aluno1` / `123456`
- `aluno2` / `123456`

1. Abra `http://localhost:5173` em **duas abas**.
2. Faça login como `aluno1` na aba 1 e `aluno2` na aba 2.
3. Envie perguntas simultaneamente nas duas abas.
4. Observe no painel do RabbitMQ (`http://localhost:15672`) as filas duráveis `perguntas.queue` e `respostas.queue` e confirme que cada usuário recebe **somente** suas próprias respostas via SSE.
