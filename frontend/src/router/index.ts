import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue') },
        { path: 'items', name: 'items', component: () => import('@/views/public/ItemsView.vue') },
        { path: 'items/:id', name: 'item-detail', component: () => import('@/views/public/ItemDetailView.vue') },
        { path: 'wanted-posts', name: 'wanted-posts', component: () => import('@/views/public/WantedPostsView.vue') },
        { path: 'wanted-posts/:id', name: 'wanted-post-detail', component: () => import('@/views/public/WantedPostDetailView.vue') },
        { path: 'announcements', name: 'announcements', component: () => import('@/views/public/AnnouncementsView.vue') },
        { path: 'register', name: 'register', component: () => import('@/views/public/RegisterView.vue') },
        { path: 'register-result', name: 'register-result', component: () => import('@/views/public/RegisterResultView.vue') },
        { path: 'user/profile', name: 'user-profile', component: () => import('@/views/user/ProfileView.vue'), meta: { requiresUser: true } },
        { path: 'user/publish', name: 'publish-item', component: () => import('@/views/user/PublishItemView.vue'), meta: { requiresUser: true } },
        { path: 'user/my-items', name: 'my-items', component: () => import('@/views/user/MyItemsView.vue'), meta: { requiresUser: true } },
        { path: 'user/orders', name: 'orders', component: () => import('@/views/user/OrdersView.vue'), meta: { requiresUser: true } },
        { path: 'user/notifications', name: 'notifications', component: () => import('@/views/user/NotificationsView.vue'), meta: { requiresUser: true } },
        { path: 'user/recommendations', name: 'recommendations', component: () => import('@/views/user/RecommendationsView.vue'), meta: { requiresUser: true } },
        { path: 'user/comments', name: 'received-comments', component: () => import('@/views/user/ReceivedCommentsView.vue'), meta: { requiresUser: true } },
        { path: 'user/wanted-posts', name: 'my-wanted-posts', component: () => import('@/views/user/MyWantedPostsView.vue'), meta: { requiresUser: true } }
      ]
    },
    {
      path: '/user',
      redirect: '/user/profile'
    },
    {
      path: '/login',
      name: 'user-login',
      component: () => import('@/views/user/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/LoginView.vue'),
      meta: { guestOnlyAdmin: true }
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'registrations', name: 'admin-registrations', component: () => import('@/views/admin/RegistrationsView.vue') },
        { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UsersView.vue') },
        { path: 'items', name: 'admin-items', component: () => import('@/views/admin/ItemsView.vue') },
        { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/OrdersView.vue') },
        { path: 'announcements', name: 'admin-announcements', component: () => import('@/views/admin/AnnouncementsView.vue') },
        { path: 'mail-settings', name: 'admin-mail-settings', component: () => import('@/views/admin/MailSettingsView.vue') },
        { path: 'reports', name: 'admin-reports', component: () => import('@/views/admin/ReportsView.vue') }
      ]
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresUser && !authStore.isUserLoggedIn) {
    return '/login';
  }
  if (to.meta.requiresAdmin && !authStore.isAdminLoggedIn) {
    return '/admin/login';
  }
  if (to.meta.guestOnly && authStore.isUserLoggedIn) {
    return '/';
  }
  if (to.meta.guestOnlyAdmin && authStore.isAdminLoggedIn) {
    return '/admin/dashboard';
  }
  return true;
});

export default router;
