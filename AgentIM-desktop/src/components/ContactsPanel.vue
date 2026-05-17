<script setup lang="ts">
import { ref } from "vue";
import { MessageSquarePlus, Search, UserPlus, X } from "lucide-vue-next";
import type { Id, ImContact, ImUser } from "../types/im";
import { displayName, idText } from "../lib/format";
import AvatarBadge from "./AvatarBadge.vue";

defineProps<{
  contacts: ImContact[];
  searchUsers: ImUser[];
  loading: boolean;
}>();

const emit = defineEmits<{
  search: [q: string];
  add: [userId: Id];
  remove: [userId: Id];
  openPrivate: [userId: Id];
  refresh: [];
}>();

const query = ref("");

function doSearch() {
  if (query.value.trim()) {
    emit("search", query.value.trim());
  }
}
</script>

<template>
  <section class="side-panel">
    <header class="pane-header">
      <div>
        <h2>联系人</h2>
        <span>{{ contacts.length }} 位联系人</span>
      </div>
      <button title="刷新联系人" @click="emit('refresh')">刷新</button>
    </header>

    <div class="search-box">
      <Search :size="17" />
      <input v-model="query" placeholder="搜索用户名或昵称" @keydown.enter="doSearch" />
      <button @click="doSearch">搜索</button>
    </div>

    <div v-if="searchUsers.length" class="search-results">
      <h3>搜索结果</h3>
      <div v-for="user in searchUsers" :key="idText(user.id)" class="person-row">
        <AvatarBadge :src="user.avatar" :label="displayName(user)" :status="user.status" />
        <div>
          <strong>{{ displayName(user) }}</strong>
          <span>@{{ user.username }}</span>
        </div>
        <button title="添加联系人" @click="emit('add', user.id)"><UserPlus :size="17" /></button>
        <button title="发起私聊" @click="emit('openPrivate', user.id)"><MessageSquarePlus :size="17" /></button>
      </div>
    </div>

    <div class="people-list">
      <div v-for="contact in contacts" :key="idText(contact.id)" class="person-row">
        <AvatarBadge :src="contact.avatar" :label="contact.nickname || contact.username" :status="contact.status" />
        <div>
          <strong>{{ contact.remark || contact.nickname || contact.username }}</strong>
          <span>@{{ contact.username }}</span>
        </div>
        <button title="私聊" @click="emit('openPrivate', contact.contactUserId)"><MessageSquarePlus :size="17" /></button>
        <button title="删除联系人" @click="emit('remove', contact.contactUserId)"><X :size="17" /></button>
      </div>
      <div v-if="!contacts.length && !loading" class="pane-hint">暂无联系人</div>
    </div>
  </section>
</template>
