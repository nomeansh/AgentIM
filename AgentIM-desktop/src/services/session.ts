import type { ConnectionSettings, LoginVo } from "../types/im";

const TOKEN_KEY = "agentim.accessToken";
const SETTINGS_KEY = "agentim.settings";

export const defaultSettings: ConnectionSettings = {
  authBaseUrl: "http://localhost:9210",
  apiBaseUrl: "http://localhost:9211",
  wsPath: "/ws/im",
  clientId: "agentim-web",
  tenantId: "000000"
};

type StoredConnectionSettings = Partial<ConnectionSettings> & {
  baseUrl?: string;
};

export function loadToken(): string {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function saveToken(login: LoginVo): string {
  const token = login.access_token;
  localStorage.setItem(TOKEN_KEY, token);
  return token;
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function loadSettings(): ConnectionSettings {
  const raw = localStorage.getItem(SETTINGS_KEY);
  if (!raw) {
    return { ...defaultSettings };
  }
  try {
    const stored = JSON.parse(raw) as StoredConnectionSettings;
    const legacyBaseUrl = stored.baseUrl;
    return {
      ...defaultSettings,
      ...stored,
      authBaseUrl: stored.authBaseUrl ?? legacyBaseUrl ?? defaultSettings.authBaseUrl,
      apiBaseUrl: stored.apiBaseUrl ?? legacyBaseUrl ?? defaultSettings.apiBaseUrl
    };
  } catch {
    return { ...defaultSettings };
  }
}

export function saveSettings(settings: ConnectionSettings): void {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
}
