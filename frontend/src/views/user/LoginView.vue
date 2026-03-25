<template>
  <div class="login-page">
    <section class="login-card glass-card">
      <AppLogo />
      <div class="copy"><div class="app-chip">Student Access</div><h1>登录后，开始你的校园交易闭环。</h1><p>学号、密码、验证码输入框全部保留；当前验证码仅做界面占位，后端暂未启用真实校验。</p></div>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" show-password /></el-form-item>
        <div class="captcha-row"><el-form-item label="验证码"><el-input v-model="form.captcha" placeholder="当前为占位输入" /></el-form-item><el-form-item label="验证码键"><el-input v-model="form.captchaKey" placeholder="预留字段" /></el-form-item></div>
        <el-button type="primary" class="submit-btn" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="links"><RouterLink to="/register">没有账号，去申请注册</RouterLink><RouterLink to="/admin/login">进入后台登录</RouterLink></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { RouterLink, useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { userApi } from '@/api/user';
import { useAuthStore } from '@/stores/auth';
const router = useRouter();
const authStore = useAuthStore();
const form = reactive({ studentNo: '', password: '', captcha: '', captchaKey: '' });
async function handleLogin() { const result = await userApi.login(form); authStore.setUserAuth(result.token, result.userProfile || null); ElMessage.success('登录成功'); router.push('/user/profile'); }
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
.login-card { width: min(560px, 100%); padding: 34px; }
.copy { margin: 24px 0; }
h1 { margin: 12px 0; font-size: 48px; line-height: 0.95; font-family: var(--font-display); }
p { color: var(--text-soft); }
.captcha-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.submit-btn { width: 100%; margin-top: 8px; }
.links { margin-top: 18px; display: flex; justify-content: space-between; gap: 12px; font-size: 14px; }
@media (max-width: 640px) { .captcha-row { grid-template-columns: 1fr; } .links { flex-direction: column; } }
</style>