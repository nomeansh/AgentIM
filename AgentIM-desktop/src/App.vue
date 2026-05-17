<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import type {
  ConnectionSettings,
  Id,
  ImChatCreate,
  ImEvent,
  ImMessage,
  ImMessageSend,
  ImPoll,
  ImUser,
  ImUserProfileUpdate,
  LoginRequest,
  RegisterRequest
} from "./types/im";
import { AgentImApi } from "./services/api";
import { clearToken, loadSettings, loadToken, saveSettings as persistSettings, saveToken } from "./services/session";
import { applyEventToState, createInitialState, insertOrReplaceMessage, mergeChats, sortMessagesAscending } from "./stores/imStore";
import { connectSocket, type SocketController } from "./lib/ws";
import { idText, sameId } from "./lib/format";
import LoginPanel from "./components/LoginPanel.vue";
import SidebarNav from "./components/SidebarNav.vue";
import ChatList from "./components/ChatList.vue";
import MessageThread from "./components/MessageThread.vue";
import ComposerBar from "./components/ComposerBar.vue";
import ContactsPanel from "./components/ContactsPanel.vue";
import SearchPanel from "./components/SearchPanel.vue";
import SettingsPanel from "./components/SettingsPanel.vue";
import CreateChatDialog from "./components/CreateChatDialog.vue";

const state = reactive(createInitialState());
state.settings = loadSettings();
state.token = loadToken();

const view = ref<"chats" | "contacts" | "search" | "settings">("chats");
const createDialogOpen = ref(false);
const uploadBusy = ref(false);
const userSearchResults = ref<ImUser[]>([]);
let socket: SocketController | null = null;

const activeChat = computed(() => state.chats.find((chat) => sameId(chat.id, state.activeChatId)) ?? null);
const activeMessages = computed(() => (state.activeChatId ? state.messagesByChat[idText(state.activeChatId)] ?? [] : []));
const activePinned = computed(() => (state.activeChatId ? state.pinnedByChat[idText(state.activeChatId)] ?? [] : []));
const isAuthenticated = computed(() => Boolean(state.token));

function api(settings = state.settings): AgentImApi {
  return new AgentImApi(settings, () => state.token);
}

function setError(error: unknown): void {
  state.error = error instanceof Error ? error.message : String(error);
}

async function runTask(task: () => Promise<void>): Promise<void> {
  state.loading = true;
  state.error = "";
  try {
    await task();
  } catch (error) {
    setError(error);
  } finally {
    state.loading = false;
  }
}

async function handleLogin(body: LoginRequest, settings: ConnectionSettings): Promise<void> {
  await runTask(async () => {
    saveConnectionSettings(settings);
    const login = await api(settings).login(body);
    state.token = saveToken(login);
    await loadWorkspace();
    openSocket();
  });
}

async function handleRegister(body: RegisterRequest, settings: ConnectionSettings): Promise<void> {
  await runTask(async () => {
    saveConnectionSettings(settings);
    await api(settings).register(body);
    const login = await api(settings).login(body);
    state.token = saveToken(login);
    await loadWorkspace();
    openSocket();
  });
}

function saveConnectionSettings(settings: ConnectionSettings): void {
  state.settings = { ...settings };
  persistSettings(state.settings);
}

async function loadWorkspace(): Promise<void> {
  const client = api();
  const [profile, chats, contacts] = await Promise.all([client.profile(), client.listChats(), client.listContacts()]);
  state.profile = profile;
  state.chats = chats;
  state.contacts = contacts;
  if (!state.activeChatId && chats.length) {
    await selectChat(chats[0].id);
  } else if (state.activeChatId) {
    await selectChat(state.activeChatId);
  }
}

async function selectChat(chatId: Id): Promise<void> {
  state.activeChatId = chatId;
  view.value = "chats";
  const client = api();
  const [chat, messages, pinned] = await Promise.all([client.getChat(chatId), client.listMessages(chatId), client.pinnedMessages(chatId)]);
  state.chats = mergeChats(state.chats, chat);
  state.messagesByChat[idText(chatId)] = sortMessagesAscending(messages);
  state.pinnedByChat[idText(chatId)] = pinned;
  await hydratePolls(messages);
  const newest = state.messagesByChat[idText(chatId)].at(-1);
  if (newest) {
    await client.markRead(newest.id).catch(() => undefined);
  }
}

async function hydratePolls(messages: ImMessage[]): Promise<void> {
  const pollMessages = messages.filter((message) => message.messageType === "poll");
  const results = await Promise.all(
    pollMessages.map((message) =>
      api()
        .getPoll(message.id)
        .then((poll) => [idText(message.id), poll] as const)
        .catch(() => null)
    )
  );
  results.forEach((item) => {
    if (item) {
      state.pollsByMessage[item[0]] = item[1];
    }
  });
}

