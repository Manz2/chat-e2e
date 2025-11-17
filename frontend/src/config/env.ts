type EnvGetter = () => string;

const requireEnv = (key: string): string => {
  const value = import.meta.env[key as keyof ImportMetaEnv];
  if (!value || typeof value !== 'string') {
    throw new Error(`Missing required environment variable ${key}. Define it in frontend/.env.`);
  }
  return value.trim().replace(/\/+$/, '');
};

const optionalEnv = (key: string, fallback?: EnvGetter): string => {
  const value = import.meta.env[key as keyof ImportMetaEnv];
  if (value && typeof value === 'string') {
    return value.trim();
  }
  return fallback ? fallback() : '';
};

const createWsUrl = (baseHttpUrl: string, path: string): string => {
  const targetPath = path.startsWith('/') ? path : `/${path}`;
  const base = new URL(baseHttpUrl.endsWith('/') ? baseHttpUrl : `${baseHttpUrl}/`);
  const wsUrl = new URL(targetPath, base);
  wsUrl.protocol = wsUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  wsUrl.search = '';
  wsUrl.hash = '';
  return wsUrl.toString();
};

const backendUrl = requireEnv('VITE_BACKEND_URL');
const redisUrl = optionalEnv('VITE_REDIS_URL', () => 'redis://localhost:6379');
const wsPath = optionalEnv('VITE_WS_PATH', () => '/ws');

export const appConfig = {
  backendUrl,
  redisUrl,
  websocketUrl: createWsUrl(backendUrl, wsPath),
} as const;

export type AppConfig = typeof appConfig;
