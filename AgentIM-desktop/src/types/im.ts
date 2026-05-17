export type Id = string | number;

export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

export interface LoginRequest {
  clientId: string;
  grantType: "password";
  tenantId?: string;
  username: string;
  password: string;
  code?: string;
  uuid?: string;
}

export interface RegisterRequest extends LoginRequest {
  userType?: string;
}

export interface LoginVo {
  access_token: string;
  refresh_token?: string;
  expire_in?: number;
  refresh_expire_in?: number;
  client_id?: string;
  scope?: string;
  openid?: string;
}

export interface ImUser {
  id: Id;
  username: string;
  nickname?: string;
  avatar?: string;
  bio?: string;
  phone?: string;
  email?: string;
  status?: string;
  createTime?: string;
}

export interface ImUserProfileUpdate {
  nickname?: string;
  bio?: string;
  avatar?: string;
  phone?: string;
  email?: string;
}

export interface ImContact {
  id: Id;
  contactUserId: Id;
  username: string;
  nickname?: string;
  avatar?: string;
  remark?: string;
  status?: string;
}

export interface ImChatMember {
  id: Id;
  chatId: Id;
  userId: Id;
  username?: string;
  nickname?: string;
  avatar?: string;
  role: string;
  joinedTime?: string;
  muted?: string;
}

export interface ImChat {
  id: Id;
  type: "private" | "group" | "channel" | "saved" | string;
  title?: string;
  avatar?: string;
  description?: string;
  ownerId?: Id;
  seq?: Id;
  lastMsgId?: Id;
  lastMsgContent?: string;
  lastMsgTime?: string;
  status?: string;
  unreadCount?: Id;
  members?: ImChatMember[];
}

export interface ImChatCreate {
  type: "private" | "group" | "channel";
  title?: string;
  avatar?: string;
  description?: string;
  memberIds?: Id[];
}

export interface ImChatUpdate {
  title?: string;
  avatar?: string;
  description?: string;
}

export interface ImChatMemberUpdate {
  userId: Id;
  role?: string;
}

export interface ImReactionSummary {
  reaction: string;
  count: Id;
}

export interface ImReaction {
  id: Id;
  messageId: Id;
  userId: Id;
  username?: string;
  nickname?: string;
  reaction: string;
}

export interface ImMessage {
  id: Id;
  chatId: Id;
  senderId: Id;
  senderUsername?: string;
  senderNickname?: string;
  messageType: "text" | "image" | "file" | "voice" | "video" | "poll" | "forward" | string;
  content?: string;
  contentPayload?: string;
  resourceIds?: string;
  replyToMessageId?: Id;
  forwardFromMessageId?: Id;
  forwardFromChatId?: Id;
  forwardSenderId?: Id;
  clientMsgId?: string;
  idempotentKey?: string;
  seq?: Id;
  replyCount?: number;
  status: string;
  editTime?: string;
  deleteTime?: string;
  createTime?: string;
  reactions?: ImReactionSummary[];
}

export interface ImMessageSend {
  messageType: string;
  content?: string;
  contentPayload?: string;
  resourceIds?: Id[];
  replyToMessageId?: Id;
  forwardFromMessageId?: Id;
  forwardFromChatId?: Id;
  clientMsgId?: string;
  idempotentKey: string;
}

export interface ImMessageEdit {
  content?: string;
  contentPayload?: string;
  resourceIds?: Id[];
}

export interface ImResourceUpload {
  resourceId: Id;
  originalName: string;
  url?: string;
  thumbnailUrl?: string;
}

export interface ImResource {
  id: Id;
  chatId?: Id;
  messageId?: Id;
  uploaderId?: Id;
  resourceType?: string;
  originalName?: string;
  mimeType?: string;
  size?: Id;
  storageProvider?: string;
  objectKey?: string;
  thumbnailKey?: string;
  width?: number;
  height?: number;
  duration?: number;
  accessLevel?: string;
  url?: string;
}

export interface ImPinnedMessage {
  id: Id;
  chatId: Id;
  messageId: Id;
  pinnedBy?: Id;
  pinnedTime?: string;
  message?: ImMessage;
}

export interface ImPollOption {
  id: Id;
  pollId: Id;
  text: string;
  ordinal?: number;
  voteCount?: Id;
  selectedByMe?: boolean;
}

export interface ImPoll {
  id: Id;
  messageId: Id;
  question: string;
  multiple?: string;
  anonymous?: string;
  status?: string;
  closeTime?: string;
  options: ImPollOption[];
}

export interface ImPollVote {
  optionIds: Id[];
}

export interface ImReadMember {
  userId: Id;
  username?: string;
  nickname?: string;
  avatar?: string;
  readTime?: string;
}

export interface ImMessageReadStatus {
  status?: string;
  totalCount?: number;
  readCount?: number;
  ratio?: number;
  readBy?: ImReadMember[];
  unreadBy?: ImReadMember[];
}

export interface ImSearchResult {
  messages: ImMessage[];
  users: ImUser[];
}

export interface ImEvent<TPayload = unknown> {
  eventId?: string;
  eventType: string;
  chatId?: Id;
  actorId?: Id;
  payload?: TPayload;
  occurredAt?: string;
}

export interface ConnectionSettings {
  authBaseUrl: string;
  apiBaseUrl: string;
  wsPath: string;
  clientId: string;
  tenantId: string;
}