async function loadMoreMessages(): Promise<void> {
  if (!state.activeChatId) {
    return;
  }
  await runTask(async () => {
    const key = idText(state.activeChatId);
    const first = state.messagesByChat[key]?.[0];
    const earlier = await api().listMessages(state.activeChatId as Id, first?.seq);
    state.messagesByChat[key] = sortMessagesAscending([...earlier, ...(state.messagesByChat[key] ?? [])]);
    await hydratePolls(earlier);
  });
}

async function sendMessage(message: ImMessageSend, file?: File): Promise<void> {
  if (!state.activeChatId) {
    return;
  }
  await runTask(async () => {
    let body = { ...message };
    if (file) {
      uploadBusy.value = true;
      try {
        const uploaded = await api().uploadResource(file);
        body = { ...body, resourceIds: [uploaded.resourceId] };
      } finally {
        uploadBusy.value = false;
      }
    }
    const created = await api().sendMessage(state.activeChatId as Id, body);
    const key = idText(created.chatId);
    state.messagesByChat[key] = insertOrReplaceMessage(state.messagesByChat[key] ?? [], created);
    state.chats = state.chats.map((chat) =>
      sameId(chat.id, created.chatId)
        ? { ...chat, lastMsgId: created.id, lastMsgContent: created.content, lastMsgTime: created.createTime, seq: created.seq }
        : chat
    );
    if (created.messageType === "poll") {
      await hydratePolls([created]);
    }
  });
}

async function addReaction(message: ImMessage, reaction: string): Promise<void> {
  await runTask(async () => {
    await api().addReaction(message.id, reaction);
    if (state.activeChatId) {
      const updated = await api().listMessages(state.activeChatId);
      state.messagesByChat[idText(state.activeChatId)] = sortMessagesAscending(updated);
    }
  });
}

async function pinMessage(message: ImMessage): Promise<void> {
  await runTask(async () => {
    await api().pinMessage(message.id);
    if (state.activeChatId) {
      state.pinnedByChat[idText(state.activeChatId)] = await api().pinnedMessages(state.activeChatId);
    }
  });
}

async function deleteMessage(message: ImMessage): Promise<void> {
  await runTask(async () => {
    await api().deleteForAll(message.id);
    if (state.activeChatId) {
      state.messagesByChat[idText(state.activeChatId)] = (state.messagesByChat[idText(state.activeChatId)] ?? []).map((item) =>
        sameId(item.id, message.id) ? { ...item, status: "deleted_all", content: "[消息已撤回]" } : item
      );
    }
  });
}

async function hideMessage(message: ImMessage): Promise<void> {
  await runTask(async () => {
    await api().hideForSelf(message.id);
    if (state.activeChatId) {
      state.messagesByChat[idText(state.activeChatId)] = (state.messagesByChat[idText(state.activeChatId)] ?? []).filter((item) => !sameId(item.id, message.id));
    }
  });
}

async function markRead(message: ImMessage): Promise<void> {
  await api().markRead(message.id).catch(() => undefined);
}

async function vote(message: ImMessage, optionIds: Id[]): Promise<void> {
  await runTask(async () => {
    let poll: ImPoll | undefined = state.pollsByMessage[idText(message.id)];
    if (!poll) {
      poll = await api().getPoll(message.id);
    }
    const normalizedIds = optionIds.map((id) => {
      const index = Number(id);
      return poll?.options[index]?.id ?? id;
    });
    state.pollsByMessage[idText(message.id)] = await api().vote(message.id, { optionIds: normalizedIds });
  });
}

async function createChat(body: ImChatCreate): Promise<void> {
  await runTask(async () => {
    const chat = await api().createChat(body);
    state.chats = mergeChats(state.chats, chat);
    createDialogOpen.value = false;
    await selectChat(chat.id);
  });
}

async function openSavedChat(): Promise<void> {
  await runTask(async () => {
    const chat = await api().savedChat();
    state.chats = mergeChats(state.chats, chat);
    await selectChat(chat.id);
  });
}

async function searchUsers(q: string): Promise<void> {
  await runTask(async () => {
    userSearchResults.value = await api().searchUsers(q);
  });
}

async function addContact(userId: Id): Promise<void> {
  await runTask(async () => {
    await api().addContact(userId);
    state.contacts = await api().listContacts();
  });
}

async function removeContact(userId: Id): Promise<void> {
  await runTask(async () => {
    await api().removeContact(userId);
    state.contacts = await api().listContacts();
  });
}

