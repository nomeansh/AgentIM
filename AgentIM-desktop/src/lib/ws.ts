import type { ImEvent } from "../types/im";
import { normalizeBaseUrl } from "./http";

export function deriveWsUrl(baseUrl: string, token: string, wsPath = "/ws/im"): string {
  const url = new URL(normalizeBaseUrl(baseUrl));
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = wsPath.startsWith("/") ? wsPath : `/${wsPath}`;
  url.search = "";
  url.searchParams.set("token", token);
  return url.toString();
}

function parseJson(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

export function parseSocketMessage(raw: string): ImEvent | null {
  const decoded = parseJson(raw);
  if (!decoded || typeof decoded !== "object") {
    return null;
  }

  const maybeWrapped = decoded as { message?: unknown };
  if (typeof maybeWrapped.message === "string") {
    return parseSocketMessage(maybeWrapped.message);
  }

  const maybeEvent = decoded as Partial<ImEvent>;
  if (typeof maybeEvent.eventType !== "string") {
    return null;
  }

  return maybeEvent as ImEvent;
}

export interface SocketController {
  close: () => void;
}

export function connectSocket(params: {
  baseUrl: string;
  token: string;
  wsPath: string;
  onEvent: (event: ImEvent) => void;
  onStatus: (status: "connecting" | "open" | "closed" | "error") => void;
}): SocketController {
  params.onStatus("connecting");
  const socket = new WebSocket(deriveWsUrl(params.baseUrl, params.token, params.wsPath));

  socket.onopen = () => params.onStatus("open");
  socket.onclose = () => params.onStatus("closed");
  socket.onerror = () => params.onStatus("error");
  socket.onmessage = (message) => {
    const event = parseSocketMessage(String(message.data));
    if (event) {
      params.onEvent(event);
    }
  };

  return {
    close: () => socket.close()
  };
}
