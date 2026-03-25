<template>
  <div>
    <header class="public-header">
      <div class="page-shell nav-inner">
        <AppLogo />
        <nav>
          <RouterLink to="/" exact-active-class="is-active">首页</RouterLink>
          <RouterLink to="/items" active-class="is-active">商品</RouterLink>
          <RouterLink to="/wanted-posts" active-class="is-active">求购</RouterLink>
          <RouterLink to="/announcements" active-class="is-active">公告</RouterLink>
          <RouterLink to="/register" active-class="is-active">注册</RouterLink>
        </nav>
        <div class="actions">
          <RouterLink class="ghost-link" to="/login">学生登录</RouterLink>
          <RouterLink class="solid-link" to="/admin/login">管理入口</RouterLink>
        </div>
      </div>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
</script>

<style scoped>
.public-header {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 16px 0;
  backdrop-filter: blur(18px);
  background: rgba(244, 239, 232, 0.72);
  border-bottom: 1px solid rgba(25, 63, 58, 0.08);
}
.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
nav {
  display: flex;
  gap: 18px;
  font-size: 14px;
}
nav a.is-active {
  color: var(--accent);
}
.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
.ghost-link,
.solid-link {
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
}
.solid-link {
  background: var(--brand);
  color: #fff;
  border-color: transparent;
}
@media (max-width: 900px) {
  .nav-inner {
    flex-wrap: wrap;
  }
  nav {
    width: 100%;
    overflow: auto;
  }
}
</style>