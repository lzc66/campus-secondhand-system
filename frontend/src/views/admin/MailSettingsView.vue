<template>
  <section class="mail-page">
    <div class="hero-card glass-card fade-up">
      <div>
        <span class="hero-tag">Mail Control</span>
        <h2>SMTP 邮件配置</h2>
        <p>在后台统一配置通知邮件的发信参数。保存后，注册审核邮件和后续通知邮件都会读取这里的实时配置。</p>
      </div>
      <div class="hero-status">
        <el-tag :type="settings.enabled ? 'success' : 'info'">{{ settings.enabled ? 'SMTP 已启用' : 'SMTP 未启用' }}</el-tag>
        <el-tag :type="settings.smtpReady ? 'success' : 'warning'">{{ settings.smtpReady ? '配置完整' : '配置未完成' }}</el-tag>
        <el-tag :type="settings.passwordConfigured ? 'success' : 'warning'">{{ settings.passwordConfigured ? '密码已保存' : '密码未保存' }}</el-tag>
      </div>
    </div>

    <div class="mail-grid">
      <section class="glass-card panel">
        <SectionHeading title="发送配置" description="保存 SMTP 主机、端口、账号和安全策略。密码留空时会继续沿用后台已保存的值。" tag="SMTP" />
        <el-form ref="settingsFormRef" :model="form" :rules="rules" label-position="top" class="config-form">
          <div class="switch-row">
            <div>
              <strong>启用邮件发送</strong>
              <p>关闭后，系统仍写通知记录，但不会实际发信。</p>
            </div>
            <el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="关闭" />
          </div>

          <div class="form-grid">
            <el-form-item label="SMTP 主机" prop="host">
              <el-input v-model="form.host" placeholder="例如：smtp.qq.com" />
            </el-form-item>
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="form.port" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="SMTP 用户名" prop="username">
              <el-input v-model="form.username" placeholder="通常是邮箱账号" />
            </el-form-item>
            <el-form-item label="发件人地址" prop="fromAddress">
              <el-input v-model="form.fromAddress" placeholder="例如：noreply@example.com" />
            </el-form-item>
            <el-form-item label="SMTP 密码 / 授权码" class="full-width">
              <el-input v-model="form.password" show-password placeholder="留空则保留当前已保存密码" />
              <div class="field-tip">{{ settings.passwordConfigured ? '后台已保存密码，本次留空不会覆盖。' : '当前还没有保存过密码。' }}</div>
            </el-form-item>
          </div>

          <div class="policy-grid">
            <label class="policy-card">
              <div>
                <strong>SMTP 认证</strong>
                <p>大多数邮箱服务都需要开启。</p>
              </div>
              <el-switch v-model="form.authEnabled" />
            </label>
            <label class="policy-card">
              <div>
                <strong>STARTTLS</strong>
                <p>建议优先开启，适合 587 端口。</p>
              </div>
              <el-switch v-model="form.starttlsEnabled" />
            </label>
            <label class="policy-card">
              <div>
                <strong>SSL</strong>
                <p>常用于 465 端口的直连 SSL。</p>
              </div>
              <el-switch v-model="form.sslEnabled" />
            </label>
          </div>

          <div class="actions-row">
            <el-button @click="loadSettings">重新读取</el-button>
            <el-button type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
          </div>
        </el-form>
      </section>

      <section class="glass-card panel side-panel">
        <SectionHeading title="状态与测试" description="保存后可以立刻发送测试邮件，验证账号、授权码和网络连通性。" tag="Diagnostics" />
        <div class="state-list">
          <div class="state-item">
            <span>最近更新时间</span>
            <strong>{{ settings.updatedAt || '尚未保存' }}</strong>
          </div>
          <div class="state-item">
            <span>最近更新人 ID</span>
            <strong>{{ settings.updatedByAdminId || '--' }}</strong>
          </div>
          <div class="state-item">
            <span>当前发件地址</span>
            <strong>{{ settings.fromAddress || '--' }}</strong>
          </div>
        </div>

        <el-divider />

        <el-form ref="testFormRef" :model="testForm" :rules="testRules" label-position="top">
          <el-form-item label="测试收件邮箱" prop="toEmail">
            <el-input v-model="testForm.toEmail" placeholder="例如：yourname@example.com" />
          </el-form-item>
          <el-form-item label="邮件主题" prop="subject">
            <el-input v-model="testForm.subject" placeholder="例如：校园二手系统 SMTP 测试" />
          </el-form-item>
          <el-form-item label="邮件内容" prop="content">
            <el-input v-model="testForm.content" type="textarea" :rows="6" placeholder="建议写上当前时间和配置说明，便于确认是否成功收到。" />
          </el-form-item>
          <el-button type="primary" class="full-btn" :loading="testing" :disabled="!settings.smtpReady" @click="sendTestMail">
            发送测试邮件
          </el-button>
          <p class="test-tip">
            {{ settings.smtpReady ? '当前配置已达到可测试状态。' : '请先补全并保存主机、端口、发件地址，以及认证所需账号密码。' }}
          </p>
        </el-form>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';
import type { SmtpSettings } from '@/types/api';

