<script setup lang="ts">
import { computed } from "vue";
import { Edit3, Plus, RefreshCw } from "lucide-vue-next";
import type { Id, ImChat } from "../types/im";
import { chatTitle, formatTime, idText, sameId } from "../lib/format";
import AvatarBadge from "./AvatarBadge.vue";

const props = defineProps<{
  chats: ImChat[];
  activeChatId: Id | null;
  currentUserId?: Id;
  loading: boolean;
}>();

const emit = defineEmits<{
  select: [chatId: Id];
  refresh: [];
  create: [];
  saved: [];
}>();

const sortedChats = computed(() => props.chats);
</script>

<template>
  <section class="list-pane">
    <header class="pane-header">
      <div>
        <h2>会话</h2>
        <span>{{ chats.length }} 个聊天</span>
      </div>
      <div class="header-actions">
        <button title="保存的消息" @click="emit('saved')"><Edit3 :size="17" /></button>
        <button title="新建会话" @click="emit('create')"><Plus :size="18" /></button>
        <button title="刷新" @click="emit('refresh')"><RefreshCw :size="17" /></button>
      </div>
    </header>

    <div class="chat-list">
      <button
        v-for="chat in sortedChats"
        :key="idText(chat.id)"
        class="chat-row"
        :class="{ active: sameId(chat.id, activeChatId) }"
        @click="emit('select', chat.id)"
      >
        <AvatarBadge :src="chat.avatar" :label="chatTitle(chat, currentUserId)" />
        <div class="chat-row-main">
          <div class="chat-row-title">
            <strong>{{ chatTitle(chat, currentUserId) }}</strong>
            <time>{{ formatTime(chat.lastMsgTime) }}</time>
          </div>
          <p>{{ chat.lastMsgContent || chat.description || "暂无消息" }}</p>
        </div>
        <span v-if="Number(chat.unreadCount || 0) > 0" class="unread-pill">{{ chat.unreadCount }}</span>
      </button>
    </div>
    <div v-if="loading" class="pane-hint">加载中...</div>
    <div v-else-if="!chats.length" class="pane-hint">暂无会话</div>
  </section>
</template>
