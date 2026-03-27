<template>
  <div class="page-shell result-page">
    <section class="glass-card result-card">
      <div class="app-chip">Application Submitted</div>
      <h1>注册申请已进入审核</h1>
      <p>申请编号：{{ route.query.applicationNo || '系统已记录' }}</p>
      <p>你可以先返回首页浏览商品和公告，审核通过后再使用学号和密码登录。</p>
      <div class="result-actions"><RouterLink to="/">返回首页</RouterLink><RouterLink to="/login">前往登录页</RouterLink></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router';
const route = useRoute();
</script>

<style scoped>
.result-page { padding: 60px 0; }
.result-card { padding: 40px; text-align: center; }
h1 { font-family: var(--font-display); font-size: 52px; margin: 14px 0; }
p { color: var(--text-soft); }
.result-actions { margin-top: 22px; display: flex; gap: 14px; justify-content: center; }
.result-actions a { padding: 12px 18px; border-radius: 999px; background: rgba(25,63,58,0.08); }
</style>