async function openPrivate(userId: Id): Promise<void> {
  await createChat({ type: "private", memberIds: [userId] });
}

async function searchAll(q: string, chatId?: Id): Promise<void> {
  await runTask(async () => {
    state.searchResult = await api().search(q, chatId);
  });
}

async function refreshContacts(): Promise<void> {
  await runTask(async () => {
    state.contacts = await api().listContacts();
  });
}

async function updateProfile(profile: ImUserProfileUpdate): Promise<void> {
  await runTask(async () => {
    state.profile = await api().updateProfile(profile);
  });
}

async function syncChatSnapshot(chatId: Id): Promise<void> {
  try {
    const chat = await api().getChat(chatId);
    state.chats = mergeChats(state.chats, chat);
  } catch {
    // 聊天同步失败时不打断实时事件处理，交给后续刷新兜底
  }
}

function openSocket(): void {
  socket?.close();
  if (!state.token) {
    return;
  }
  socket = connectSocket({
    baseUrl: state.settings.apiBaseUrl,
    token: state.token,
    wsPath: state.settings.wsPath,
    onStatus: (status) => {
      state.socketStatus = status;
    },
    onEvent: (event: ImEvent) => {
      applyEventToState(state, event);
      if ((event.eventType === "chat.created" || event.eventType === "chat.updated") && event.chatId) {
        void syncChatSnapshot(event.chatId);
      }
      if (event.eventType === "message.created" && state.activeChatId && sameId(event.chatId, state.activeChatId)) {
        const payload = event.payload as ImMessage | undefined;
        if (payload) {
          markRead(payload);
        }
      }
    }
  });
}

async function logout(): Promise<void> {
  await api().logout().catch(() => undefined);
  socket?.close();
  socket = null;
  clearToken();
  state.token = "";
  state.profile = null;
  state.chats = [];
  state.contacts = [];
  state.messagesByChat = {};
  state.pinnedByChat = {};
  state.pollsByMessage = {};
  state.activeChatId = null;
  state.socketStatus = "idle";
}

onMounted(async () => {
  if (state.token) {
    await runTask(loadWorkspace);
    openSocket();
  }
});

onBeforeUnmount(() => {
  socket?.close();
});
</script>

<template>
  <LoginPanel
    v-if="!isAuthenticated"
    :settings="state.settings"
    :loading="state.loading"
    :error="state.error"
    @login="handleLogin"
    @register="handleRegister"
    @update-settings="saveConnectionSettings"
  />

  <main v-else class="desktop-shell">
    <SidebarNav :view="view" :socket-status="state.socketStatus" @change="view = $event" @logout="logout" />

    <ChatList
      v-if="view === 'chats'"
      :chats="state.chats"
      :active-chat-id="state.activeChatId"
      :current-user-id="state.profile?.id"
      :loading="state.loading"
      @select="selectChat"
      @refresh="loadWorkspace"
      @create="createDialogOpen = true"
      @saved="openSavedChat"
    />
    <ContactsPanel
      v-else-if="view === 'contacts'"
      :contacts="state.contacts"
      :search-users="userSearchResults"
      :loading="state.loading"
      @search="searchUsers"
      @add="addContact"
      @remove="removeContact"
      @open-private="openPrivate"
      @refresh="refreshContacts"
    />
    <SearchPanel
      v-else-if="view === 'search'"
      :result="state.searchResult"
      :loading="state.loading"
      @search="searchAll"
      @open-chat="selectChat"
      @add-contact="addContact"
    />
    <SettingsPanel
      v-else
      :settings="state.settings"
      :profile="state.profile"
      :loading="state.loading"
      @save-settings="(settings) => { saveConnectionSettings(settings); openSocket(); }"
      @save-profile="updateProfile"
      @reload="loadWorkspace"
    />

    <section class="conversation-area">
      <MessageThread
        :chat="activeChat"
        :messages="activeMessages"
        :pinned="activePinned"
        :polls="state.pollsByMessage"
        :profile="state.profile"
        :loading="state.loading"
        @load-more="loadMoreMessages"
        @reaction="addReaction"
        @pin="pinMessage"
        @delete-message="deleteMessage"
        @hide-message="hideMessage"
        @read="markRead"
        @poll="vote"
      />
      <ComposerBar :chat="activeChat" :uploading="uploadBusy" @send="sendMessage" />
      <p v-if="state.error" class="app-error">{{ state.error }}</p>
    </section>

    <CreateChatDialog :open="createDialogOpen" @close="createDialogOpen = false" @create="createChat" />
  </main>
</template>
