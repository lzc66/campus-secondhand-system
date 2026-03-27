<template>
  <div class="portal-wrap page-shell">
    <aside class="sidebar glass-card">
      <AppLogo to="/" />
      <div class="user-box">
        <img v-if="authStore.userProfile?.avatarUrl" :src="authStore.userProfile.avatarUrl" alt="avatar" />
        <div v-else class="avatar-fallback">{{ authStore.userProfile?.realName?.slice(0,1) || 'U' }}</div>
        <div>
          <strong>{{ authStore.userProfile?.realName || '校园用户' }}</strong>
          <p>{{ authStore.userProfile?.studentNo }}</p>
        </div>
      </div>
      <nav>
        <RouterLink to="/user/profile">个人资料</RouterLink>
        <RouterLink to="/user/my-items">我的商品</RouterLink>
        <RouterLink to="/user/publish">发布商品</RouterLink>
        <RouterLink to="/user/orders">我的订单</RouterLink>
        <RouterLink to="/user/wanted-posts">我的求购</RouterLink>
        <RouterLink to="/user/notifications">通知中心</RouterLink>
        <RouterLink to="/user/recommendations">推荐好物</RouterLink>
        <RouterLink to="/user/comments">评论回复</RouterLink>
      </nav>
      <el-button type="danger" plain @click="logout">退出登录</el-button>
    </aside>
    <section class="content-area">
      <router-view />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { RouterLink, useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

function logout() {
  authStore.clearUserAuth();
  ElMessage.success('已退出登录');
  router.push('/');
}
</script>

<style scoped>
.portal-wrap { display: grid; grid-template-columns: 280px 1fr; gap: 22px; padding: 28px 0 34px; }
.sidebar { padding: 24px; position: sticky; top: 96px; height: fit-content; }
.user-box { display: flex; gap: 14px; align-items: center; margin: 28px 0 22px; padding: 14px; border-radius: 18px; background: rgba(25, 63, 58, 0.06); }
.user-box img, .avatar-fallback { width: 54px; height: 54px; border-radius: 18px; object-fit: cover; }
.avatar-fallback { display: grid; place-items: center; background: linear-gradient(135deg, var(--brand), var(--accent)); color: #fff; font-family: var(--font-display); font-size: 22px; }
.user-box p { margin: 4px 0 0; color: var(--text-soft); }
nav { display: grid; gap: 8px; margin-bottom: 22px; }
nav a { padding: 12px 14px; border-radius: 14px; color: var(--text-soft); }
nav a.router-link-active { background: var(--brand); color: #fff; }
.content-area { min-width: 0; }
@media (max-width: 1024px) { .portal-wrap { grid-template-columns: 1fr; } .sidebar { position: static; } }
</style>