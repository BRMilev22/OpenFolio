import apiClient from '../../../api/apiClient';
import {endpoints} from '../../../api/endpoints';
import type {ApiResponse} from '../../../api/types/common';
import type {
  TokenResponse,
  OAuthCallbackRequest,
} from '../../../api/types/auth';

async function oauthGitHub(code: string, redirectUri?: string): Promise<TokenResponse> {
  const body: OAuthCallbackRequest = {code, redirectUri};
  const {data} = await apiClient.post<ApiResponse<TokenResponse>>(
    endpoints.auth.oauthGitHub,
    body,
  );
  if (!data.data) throw new Error(data.error?.message ?? 'GitHub sign in failed');
  return data.data;
}

async function logout(): Promise<void> {
  await apiClient.post(endpoints.auth.logout);
}

export const authService = {oauthGitHub, logout};
