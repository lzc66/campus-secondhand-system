<template>
  <div class="empty-state glass-card">
    <div class="orb"></div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '这里还没有内容，稍后再回来看看。' }
});
</script>

<style scoped>
.empty-state {
  position: relative;
  overflow: hidden;
  padding: 34px;
  text-align: center;
}
.orb {
  width: 96px;
  height: 96px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(220, 127, 57, 0.8), rgba(35, 78, 119, 0.26));
  filter: blur(4px);
}
h3 { margin: 0 0 10px; font-family: var(--font-display); font-size: 28px; }
p { margin: 0; color: var(--text-soft); }
</style>