import type {
  ConnectionSettings,
  Id,
  ImChat,
  ImChatCreate,
  ImChatMember,
  ImChatMemberUpdate,
  ImChatUpdate,
  ImContact,
  ImMessage,
  ImMessageEdit,
  ImMessageReadStatus,
  ImMessageSend,
  ImPinnedMessage,
  ImPoll,
  ImPollVote,
  ImReaction,
  ImResource,
  ImResourceUpload,
  ImSearchResult,
  ImUser,
  ImUserProfileUpdate,
  LoginRequest,
  LoginVo,
  RegisterRequest
} from "../types/im";
import { request } from "../lib/http";

export class AgentImApi {
  constructor(
    private readonly settings: Pick<ConnectionSettings, "authBaseUrl" | "apiBaseUrl" | "clientId">,
    private readonly getToken: () => string | undefined
  ) {}

  login(body: LoginRequest): Promise<LoginVo> {
    return request<LoginVo>(this.settings.authBaseUrl, "/login", { method: "POST", body });
  }

  register(body: RegisterRequest): Promise<void> {
    return request<void>(this.settings.authBaseUrl, "/register", { method: "POST", body });
  }

  logout(): Promise<void> {
    return request<void>(this.settings.authBaseUrl, "/logout", {
      method: "POST",
      token: this.getToken(),
      clientId: this.settings.clientId
    });
  }

  profile(): Promise<ImUser> {
    return this.get<ImUser>("/im/users/profile");
  }

  updateProfile(body: ImUserProfileUpdate): Promise<ImUser> {
    return this.put<ImUser>("/im/users/profile", body);
  }

  searchUsers(q: string, limit = 20): Promise<ImUser[]> {
    return this.get<ImUser[]>("/im/users/search", { q, limit });
  }

  listContacts(): Promise<ImContact[]> {
    return this.get<ImContact[]>("/im/contacts");
  }

  addContact(contactUserId: Id): Promise<ImContact> {
    return this.post<ImContact>("/im/contacts", { contactUserId });
  }

  removeContact(contactUserId: Id): Promise<void> {
    return this.delete<void>(`/im/contacts/${contactUserId}`);
  }

  listChats(): Promise<ImChat[]> {
    return this.get<ImChat[]>("/im/chats");
  }

  savedChat(): Promise<ImChat> {
    return this.get<ImChat>("/im/chats/saved");
  }

  getChat(chatId: Id): Promise<ImChat> {
    return this.get<ImChat>(`/im/chats/${chatId}`);
  }

  createChat(body: ImChatCreate): Promise<ImChat> {
    return this.post<ImChat>("/im/chats", body);
  }

  updateChat(chatId: Id, body: ImChatUpdate): Promise<ImChat> {
    return this.put<ImChat>(`/im/chats/${chatId}`, body);
  }

  deleteChat(chatId: Id): Promise<void> {
    return this.delete<void>(`/im/chats/${chatId}`);
  }

  archiveChat(chatId: Id): Promise<void> {
    return this.post<void>(`/im/chats/${chatId}/archive`);
  }

  unarchiveChat(chatId: Id): Promise<void> {
    return this.post<void>(`/im/chats/${chatId}/unarchive`);
  }

  listMembers(chatId: Id): Promise<ImChatMember[]> {
    return this.get<ImChatMember[]>(`/im/chats/${chatId}/members`);
  }

  addMember(chatId: Id, body: ImChatMemberUpdate): Promise<ImChatMember> {
    return this.post<ImChatMember>(`/im/chats/${chatId}/members`, body);
  }

  updateMember(chatId: Id, userId: Id, body: ImChatMemberUpdate): Promise<ImChatMember> {
    return this.put<ImChatMember>(`/im/chats/${chatId}/members/${userId}`, body);
  }

  removeMember(chatId: Id, userId: Id): Promise<void> {
    return this.delete<void>(`/im/chats/${chatId}/members/${userId}`);
  }

  pinnedMessages(chatId: Id): Promise<ImPinnedMessage[]> {
    return this.get<ImPinnedMessage[]>(`/im/chats/${chatId}/pinned`);
  }

