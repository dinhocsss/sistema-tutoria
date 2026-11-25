import { useCallback, useState } from 'react';
import { AuthForm } from './components/AuthForm';
import { TutorChat } from './components/TutorChat';
import type { AuthSession } from './types';

const STORAGE_KEY = 'tutoria_session';

function loadSession(): AuthSession | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

export function App() {
  const [session, setSession] = useState<AuthSession | null>(loadSession);

  const handleAuthenticated = useCallback((nextSession: AuthSession) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
  }, []);

  const handleLogout = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setSession(null);
  }, []);

  if (!session) {
    return <AuthForm onAuthenticated={handleAuthenticated} />;
  }

  return <TutorChat session={session} onLogout={handleLogout} />;
}

export default App;
