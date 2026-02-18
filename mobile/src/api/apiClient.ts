import axios, {
  AxiosError,
  AxiosInstance,
  InternalAxiosRequestConfig,
} from 'axios';
import * as Keychain from 'react-native-keychain';

const BASE_URL = 'http://localhost:8080/api/v1';

const KEYCHAIN_SERVICE = 'openfolio.tokens';
const REFRESH_ENDPOINT = '/auth/refresh';

let isRefreshing = false;
let pendingQueue: Array<{
  resolve: (value: string) => void;
  reject: (reason: unknown) => void;
}> = [];

function processPendingQueue(error: unknown, token: string | null) {
  pendingQueue.forEach(p => (token ? p.resolve(token) : p.reject(error)));
  pendingQueue = [];
}

async function loadTokens(): Promise<{access: string; refresh: string} | null> {
  const creds = await Keychain.getGenericPassword({service: KEYCHAIN_SERVICE});
  if (!creds) return null;
  const {access, refresh} = JSON.parse(creds.password);
  return {access, refresh};
}

export async function saveTokens(access: string, refresh: string) {
  await Keychain.setGenericPassword(
    'tokens',
    JSON.stringify({access, refresh}),
    {service: KEYCHAIN_SERVICE},
  );
}

export async function clearTokens() {
  await Keychain.resetGenericPassword({service: KEYCHAIN_SERVICE});
}

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {'Content-Type': 'application/json'},
});

apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const tokens = await loadTokens();
    if (tokens?.access) {
      config.headers.Authorization = `Bearer ${tokens.access}`;
    }
    return config;
  },
);

apiClient.interceptors.response.use(
  res => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & {_retry?: boolean};

    if (error.response?.status !== 401 || original._retry || original.url === REFRESH_ENDPOINT) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        pendingQueue.push({resolve, reject});
      }).then(token => {
        original.headers.Authorization = `Bearer ${token}`;
        return apiClient(original);
      });
    }

    original._retry = true;
    isRefreshing = true;

    try {
      const tokens = await loadTokens();
      if (!tokens?.refresh) throw new Error('No refresh token');

      const {data} = await axios.post(`${BASE_URL}${REFRESH_ENDPOINT}`, {
        refreshToken: tokens.refresh,
      });
      const {accessToken, refreshToken} = data.data;
      await saveTokens(accessToken, refreshToken);
      processPendingQueue(null, accessToken);
      original.headers.Authorization = `Bearer ${accessToken}`;
      return apiClient(original);
    } catch (refreshError) {
      processPendingQueue(refreshError, null);
      await clearTokens();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

export default apiClient;
