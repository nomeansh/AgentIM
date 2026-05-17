<script setup lang="ts">
import { Archive, LogOut, MessageSquare, Search, Settings, UserRound, UsersRound } from "lucide-vue-next";

defineProps<{
  view: string;
  socketStatus: string;
}>();

const emit = defineEmits<{
  change: [view: "chats" | "contacts" | "search" | "settings"];
  logout: [];
}>();
</script>

<template>
  <aside class="sidebar-nav">
    <button title="会话" :class="{ active: view === 'chats' }" @click="emit('change', 'chats')"><MessageSquare :size="20" /></button>
    <button title="联系人" :class="{ active: view === 'contacts' }" @click="emit('change', 'contacts')"><UsersRound :size="20" /></button>
    <button title="搜索" :class="{ active: view === 'search' }" @click="emit('change', 'search')"><Search :size="20" /></button>
    <button title="设置" :class="{ active: view === 'settings' }" @click="emit('change', 'settings')"><Settings :size="20" /></button>
    <div class="nav-spacer"></div>
    <div class="socket-dot" :class="socketStatus" :title="`WebSocket: ${socketStatus}`"></div>
    <button title="退出" @click="emit('logout')"><LogOut :size="20" /></button>
  </aside>
</template>
