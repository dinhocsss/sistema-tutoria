export interface AuthSession {
  token: string;
  userId: string;
  username: string;
}

export interface PerguntaResponse {
  correlationId: string;
  status: string;
  timestamp: string;
}

export interface RespostaMessage {
  correlationId: string;
  userId: string;
  pergunta: string;
  resposta: string;
  status: 'CONCLUIDO' | 'ERRO';
  timestamp: string;
}

export interface ChatItem {
  correlationId: string;
  pergunta: string;
  resposta?: string;
  status: 'PROCESSANDO' | 'CONCLUIDO' | 'ERRO';
  createdAt: string;
  answeredAt?: string;
}
