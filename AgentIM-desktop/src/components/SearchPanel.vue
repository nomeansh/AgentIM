<script setup lang="ts">
import { ref } from "vue";
import { MessageSquare, Search, UserPlus } from "lucide-vue-next";
import type { Id, ImSearchResult } from "../types/im";
import { displayName, formatTime, idText, messagePreview } from "../lib/format";
import AvatarBadge from "./AvatarBadge.vue";

defineProps<{
  result: ImSearchResult | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  search: [q: string, chatId?: Id];
  openChat: [chatId: Id];
  addContact: [userId: Id];
}>();

const query = ref("");

function submit() {
  if (query.value.trim()) {
    emit("search", query.value.trim());
  }
}
</script>

<template>
  <section class="side-panel">
    <header class="pane-header">
      <div>
        <h2>搜索</h2>
        <span>用户与消息</span>
      </div>
    </header>

    <div class="search-box">
      <Search :size="17" />
      <input v-model="query" placeholder="输入关键词" @keydown.enter="submit" />
      <button @click="submit">搜索</button>
    </div>

    <div v-if="loading" class="pane-hint">搜索中...</div>
    <template v-if="result">
      <section class="search-results">
        <h3>用户</h3>
        <div v-for="user in result.users" :key="idText(user.id)" class="person-row">
          <AvatarBadge :src="user.avatar" :label="displayName(user)" />
          <div>
            <strong>{{ displayName(user) }}</strong>
            <span>@{{ user.username }}</span>
          </div>
          <button title="添加联系人" @click="emit('addContact', user.id)"><UserPlus :size="17" /></button>
        </div>
        <div v-if="!result.users.length" class="pane-hint">没有匹配用户</div>
      </section>

      <section class="search-results">
        <h3>消息</h3>
        <button v-for="message in result.messages" :key="idText(message.id)" class="message-result" @click="emit('openChat', message.chatId)">
          <MessageSquare :size="17" />
          <div>
            <strong>{{ message.senderNickname || message.senderUsername || message.senderId }}</strong>
            <p>{{ messagePreview(message) }}</p>
            <span>{{ formatTime(message.createTime) }}</span>
          </div>
        </button>
        <div v-if="!result.messages.length" class="pane-hint">没有匹配消息</div>
      </section>
    </template>
  </section>
</template>
