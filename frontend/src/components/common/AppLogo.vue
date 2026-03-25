<template>
  <RouterLink class="logo" :to="to">
    <span class="logo-mark">CB</span>
    <span>
      <strong>Campus Bazaar</strong>
      <small>校园二手交易板</small>
    </span>
  </RouterLink>
</template>

<script setup lang="ts">
defineProps({
  to: { type: String, default: '/' }
});
</script>

<style scoped>
.logo {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}
.logo-mark {
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  color: #fff8f0;
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 0.08em;
  box-shadow: 0 10px 30px rgba(25, 63, 58, 0.22);
}
strong { display: block; font-size: 16px; }
small { color: var(--text-soft); }
</style>