const settingsFormRef = ref<FormInstance>();
const testFormRef = ref<FormInstance>();
const saving = ref(false);
const testing = ref(false);
const settings = reactive<SmtpSettings>({
  enabled: false,
  host: '',
  port: 587,
  username: '',
  fromAddress: '',
  authEnabled: true,
  starttlsEnabled: true,
  sslEnabled: false,
  passwordConfigured: false,
  smtpReady: false,
  updatedAt: '',
  updatedByAdminId: undefined
});

const form = reactive({
  enabled: false,
  host: '',
  port: 587,
  username: '',
  fromAddress: '',
  password: '',
  authEnabled: true,
  starttlsEnabled: true,
  sslEnabled: false
});

const testForm = reactive({
  toEmail: '',
  subject: '校园二手系统 SMTP 测试邮件',
  content: '这是一封来自校园二手系统后台的测试邮件，用于验证 SMTP 配置是否可用。'
});

const rules: FormRules = {
  host: [{ validator: validateHost, trigger: 'blur' }],
  port: [{ validator: validatePort, trigger: 'change' }],
  username: [{ validator: validateUsername, trigger: 'blur' }],
  fromAddress: [{ validator: validateFromAddress, trigger: 'blur' }]
};

const testRules: FormRules = {
  toEmail: [
    { required: true, message: '请输入测试收件邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  subject: [{ required: true, message: '请输入邮件主题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入邮件内容', trigger: 'blur' }]
};

onMounted(() => {
  void loadSettings();
});

async function loadSettings() {
  const data = await adminApi.getSmtpSettings();
  Object.assign(settings, data);
  Object.assign(form, {
    enabled: data.enabled,
    host: data.host || '',
    port: data.port || 587,
    username: data.username || '',
    fromAddress: data.fromAddress || '',
    password: '',
    authEnabled: data.authEnabled,
    starttlsEnabled: data.starttlsEnabled,
    sslEnabled: data.sslEnabled
  });
}

async function saveSettings() {
  const valid = await settingsFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const result = await adminApi.updateSmtpSettings({ ...form });
    Object.assign(settings, result);
    form.password = '';
    ElMessage.success('SMTP 配置已保存');
  } finally {
    saving.value = false;
  }
}

async function sendTestMail() {
  const valid = await testFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  testing.value = true;
  try {
    const result = await adminApi.sendSmtpTestEmail({ ...testForm });
    ElMessage.success(`测试邮件已发送到 ${result.toEmail}`);
    await loadSettings();
  } finally {
    testing.value = false;
  }
}

function validateHost(_: unknown, value: string, callback: (error?: Error) => void) {
  if (!form.enabled) {
    callback();
    return;
  }
  if (!value?.trim()) {
    callback(new Error('启用 SMTP 时必须填写主机地址'));
    return;
  }
  callback();
}

function validatePort(_: unknown, value: number, callback: (error?: Error) => void) {
  if (!form.enabled) {
    callback();
    return;
  }
  if (!value || value <= 0) {
    callback(new Error('请输入正确的端口号'));
    return;
  }
  callback();
}

function validateUsername(_: unknown, value: string, callback: (error?: Error) => void) {
  if (!form.enabled || !form.authEnabled) {
    callback();
    return;
  }
  if (!value?.trim()) {
    callback(new Error('开启 SMTP 认证后必须填写用户名'));
    return;
  }
  callback();
}

function validateFromAddress(_: unknown, value: string, callback: (error?: Error) => void) {
  if (!form.enabled) {
    callback();
    return;
  }
  if (!value?.trim()) {
    callback(new Error('启用 SMTP 时必须填写发件人地址'));
    return;
  }
  callback();
}
</script>

<style scoped>
.mail-page {
  display: grid;
  gap: 18px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(242, 248, 245, 0.98), rgba(235, 242, 250, 0.96));
}

.hero-tag,
.state-item span {
  color: var(--text-soft);
}

.hero-card h2 {
  margin: 12px 0;
  font-size: 36px;
  font-family: var(--font-display);
}

.hero-card p {
  margin: 0;
  max-width: 720px;
  color: var(--text-soft);
  line-height: 1.8;
}

.hero-status {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.mail-grid {
  display: grid;
  grid-template-columns: 1.45fr 0.95fr;
  gap: 18px;
}

.panel {
  padding: 22px;
}

.config-form {
  margin-top: 18px;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px;
  border-radius: 18px;
  background: rgba(20, 59, 45, 0.05);
}

.switch-row p,
.field-tip,
.test-tip,
.policy-card p {
  margin: 6px 0 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
  margin-top: 18px;
}

.full-width {
  grid-column: 1 / -1;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 6px 0 18px;
}

.policy-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(20, 59, 45, 0.08);
}

.actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.side-panel {
  align-self: start;
}

.state-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.state-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(23, 48, 45, 0.05);
}

.state-item strong {
  display: block;
  margin-top: 8px;
  font-size: 16px;
}

.full-btn {
  width: 100%;
}

@media (max-width: 1080px) {
  .mail-grid {
    grid-template-columns: 1fr;
  }

  .policy-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-card,
  .switch-row,
  .form-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .form-grid {
    display: grid;
  }
}
</style>