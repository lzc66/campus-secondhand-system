<template>
  <div class="page-shell register-page">
    <section class="register-card glass-card">
      <SectionHeading title="提交注册申请" description="先上传学生证，再填写身份信息。审核通过后才可登录系统。" tag="Campus Verification" />
      <div class="register-banner">
        <div>
          <strong>校园实名注册</strong>
          <p>支持学生证图片上传、分步填写和申请结果追踪，注册信息会进入管理员审核队列。</p>
        </div>
        <ul>
          <li>图片限 10MB 以内</li>
          <li>邮箱和手机号需真实可用</li>
          <li>密码长度不少于 6 位</li>
        </ul>
      </div>
      <el-steps :active="step" simple>
        <el-step title="上传学生证" />
        <el-step title="填写资料" />
        <el-step title="提交完成" />
      </el-steps>
      <div v-if="step === 0" class="step-panel">
        <el-upload drag :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="handleFileChange">
          <el-icon size="42"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽或点击上传学生证照片</div>
          <template #tip><div class="upload-tip">仅支持 JPG / PNG / WEBP，建议文字区域清晰可辨。</div></template>
        </el-upload>
        <div v-if="previewUrl" class="preview-card">
          <img :src="previewUrl" alt="student-card" />
          <div class="preview-meta"><span>{{ selectedFile?.name }}</span><small>{{ fileSizeText }}</small></div>
        </div>
        <div class="upload-actions">
          <el-button @click="resetUpload">重新选择</el-button>
          <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="uploadStudentCard">上传并继续</el-button>
        </div>
      </div>
      <div v-else-if="step === 1" class="step-panel">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="register-form">
          <div class="form-grid">
            <el-form-item label="学号" prop="studentNo"><el-input v-model="form.studentNo" maxlength="20" /></el-form-item>
            <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" maxlength="20" /></el-form-item>
            <el-form-item label="性别" prop="gender"><el-select v-model="form.gender"><el-option label="男" value="male" /><el-option label="女" value="female" /></el-select></el-form-item>
            <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" maxlength="80" /></el-form-item>
            <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
            <el-form-item label="密码" prop="password"><el-input v-model="form.password" show-password /></el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" show-password /></el-form-item>
            <el-form-item label="学院" prop="collegeName"><el-input v-model="form.collegeName" maxlength="50" /></el-form-item>
            <el-form-item label="专业" prop="majorName"><el-input v-model="form.majorName" maxlength="50" /></el-form-item>
            <el-form-item label="班级" prop="className"><el-input v-model="form.className" maxlength="50" /></el-form-item>
          </div>
        </el-form>
        <div class="form-actions"><el-button @click="step = 0">返回</el-button><el-button type="primary" :loading="submitting" @click="submitRegistration">提交申请</el-button></div>
      </div>
      <div v-else class="step-panel done-panel">
        <el-result icon="success" title="申请已提交" sub-title="注册申请已进入管理员审核流程，审核结果会通过站内通知或邮件反馈。"><template #extra><el-button type="primary" @click="goResult">查看提交结果页</el-button></template></el-result>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import type { FormInstance, FormRules, UploadFile } from 'element-plus';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { UploadFilled } from '@element-plus/icons-vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
const router = useRouter();
const step = ref(0); const formRef = ref<FormInstance>(); const selectedFile = ref<File | null>(null); const uploadedFileId = ref<number | null>(null); const previewUrl = ref(''); const applicationNo = ref(''); const uploading = ref(false); const submitting = ref(false);
const form = reactive({ studentNo: '', realName: '', gender: 'female', email: '', phone: '', password: '', confirmPassword: '', collegeName: '', majorName: '', className: '' });
const rules: FormRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: ['blur','change'] }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少 6 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }, { validator: (_rule, value, callback) => { if (value !== form.password) callback(new Error('两次输入的密码不一致')); else callback(); }, trigger: 'blur' }],
  collegeName: [{ required: true, message: '请输入学院', trigger: 'blur' }],
  majorName: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }]
};
const fileSizeText = computed(() => !selectedFile.value ? '' : `${(selectedFile.value.size / 1024 / 1024).toFixed(2)} MB`);
function handleFileChange(file: UploadFile) { if (!file.raw) return; const raw = file.raw; if (!raw.type.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return; } if (raw.size > 10 * 1024 * 1024) { ElMessage.error('图片大小不能超过 10MB'); return; } selectedFile.value = raw; previewUrl.value = URL.createObjectURL(raw); }
function resetUpload() { selectedFile.value = null; previewUrl.value = ''; }
async function uploadStudentCard() { if (!selectedFile.value) return; uploading.value = true; try { const result = await publicApi.uploadStudentCard(selectedFile.value); uploadedFileId.value = result.fileId; previewUrl.value = result.fileUrl; step.value = 1; ElMessage.success('学生证上传成功'); } finally { uploading.value = false; } }
async function submitRegistration() { if (!uploadedFileId.value) { ElMessage.warning('请先上传学生证'); return; } const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { const result = await publicApi.submitRegistration({ studentNo: form.studentNo, realName: form.realName, gender: form.gender, email: form.email, phone: form.phone, password: form.password, collegeName: form.collegeName, majorName: form.majorName, className: form.className, studentCardFileId: uploadedFileId.value }); applicationNo.value = result.applicationNo || ''; step.value = 2; } finally { submitting.value = false; } }
function goResult() { router.push({ path: '/register-result', query: { applicationNo: applicationNo.value } }); }
</script>

<style scoped>
.register-page { padding: 28px 0 42px; }
.register-card { padding: 28px; }
.register-banner { display: grid; grid-template-columns: 1.4fr 1fr; gap: 18px; margin-bottom: 22px; padding: 18px 20px; border-radius: 22px; background: linear-gradient(135deg, rgba(25, 63, 58, 0.08), rgba(220, 127, 57, 0.1)); }
.register-banner strong { display: block; margin-bottom: 8px; font-size: 20px; }
.register-banner p { margin: 0; color: var(--text-soft); }
.register-banner ul { margin: 0; padding-left: 18px; color: var(--text-soft); }
.step-panel { margin-top: 24px; display: grid; gap: 18px; }
.preview-card { max-width: 360px; overflow: hidden; border-radius: 18px; background: rgba(25, 63, 58, 0.05); }
.preview-card img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; }
.preview-meta { display: flex; justify-content: space-between; gap: 12px; padding: 12px 14px; color: var(--text-soft); }
.upload-tip { margin-top: 8px; color: var(--text-soft); }
.upload-actions { display: flex; justify-content: flex-end; gap: 12px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 18px; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; }
.done-panel { min-height: 300px; }
@media (max-width: 900px) { .register-banner, .form-grid { grid-template-columns: 1fr; } }
</style>