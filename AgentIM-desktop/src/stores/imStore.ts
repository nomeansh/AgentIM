import type { ConnectionSettings, Id, ImChat, ImContact, ImEvent, ImMessage, ImPinnedMessage, ImPoll, ImReaction, ImSearchResult, ImUser } from "../types/im";
import { defaultSettings } from "../services/session";
import { idText, numericId, sameId } from "../lib/format";

export interface ImState {
  settings: ConnectionSettings;
  token: string;
  profile: ImUser | null;
  chats: ImChat[];
  contacts: ImContact[];
  activeChatId: Id | null;
  messagesByChat: Record<string, ImMessage[]>;
  pinnedByChat: Record<string, ImPinnedMessage[]>;
  pollsByMessage: Record<string, ImPoll>;
  searchResult: ImSearchResult | null;
  socketStatus: "idle" | "connecting" | "open" | "closed" | "error";
  loading: boolean;
  error: string;
}

export function createInitialState(): ImState {
  return {
    settings: { ...defaultSettings },
    token: "",
    profile: null,
    chats: [],
    contacts: [],
    activeChatId: null,
    messagesByChat: {},
    pinnedByChat: {},
    pollsByMessage: {},
    searchResult: null,
    socketStatus: "idle",
    loading: false,
    error: ""
  };
}

export function sortMessagesAscending(messages: ImMessage[]): ImMessage[] {
  return [...messages].sort((left, right) => numericId(left.seq) - numericId(right.seq));
}

export function insertOrReplaceMessage(messages: ImMessage[], message: ImMessage): ImMessage[] {
  const index = messages.findIndex((item) => sameId(item.id, message.id));
  if (index >= 0) {
    const next = [...messages];
    next[index] = message;
    return sortMessagesAscending(next);
  }
  return sortMessagesAscending([...messages, message]);
}

export function mergeChats(chats: ImChat[], chat: ImChat): ImChat[] {
  const index = chats.findIndex((item) => sameId(item.id, chat.id));
  const next = index >= 0 ? chats.map((item) => (sameId(item.id, chat.id) ? { ...item, ...chat } : item)) : [chat, ...chats];
  return next.sort((left, right) => {
    const leftTime = left.lastMsgTime ? new Date(left.lastMsgTime.replace(" ", "T")).getTime() : 0;
    const rightTime = right.lastMsgTime ? new Date(right.lastMsgTime.replace(" ", "T")).getTime() : 0;
    return rightTime - leftTime;
  });
}

function updateMessage(state: ImState, chatKey: string, messageId: Id, update: (message: ImMessage) => ImMessage): void {
  state.messagesByChat[chatKey] = (state.messagesByChat[chatKey] ?? []).map((message) => (sameId(message.id, messageId) ? update(message) : message));
}

function applyReactionDelta(state: ImState, event: ImEvent, delta: 1 | -1): void {
  const reaction = event.payload as Partial<ImReaction> | undefined;
  const chatKey = idText(event.chatId);
  if (!reaction?.messageId || !reaction.reaction || !chatKey) {
    return;
  }
  updateMessage(state, chatKey, reaction.messageId, (message) => {
    const summaries = [...(message.reactions ?? [])];
    const index = summaries.findIndex((item) => item.reaction === reaction.reaction);
    if (index < 0 && delta > 0) {
      summaries.push({ reaction: reaction.reaction!, count: 1 });
    } else if (index >= 0) {
      const nextCount = Number(summaries[index].count || 0) + delta;
      if (nextCount <= 0) {
        summaries.splice(index, 1);
      } else {
        summaries[index] = { ...summaries[index], count: nextCount };
      }
    }
    return { ...message, reactions: summaries };
  });
}

function isChatPayload(payload: unknown): payload is Partial<ImChat> {
  if (!payload || typeof payload !== "object") {
    return false;
  }
  const candidate = payload as Partial<ImChat>;
  return typeof candidate.type === "string";
}

