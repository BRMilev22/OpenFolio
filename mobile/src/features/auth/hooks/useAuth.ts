import {useState} from 'react';
import {authService} from '../services/authService';
import {useAuthStore} from '../store/authStore';

export function useAuth() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const setAuthenticated = useAuthStore(s => s.setAuthenticated);
  const logoutStore = useAuthStore(s => s.logout);

  const loginWithGitHub = async (code: string, redirectUri?: string) => {
    setLoading(true);
    setError(null);
    try {
      const tokens = await authService.oauthGitHub(code, redirectUri);
      await setAuthenticated(tokens);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'GitHub sign-in failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const signOut = async () => {
    try {
      await authService.logout();
    } finally {
      await logoutStore();
    }
  };

  return {loginWithGitHub, signOut, loading, error};
}
