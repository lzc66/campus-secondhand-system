<template>
  <div class="empty-state glass-card fade-up">
    <div class="orb-wrap" aria-hidden="true">
      <span class="ring ring-one"></span>
      <span class="ring ring-two"></span>
      <span class="orb"></span>
    </div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <div v-if="$slots.default" class="actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '这里还没有数据，稍后再回来看看。' }
});
</script>

<style scoped>
.empty-state {
  position: relative;
  overflow: hidden;
  padding: 38px 30px;
  text-align: center;
}
.orb-wrap {
  position: relative;
  width: 118px;
  height: 118px;
  margin: 0 auto 18px;
}
.ring,
.orb {
  position: absolute;
  inset: 0;
  border-radius: 50%;
}
.ring-one {
  border: 1px solid rgba(220, 127, 57, 0.28);
  animation: drift 7s ease-in-out infinite;
}
.ring-two {
  inset: 10px;
  border: 1px dashed rgba(35, 78, 119, 0.2);
  animation: driftReverse 9s linear infinite;
}
.orb {
  inset: 22px;
  background: radial-gradient(circle at 30% 30%, rgba(247, 179, 96, 0.95), rgba(35, 78, 119, 0.28) 72%);
  box-shadow: 0 18px 44px rgba(35, 78, 119, 0.16);
  filter: blur(2px);
  animation: float 3.6s ease-in-out infinite, pulse 4.8s ease-in-out infinite;
}
h3 {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: 28px;
}
p {
  max-width: 460px;
  margin: 0 auto;
  color: var(--text-soft);
  line-height: 1.8;
}
.actions {
  margin-top: 18px;
  display: inline-flex;
  justify-content: center;
}
@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}
@keyframes drift {
  0%, 100% { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(12deg) scale(1.05); }
}
@keyframes driftReverse {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(-360deg); }
}
</style>