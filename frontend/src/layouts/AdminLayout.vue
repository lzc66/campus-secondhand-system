<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <AppLogo to="/admin/dashboard" />
      <div class="admin-badge">
        <span>{{ roleLabel }}</span>
        <strong>{{ authStore.adminProfile?.adminName || '系统管理员' }}</strong>
      </div>
      <nav>
        <RouterLink to="/admin/dashboard">统计看板</RouterLink>
        <RouterLink to="/admin/registrations">注册审核</RouterLink>
        <RouterLink to="/admin/users">用户管理</RouterLink>
        <RouterLink to="/admin/items">商品管理</RouterLink>
        <RouterLink to="/admin/orders">订单管理</RouterLink>
        <RouterLink to="/admin/announcements">公告管理</RouterLink>
        <RouterLink to="/admin/mail-settings">邮件配置</RouterLink>
        <RouterLink to="/admin/reports">报表导出</RouterLink>
      </nav>
      <el-button type="danger" plain @click="logout">退出后台</el-button>
    </aside>
    <main class="admin-main">
      <div class="admin-topbar glass-card">
        <div>
          <div class="app-chip">Admin Panel</div>
          <h1>校园交易管理后台</h1>
          <p>覆盖审核、运营、邮件通知与演示管理。</p>
        </div>
        <RouterLink to="/">返回前台</RouterLink>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage } from 'element-plus';
import { RouterLink, useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    super_admin: '超级管理员',
    auditor: '审核员',
    operator: '运营管理员'
  };
  return map[authStore.adminProfile?.roleCode || ''] || '后台成员';
});

function logout() {
  authStore.clearAdminAuth();
  ElMessage.success('已退出后台');
  router.push('/admin/login');
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
}

.admin-sidebar {
  background: linear-gradient(180deg, #17302d, #203f55);
  color: rgba(255, 255, 255, 0.92);
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.admin-badge {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
}

.admin-badge span {
  display: inline-block;
  margin-bottom: 8px;
  text-transform: uppercase;
  font-size: 12px;
  opacity: 0.75;
}

nav {
  display: grid;
  gap: 8px;
}

nav a {
  padding: 12px 14px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.72);
}

nav a.router-link-active {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.admin-main {
  padding: 24px;
}

.admin-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 22px;
  margin-bottom: 22px;
}

.admin-topbar h1 {
  margin: 8px 0;
  font-family: var(--font-display);
}

.admin-topbar p {
  margin: 0;
  color: var(--text-soft);
}

@media (max-width: 1024px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }
}
</style>