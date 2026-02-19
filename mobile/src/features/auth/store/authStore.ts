import {create} from 'zustand';
import * as Keychain from 'react-native-keychain';
import type {TokenResponse} from '../../../api/types/auth';
import {saveTokens, clearTokens} from '../../../api/apiClient';
import apiClient from '../../../api/apiClient';

const KEYCHAIN_SERVICE = 'openfolio.tokens';

type AuthState = {
  isAuthenticated: boolean;
  isRestoring: boolean;
  userId: number | null;
  email: string | null;
  displayName: string | null;
  githubUsername: string | null;
  setAuthenticated: (tokens: TokenResponse) => void;
  logout: () => void;
  restoreSession: () => Promise<void>;
};

export const useAuthStore = create<AuthState>(set => ({
  isAuthenticated: false,
  isRestoring: true,
  userId: null,
  email: null,
  displayName: null,
  githubUsername: null,

  setAuthenticated: async (tokens: TokenResponse) => {
    await saveTokens(tokens.accessToken, tokens.refreshToken);
    set({
      isAuthenticated: true,
      userId: tokens.userId ?? null,
      email: tokens.email ?? null,
      displayName: tokens.displayName ?? null,
      githubUsername: tokens.githubUsername ?? null,
    });
  },

  logout: async () => {
    await clearTokens();
    set({isAuthenticated: false, userId: null, email: null, displayName: null, githubUsername: null});
  },

  restoreSession: async () => {
    try {
      const creds = await Keychain.getGenericPassword({service: KEYCHAIN_SERVICE});
      if (!creds) {
        set({isRestoring: false});
        return;
      }
      const {access} = JSON.parse(creds.password);
      if (!access) {
        set({isRestoring: false});
        return;
      }
      // Validate the token by calling /users/me
      const resp = await apiClient.get('/users/me');
      const user = resp.data?.data;
      if (user) {
        set({
          isAuthenticated: true,
          isRestoring: false,
          userId: user.id ?? null,
          email: user.email ?? null,
          displayName: user.displayName ?? null,
          githubUsername: user.githubUsername ?? null,
        });
      } else {
        set({isRestoring: false});
      }
    } catch {
      // Token expired or invalid — the interceptor will try to refresh.
      // If refresh also fails, we stay logged out.
      set({isRestoring: false});
    }
  },
}));