export function applyEventToState(state: ImState, event: ImEvent): void {
  const payload = event.payload as Partial<ImMessage & ImChat> | undefined;
  if (!payload) {
    return;
  }

  if (event.eventType === "message.created" || event.eventType === "message.edited") {
    const message = payload as ImMessage;
    const chatKey = idText(message.chatId || event.chatId);
    if (!chatKey) {
      return;
    }
    state.messagesByChat[chatKey] = insertOrReplaceMessage(state.messagesByChat[chatKey] ?? [], message);
    state.chats = state.chats.map((chat) =>
      sameId(chat.id, chatKey)
        ? {
            ...chat,
            lastMsgId: message.id,
            lastMsgContent: message.content,
            lastMsgTime: message.createTime || chat.lastMsgTime,
            seq: message.seq || chat.seq
          }
        : chat
    );
  }

  if (event.eventType === "message.deleted") {
    const messageId = payload.id ?? (payload as { messageId?: Id }).messageId;
    const chatKey = idText(event.chatId || payload.chatId);
    if (messageId && chatKey) {
      state.messagesByChat[chatKey] = (state.messagesByChat[chatKey] ?? []).map((message) =>
        sameId(message.id, messageId) ? { ...message, status: "deleted_all", content: "[消息已撤回]" } : message
      );
    }
  }

  if (event.eventType === "message.hidden") {
    const messageId = payload.id ?? (payload as { messageId?: Id }).messageId;
    const chatKey = idText(event.chatId || payload.chatId);
    if (messageId && chatKey) {
      state.messagesByChat[chatKey] = (state.messagesByChat[chatKey] ?? []).filter((message) => !sameId(message.id, messageId));
    }
  }

  if (event.eventType === "message.pinned") {
    const pin = payload as ImPinnedMessage;
    const chatKey = idText(pin.chatId || event.chatId);
    if (pin.messageId && chatKey) {
      const current = state.pinnedByChat[chatKey] ?? [];
      state.pinnedByChat[chatKey] = [pin, ...current.filter((item) => !sameId(item.messageId, pin.messageId))];
    }
  }

  if (event.eventType === "message.unpinned") {
    const pin = payload as Partial<ImPinnedMessage>;
    const chatKey = idText(pin.chatId || event.chatId);
    if (pin.messageId && chatKey) {
      state.pinnedByChat[chatKey] = (state.pinnedByChat[chatKey] ?? []).filter((item) => !sameId(item.messageId, pin.messageId));
    }
  }

  if (event.eventType === "reaction.created") {
    applyReactionDelta(state, event, 1);
  }

  if (event.eventType === "reaction.deleted") {
    applyReactionDelta(state, event, -1);
  }

  if (event.eventType === "read_state.updated") {
    const readState = payload as { chatId?: Id; userId?: Id } | undefined;
    const chatKey = idText(readState?.chatId || event.chatId);
    const userId = readState?.userId || event.actorId;
    if (chatKey && state.profile?.id && userId && sameId(state.profile.id, userId)) {
      state.chats = state.chats.map((chat) => (sameId(chat.id, chatKey) ? { ...chat, unreadCount: 0 } : chat));
    }
  }

  if (event.eventType === "chat.created" || event.eventType === "chat.updated") {
    if (isChatPayload(payload)) {
      const chatId = payload.id || event.chatId;
      if (chatId) {
        state.chats = mergeChats(state.chats, { ...payload, id: chatId } as ImChat);
      }
    }
  }

  if (event.eventType === "chat.deleted") {
    const chatId = event.chatId || payload.id;
    const chatKey = idText(chatId);
    if (chatKey) {
      state.chats = state.chats.filter((chat) => !sameId(chat.id, chatKey));
      delete state.messagesByChat[chatKey];
      delete state.pinnedByChat[chatKey];
      if (state.activeChatId && sameId(state.activeChatId, chatKey)) {
        state.activeChatId = null;
      }
    }
  }
}
