import { appConfig } from '../config/env.ts';

export class ApiError extends Error {
  public readonly status: number;
  public readonly statusText: string;
  public readonly payload?: unknown;

  constructor(status: number, statusText: string, payload?: unknown) {
    super(`API request failed with status ${status}: ${statusText}`);
    this.status = status;
    this.statusText = statusText;
    this.payload = payload;
  }
}

export type QueryParams = Record<string, string | number | boolean | null | undefined>;

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  query?: QueryParams;
  body?: BodyInit | Record<string, unknown> | unknown[] | null;
}

const decodeResponse = async (response: Response) => {
  const raw = await response.text();
  if (!raw) {
    return undefined;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
};

export class HttpClient {
  private readonly baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const url = this.buildUrl(path, options.query);
    const init = this.prepareInit(options);
    const response = await fetch(url, init);
    if (!response.ok) {
      const payload = await decodeResponse(response);
      throw new ApiError(response.status, response.statusText, payload);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return decodeResponse(response) as Promise<T>;
  }

  get<T>(path: string, options?: RequestOptions) {
    return this.request<T>(path, { ...options, method: 'GET' });
  }

  post<T>(path: string, options?: RequestOptions) {
    return this.request<T>(path, { ...options, method: 'POST' });
  }

  put<T>(path: string, options?: RequestOptions) {
    return this.request<T>(path, { ...options, method: 'PUT' });
  }

  patch<T>(path: string, options?: RequestOptions) {
    return this.request<T>(path, { ...options, method: 'PATCH' });
  }

  delete<T>(path: string, options?: RequestOptions) {
    return this.request<T>(path, { ...options, method: 'DELETE' });
  }

  private prepareInit(options: RequestOptions): RequestInit {
    const { query: _ignored, body: rawBody, ...rest } = options;
    const headers = new Headers(rest.headers);
    let body: BodyInit | undefined;
    if (rawBody !== undefined && rawBody !== null) {
      if (rawBody instanceof FormData || rawBody instanceof Blob) {
        body = rawBody;
      } else if (typeof rawBody === 'string') {
        body = rawBody;
        if (!headers.has('Content-Type')) {
          headers.set('Content-Type', 'text/plain');
        }
      } else {
        body = JSON.stringify(rawBody);
        headers.set('Content-Type', 'application/json');
      }
    }
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }
    return { ...rest, headers, body };
  }

  private buildUrl(path: string, query?: QueryParams) {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const url = new URL(normalizedPath, this.baseUrl);
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value === undefined || value === null) {
          return;
        }
        url.searchParams.set(key, String(value));
      });
    }
    return url.toString();
  }
}

export const httpClient = new HttpClient(appConfig.backendUrl);
