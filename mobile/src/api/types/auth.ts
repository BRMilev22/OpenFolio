export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userId?: number;
  email?: string;
  displayName?: string | null;
  githubUsername?: string | null;
};

export type OAuthCallbackRequest = {
  code: string;
  redirectUri?: string;
};
