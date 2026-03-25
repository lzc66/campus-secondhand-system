<template>
  <div class="stat-card glass-card">
    <small>{{ label }}</small>
    <strong>{{ value }}</strong>
    <p>{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps({
  label: String,
  value: [String, Number],
  hint: { type: String, default: '' }
});
</script>

<style scoped>
.stat-card { padding: 24px; min-height: 142px; }
small { display: block; margin-bottom: 14px; color: var(--text-soft); text-transform: uppercase; letter-spacing: 0.08em; }
strong { display: block; font-size: clamp(26px, 4vw, 42px); font-family: var(--font-display); }
p { margin: 10px 0 0; color: var(--text-soft); }
</style>