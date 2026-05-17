<script setup lang="ts">
import { computed } from "vue";
import { initials } from "../lib/format";

const props = defineProps<{
  src?: string;
  label?: string;
  status?: string;
  size?: "sm" | "md" | "lg";
}>();

const text = computed(() => initials(props.label));
</script>

<template>
  <div class="avatar-badge" :class="[`avatar-${size ?? 'md'}`, status === 'online' ? 'is-online' : '']">
    <img v-if="src" :src="src" :alt="label || 'avatar'" />
    <span v-else>{{ text }}</span>
  </div>
</template>
