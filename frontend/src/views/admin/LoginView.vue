<template>
  <div class="login-page">
    <section class="login-card glass-card">
      <div class="hero-grid"></div>
      <AppLogo to="/admin/login" />
      <div class="copy">
        <div class="app-chip">Admin Access</div>
        <h1>后台入口，先过验证码再进控制台。</h1>
        <p>适用于系统管理员、审核员和运营人员。验证码实时生成，登录失败后会自动刷新。</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="管理员账号" prop="adminNo">
          <el-input v-model="form.adminNo" placeholder="请输入管理员账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="请输入管理员密码" />
        </el-form-item>
        <div class="captcha-row">
          <el-form-item label="验证码" prop="captcha">
            <el-input v-model="form.captcha" maxlength="4" placeholder="请输入图中字符" @keyup.enter="handleLogin" />
          </el-form-item>
          <div class="captcha-panel">
            <button class="captcha-box" type="button" :disabled="captchaLoading" @click="fetchCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="管理员登录验证码" />
              <span v-else>{{ captchaLoading ? '加载中...' : '点击加载验证码' }}</span>
            </button>
            <button class="captcha-link" type="button" :disabled="captchaLoading" @click="fetchCaptcha">换一张</button>
          </div>
        </div>
        <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleLogin">登录后台</el-button>
      </el-form>
      <div class="tips-bar">
        <span>默认演示账号：admin1001</span>
        <span>验证码有效期 3 分钟</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useRouter } from 'vue-router';
import AppLogo from '@/components/common/AppLogo.vue';
import { adminApi } from '@/api/admin';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const captchaLoading = ref(false);
const captchaImage = ref('');

const form = reactive({
  adminNo: 'admin1001',
  password: '123456',
  captcha: '',
  captchaKey: ''
});

const rules: FormRules = {
  adminNo: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码为 4 位字符', trigger: 'blur' }
  ]
};

async function fetchCaptcha() {
  captchaLoading.value = true;
  try {
    const result = await adminApi.getLoginCaptcha();
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
    const result = await adminApi.login(form);
    authStore.setAdminAuth(result.token, result.adminProfile || null);
    ElMessage.success('后台登录成功');
    router.push('/admin/dashboard');
  } catch {
    await fetchCaptcha();
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
    radial-gradient(circle at top left, rgba(31, 58, 95, 0.22), transparent 34%),
    radial-gradient(circle at bottom right, rgba(197, 93, 31, 0.16), transparent 38%),
    linear-gradient(135deg, rgba(235, 241, 249, 0.98), rgba(228, 236, 245, 0.96));
}

.login-card {
  position: relative;
  overflow: hidden;
  width: min(560px, 100%);
  padding: 34px;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(31, 58, 95, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(31, 58, 95, 0.05) 1px, transparent 1px);
  background-size: 26px 26px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.75), transparent 78%);
  pointer-events: none;
}

.copy {
  position: relative;
  margin: 24px 0;
}

h1 {
  margin: 12px 0;
  font-size: 42px;
  line-height: 0.98;
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
  border: 1px solid rgba(31, 58, 95, 0.16);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.captcha-box:hover {
  transform: translateY(-1px);
  border-color: rgba(31, 58, 95, 0.45);
  box-shadow: 0 14px 30px rgba(31, 58, 95, 0.08);
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
  color: #1f3a5f;
  font-weight: 700;
  cursor: pointer;
}

.submit-btn {
  width: 100%;
}

.tips-bar {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(31, 58, 95, 0.07);
  color: var(--text-soft);
  font-size: 13px;
}

@media (max-width: 640px) {
  .captcha-row {
    grid-template-columns: 1fr;
  }

  .captcha-panel {
    grid-template-columns: 1fr auto;
    align-items: center;
  }

  .tips-bar {
    flex-direction: column;
  }
}
</style>