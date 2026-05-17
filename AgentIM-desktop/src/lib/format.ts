import type { Id, ImChat, ImMessage, ImUser } from "../types/im";

export function idText(id?: Id | null): string {
  return id === undefined || id === null ? "" : String(id);
}

export function sameId(left?: Id | null, right?: Id | null): boolean {
  return idText(left) === idText(right);
}

export function numericId(id?: Id | null): number {
  const value = Number(id);
  return Number.isFinite(value) ? value : 0;
}

export function displayName(user?: Pick<ImUser, "username" | "nickname"> | null): string {
  if (!user) {
    return "未知用户";
  }
  return user.nickname || user.username || "未知用户";
}

export function initials(text?: string): string {
  const value = (text || "A").trim();
  return value.slice(0, 2).toUpperCase();
}

export function formatTime(value?: string): string {
  if (!value) {
    return "";
  }
  const date = new Date(value.replace(" ", "T"));
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

export function formatBytes(value?: Id): string {
  const bytes = numericId(value);
  if (!bytes) {
    return "0 B";
  }
  const units = ["B", "KB", "MB", "GB"];
  let size = bytes;
  let index = 0;
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index += 1;
  }
  return `${size.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

export function chatTitle(chat: ImChat, currentUserId?: Id): string {
  if (chat.type === "saved") {
    return "保存的消息";
  }
  if (chat.title) {
    return chat.title;
  }
  if (chat.type === "private") {
    const peer = chat.members?.find((member) => !sameId(member.userId, currentUserId));
    return peer?.nickname || peer?.username || "私聊";
  }
  return "未命名聊天";
}

export function messagePreview(message?: ImMessage | null): string {
  if (!message) {
    return "";
  }
  if (message.status === "deleted_all") {
    return "[消息已撤回]";
  }
  if (message.messageType === "poll") {
    return `[投票] ${message.content || ""}`;
  }
  if (["image", "file", "voice", "video"].includes(message.messageType)) {
    return `[${message.messageType}] ${message.content || ""}`;
  }
  if (message.messageType === "forward") {
    return `[转发] ${message.content || ""}`;
  }
  return message.content || "";
}

export function parsePayload<T>(payload?: string): T | null {
  if (!payload) {
    return null;
  }
  try {
    return JSON.parse(payload) as T;
  } catch {
    return null;
  }
}
