<template>
  <div class="login-page">
    <section class="login-card glass-card">
      <div class="hero-orbit hero-orbit-left"></div>
      <div class="hero-orbit hero-orbit-right"></div>
      <AppLogo />
      <div class="copy">
        <div class="app-chip">Student Access</div>
        <h1>登录后，开始你的校园交易闭环。</h1>
        <p>请输入学号、密码和验证码。验证码每次校验后都会失效，点击图片可立即刷新。</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="请输入登录密码" />
        </el-form-item>
        <div class="captcha-row">
          <el-form-item label="验证码" prop="captcha">
            <el-input v-model="form.captcha" maxlength="4" placeholder="请输入图中字符" @keyup.enter="handleLogin" />
          </el-form-item>
          <div class="captcha-panel">
            <button class="captcha-box" type="button" :disabled="captchaLoading" @click="fetchCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="登录验证码" />
              <span v-else>{{ captchaLoading ? '加载中...' : '点击加载验证码' }}</span>
            </button>
            <button class="captcha-link" type="button" :disabled="captchaLoading" @click="fetchCaptcha">
              换一张
            </button>
          </div>
        </div>
        <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="tips-bar">
        <span>验证码有效期 3 分钟</span>
        <span>输入错误后请重新获取</span>
      </div>
      <div class="links">
        <RouterLink to="/register">没有账号，去申请注册</RouterLink>
        <RouterLink to="/admin/login">进入后台登录</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { RouterLink, useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { userApi } from '@/api/user';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const captchaLoading = ref(false);
const captchaImage = ref('');

const form = reactive({
  studentNo: '',
  password: '',
  captcha: '',
  captchaKey: ''
});

const rules: FormRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码为 4 位字符', trigger: 'blur' }
  ]
};

async function fetchCaptcha() {
  captchaLoading.value = true;
  try {
    const result = await userApi.getLoginCaptcha();
    form.captcha = '';
    form.captchaKey = result.captchaKey;
    captchaImage.value = result.imageData;
  } finally {
    captchaLoading.value = false;
  }
}

async function handleLogin() {
  await formRef.value?.validate();
  if (!form.captchaKey) {
    await fetchCaptcha();
    ElMessage.warning('验证码已刷新，请重新输入');
    return;
  }
  submitting.value = true;
  try {
    const result = await userApi.login(form);
    authStore.setUserAuth(result.token, result.userProfile || null);
    ElMessage.success('登录成功');
    router.push('/user/profile');
  } catch (error) {
    await fetchCaptcha();
    throw error;
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void fetchCaptcha();
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(213, 117, 42, 0.22), transparent 32%),
    radial-gradient(circle at bottom right, rgba(15, 58, 46, 0.18), transparent 36%),
    linear-gradient(135deg, rgba(244, 239, 229, 0.98), rgba(233, 238, 230, 0.96));
}

.login-card {
  position: relative;
  overflow: hidden;
  width: min(580px, 100%);
  padding: 34px;
}

.hero-orbit {
  position: absolute;
  width: 180px;
  height: 180px;
  border-radius: 999px;
  filter: blur(16px);
  opacity: 0.35;
  pointer-events: none;
}

.hero-orbit-left {
  top: -36px;
  left: -60px;
  background: rgba(16, 59, 46, 0.18);
}

.hero-orbit-right {
  right: -44px;
  bottom: -60px;
  background: rgba(213, 117, 42, 0.18);
}

.copy {
  position: relative;
  margin: 24px 0;
}

h1 {
  margin: 12px 0;
  font-size: 48px;
  line-height: 0.95;
  font-family: var(--font-display);
}

p {
  color: var(--text-soft);
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 176px;
  gap: 14px;
  align-items: start;
}

.captcha-panel {
  display: grid;
  gap: 8px;
}

.captcha-box {
  height: 72px;
  border: 1px solid rgba(16, 59, 46, 0.14);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.captcha-box:hover {
  transform: translateY(-1px);
  border-color: rgba(213, 117, 42, 0.55);
  box-shadow: 0 14px 30px rgba(16, 59, 46, 0.08);
}

.captcha-box:disabled {
  cursor: progress;
  opacity: 0.78;
}

.captcha-box img {
  width: 132px;
  height: 44px;
  display: block;
}

.captcha-link {
  justify-self: end;
  border: none;
  background: transparent;
  color: var(--brand-accent, #d5752a);
  font-weight: 600;
  cursor: pointer;
}

.tips-bar {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(16, 59, 46, 0.06);
  color: var(--text-soft);
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.links {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}

@media (max-width: 640px) {
  .captcha-row {
    grid-template-columns: 1fr;
  }

  .captcha-panel {
    grid-template-columns: 1fr auto;
    align-items: center;
  }

  .tips-bar,
  .links {
    flex-direction: column;
  }
}
</style>