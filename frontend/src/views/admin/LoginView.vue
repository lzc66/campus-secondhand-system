<template>
  <div class="login-page">
    <section class="login-card glass-card">
      <AppLogo to="/admin/dashboard" />
      <div class="copy"><div class="app-chip">Admin Access</div><h1>管理端登录</h1><p>适用于系统管理员、审核员和运营人员。</p></div>
      <el-form :model="form" label-position="top"><el-form-item label="管理员账号"><el-input v-model="form.adminNo" /></el-form-item><el-form-item label="密码"><el-input v-model="form.password" show-password /></el-form-item><el-button type="primary" class="submit-btn" @click="handleLogin">登录后台</el-button></el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { adminApi } from '@/api/admin';
import { useAuthStore } from '@/stores/auth';
const router = useRouter(); const authStore = useAuthStore(); const form = reactive({ adminNo: 'admin1001', password: '123456' });
async function handleLogin() { const result = await adminApi.login(form); authStore.setAdminAuth(result.token, result.adminProfile || null); ElMessage.success('后台登录成功'); router.push('/admin/dashboard'); }
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
.login-card { width: min(520px, 100%); padding: 34px; }
.copy { margin: 24px 0; }
h1 { margin: 12px 0; font-size: 42px; font-family: var(--font-display); }
p { color: var(--text-soft); }
.submit-btn { width: 100%; }
</style>