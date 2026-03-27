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
          <template v-if="authStore.isUserLoggedIn">
            <div class="user-brief">
              <span class="login-chip">已登录</span>
              <div>
                <strong>{{ authStore.userProfile?.realName || '校园用户' }}</strong>
                <p>{{ authStore.userProfile?.studentNo || '当前账号' }}</p>
              </div>
            </div>
            <el-dropdown trigger="click" @command="handleUserCommand">
              <button class="profile-trigger" type="button">
                <img v-if="authStore.userProfile?.avatarUrl" :src="authStore.userProfile.avatarUrl" alt="用户头像" />
                <span v-else class="avatar-fallback">{{ avatarFallback }}</span>
                <span class="trigger-copy">
                  <strong>{{ authStore.userProfile?.realName || '校园用户' }}</strong>
                  <small>{{ currentUserLabel }}</small>
                </span>
              </button>
              <template #dropdown>
                <el-dropdown-menu class="user-menu">
                  <el-dropdown-item
                    v-for="item in userMenuItems"
                    :key="item.to"
                    :command="item.to"
                    :class="{ 'is-current': route.path === item.to }"
                  >
                    <div class="menu-copy">
                      <strong>{{ item.label }}</strong>
                      <span>{{ item.description }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <div class="menu-copy danger-copy">
                      <strong>退出登录</strong>
                      <span>结束当前学生端会话并返回前台首页</span>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <RouterLink class="solid-link" to="/login">用户登录</RouterLink>
          </template>
        </div>
      </div>
    </header>
    <main>
      <RouterView v-slot="{ Component }">
        <div v-if="isUserArea" class="page-shell user-shell">
          <component :is="Component" />
        </div>
        <component :is="Component" v-else />
      </RouterView>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { userApi } from '@/api/user';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const userMenuItems = [
  { to: '/user/profile', label: '个人资料', description: '查看并维护实名资料、联系方式和头像' },
  { to: '/user/my-items', label: '我的商品', description: '管理已发布商品，支持编辑和删除' },
  { to: '/user/publish', label: '发布商品', description: '上传图片并发布新的二手商品' },
  { to: '/user/orders', label: '我的订单', description: '在买家和卖家视角之间切换查看订单' },
  { to: '/user/wanted-posts', label: '我的求购', description: '维护自己发布的求购信息' },
  { to: '/user/notifications', label: '通知中心', description: '查看审核、交易和系统通知' },
  { to: '/user/recommendations', label: '推荐好物', description: '查看系统根据行为生成的个性化推荐' },
  { to: '/user/comments', label: '评论回复', description: '查看收到的评论并进行回复' }
];

const avatarFallback = computed(() => authStore.userProfile?.realName?.slice(0, 1) || '用');
const isUserArea = computed(() => route.path.startsWith('/user/'));
const currentUserLabel = computed(() => {
  if (isUserArea.value) {
    const current = userMenuItems.find((item) => item.to === route.path);
    return current?.label || '用户中心';
  }
  return '前台浏览中';
});

onMounted(async () => {
  if (!authStore.isUserLoggedIn || authStore.userProfile) return;
  try {
    const profile = await userApi.getCurrentUser();
    authStore.setUserAuth(authStore.userToken, profile);
  } catch {
    authStore.clearUserAuth();
  }
});

async function handleUserCommand(command: string) {
  if (command === 'logout') {
    authStore.clearUserAuth();
    ElMessage.success('已退出用户登录');
    if (route.path.startsWith('/user/')) {
      await router.push('/');
    }
    return;
  }
  await router.push(command);
}
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

.solid-link {
  padding: 10px 18px;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  border: 1px solid transparent;
}

.user-brief {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 18px;
  background: rgba(25, 63, 58, 0.06);
}

.user-brief strong,
.trigger-copy strong {
  display: block;
  font-size: 14px;
}

.user-brief p,
.trigger-copy small {
  color: var(--text-soft);
  font-size: 12px;
}

.user-brief p {
  margin: 2px 0 0;
}

.login-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(220, 127, 57, 0.14);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}

.profile-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 8px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.profile-trigger:hover {
  transform: translateY(-1px);
  border-color: rgba(220, 127, 57, 0.4);
  box-shadow: 0 16px 30px rgba(25, 63, 58, 0.08);
}

.profile-trigger img,
.avatar-fallback {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  color: #fff;
  font-family: var(--font-display);
  font-size: 18px;
}

.trigger-copy {
  display: grid;
  text-align: left;
  min-width: 96px;
}

:deep(.user-menu) {
  width: 280px;
  padding: 8px;
  border-radius: 18px;
}

:deep(.user-menu .el-dropdown-menu__item) {
  padding: 12px 14px;
  border-radius: 14px;
  line-height: 1.4;
  white-space: normal;
}

:deep(.user-menu .el-dropdown-menu__item.is-current) {
  background: rgba(25, 63, 58, 0.08);
}

.menu-copy {
  display: grid;
  gap: 4px;
}

.menu-copy strong {
  font-size: 14px;
}

.menu-copy span {
  color: var(--text-soft);
  font-size: 12px;
}

.danger-copy strong {
  color: var(--danger);
}

@media (max-width: 900px) {
  .user-shell {
    width: min(var(--max-width), calc(100vw - 20px));
  }

  .nav-inner {
    flex-wrap: wrap;
  }

  nav {
    width: 100%;
    overflow: auto;
  }

  .actions {
    width: 100%;
    justify-content: space-between;
  }

  .user-brief {
    flex: 1;
  }
}

@media (max-width: 640px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
  }

  .user-brief {
    width: 100%;
  }

  .profile-trigger,
  .solid-link {
    width: 100%;
    justify-content: center;
  }
}

.user-shell {
  padding: 28px 0 34px;
}
</style>
