<template>
  <div class="section-title">
    <div>
      <div v-if="tag" class="app-chip">{{ tag }}</div>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  tag?: string;
}>();
</script>