  listMessages(chatId: Id, beforeSeq?: Id, limit = 30): Promise<ImMessage[]> {
    return this.get<ImMessage[]>(`/im/chats/${chatId}/messages`, { beforeSeq, limit });
  }

  sendMessage(chatId: Id, body: ImMessageSend): Promise<ImMessage> {
    return this.post<ImMessage>(`/im/chats/${chatId}/messages`, body);
  }

  editMessage(messageId: Id, body: ImMessageEdit): Promise<ImMessage> {
    return this.put<ImMessage>(`/im/messages/${messageId}`, body);
  }

  deleteForAll(messageId: Id): Promise<void> {
    return this.delete<void>(`/im/messages/${messageId}`);
  }

  hideForSelf(messageId: Id): Promise<void> {
    return this.post<void>(`/im/messages/${messageId}/hide`);
  }

  listReactions(messageId: Id): Promise<ImReaction[]> {
    return this.get<ImReaction[]>(`/im/messages/${messageId}/reactions`);
  }

  addReaction(messageId: Id, reaction: string): Promise<ImReaction> {
    return this.post<ImReaction>(`/im/messages/${messageId}/reactions`, { reaction });
  }

  deleteReaction(messageId: Id, userId: Id, reaction: string): Promise<void> {
    return this.delete<void>(`/im/messages/${messageId}/reactions/${userId}/${encodeURIComponent(reaction)}`);
  }

  markRead(messageId: Id): Promise<void> {
    return this.post<void>(`/im/messages/${messageId}/read`);
  }

  unreadCount(chatId: Id): Promise<Id> {
    return this.get<Id>(`/im/chats/${chatId}/unread-count`);
  }

  readStatus(messageId: Id): Promise<ImMessageReadStatus> {
    return this.get<ImMessageReadStatus>(`/im/messages/${messageId}/read-status`);
  }

  pinMessage(messageId: Id): Promise<ImPinnedMessage> {
    return this.post<ImPinnedMessage>(`/im/messages/${messageId}/pin`);
  }

  unpinMessage(messageId: Id): Promise<void> {
    return this.delete<void>(`/im/messages/${messageId}/pin`);
  }

  uploadResource(file: File): Promise<ImResourceUpload> {
    const formData = new FormData();
    formData.set("file", file);
    return request<ImResourceUpload>(this.settings.apiBaseUrl, "/im/resources/upload", {
      method: "POST",
      token: this.getToken(),
      clientId: this.settings.clientId,
      formData
    });
  }

  getResource(resourceId: Id): Promise<ImResource> {
    return this.get<ImResource>(`/im/resources/${resourceId}`);
  }

  downloadResource(resourceId: Id): Promise<string> {
    return this.get<string>(`/im/resources/${resourceId}/download`);
  }

  search(q: string, chatId?: Id, limit = 20): Promise<ImSearchResult> {
    return this.get<ImSearchResult>("/im/search", { q, chatId, limit });
  }

  getPoll(messageId: Id): Promise<ImPoll> {
    return this.get<ImPoll>(`/im/messages/${messageId}/poll`);
  }

  vote(messageId: Id, body: ImPollVote): Promise<ImPoll> {
    return this.post<ImPoll>(`/im/messages/${messageId}/poll/vote`, body);
  }

  private get<T>(path: string, query?: Record<string, unknown>): Promise<T> {
    return request<T>(this.settings.apiBaseUrl, path, { query, token: this.getToken(), clientId: this.settings.clientId });
  }

  private post<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(this.settings.apiBaseUrl, path, { method: "POST", body, token: this.getToken(), clientId: this.settings.clientId });
  }

  private put<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(this.settings.apiBaseUrl, path, { method: "PUT", body, token: this.getToken(), clientId: this.settings.clientId });
  }

  private delete<T>(path: string): Promise<T> {
    return request<T>(this.settings.apiBaseUrl, path, { method: "DELETE", token: this.getToken(), clientId: this.settings.clientId });
  }
}
