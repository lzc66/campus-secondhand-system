<template>
  <div class="not-found-page">
    <div class="glass-card not-found-card">
      <div class="not-found-code">404</div>
      <h2>页面不存在</h2>
      <p>你访问的地址不存在或已被移除。</p>
      <div class="not-found-actions">
        <el-button type="primary" @click="router.push({ name: 'home' })">返回首页</el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}
.not-found-card {
  padding: 48px 56px;
  text-align: center;
  display: grid;
  gap: 14px;
  justify-items: center;
}
.not-found-code {
  font-family: var(--font-display);
  font-size: 88px;
  line-height: 1;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.not-found-card h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
}
.not-found-card p {
  margin: 0;
  color: var(--text-soft);
}
.not-found-actions {
  margin-top: 10px;
  display: flex;
  gap: 12px;
}
</style>
