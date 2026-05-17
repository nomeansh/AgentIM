import { fetch as tauriFetch } from "@tauri-apps/plugin-http";
import type { ApiResponse } from "../types/im";

export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  token?: string;
  clientId?: string;
  query?: Record<string, unknown>;
  body?: unknown;
  formData?: FormData;
  headers?: Record<string, string>;
}

export class ApiError extends Error {
  readonly code?: number;
  readonly status?: number;

  constructor(message: string, code?: number, status?: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

export function normalizeBaseUrl(baseUrl: string): string {
  return baseUrl.trim().replace(/\/+$/, "");
}

export function buildQuery(query?: Record<string, unknown>): string {
  if (!query) {
    return "";
  }
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export function unwrapResponse<T>(payload: ApiResponse<T>): T {
  if (payload.code !== 200) {
    throw new ApiError(payload.msg || "请求失败", payload.code);
  }
  return payload.data;
}

function isTauriRuntime(): boolean {
  return typeof window !== "undefined" && "__TAURI_INTERNALS__" in window;
}

async function runFetch(input: string, init: RequestInit): Promise<Response> {
  if (isTauriRuntime()) {
    return tauriFetch(input, init);
  }
  return globalThis.fetch(input, init);
}

export async function request<T>(baseUrl: string, path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = {
    ...(options.headers ?? {})
  };

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }
  if (options.clientId) {
    headers.clientid = options.clientId;
  }

  const init: RequestInit = {
    method,
    headers
  };

  if (options.formData) {
    init.body = options.formData;
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(options.body);
  }

  const response = await runFetch(`${normalizeBaseUrl(baseUrl)}${path}${buildQuery(options.query)}`, init);
  const text = await response.text();
  const payload = text ? (JSON.parse(text) as ApiResponse<T>) : ({ code: response.status, msg: response.statusText, data: null } as ApiResponse<T>);

  if (!response.ok) {
    throw new ApiError(payload.msg || response.statusText, payload.code, response.status);
  }
  return unwrapResponse(payload);
}
