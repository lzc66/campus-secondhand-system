<template>
  <div class="page-stack">
    <section class="glass-card panel">
      <SectionHeading title="个人资料" description="维护你的实名信息、联系方式和宿舍地址。" tag="Profile" />
      <div class="profile-top">
        <div class="avatar-card">
          <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="avatar" />
          <div v-else class="avatar-fallback">{{ profile.realName?.slice(0, 1) || 'U' }}</div>
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="handleAvatarChange"><el-button>上传头像</el-button></el-upload>
        </div>
        <el-form :model="profile" label-width="90px" class="profile-form">
          <el-form-item label="学号"><el-input v-model="profile.studentNo" disabled /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="profile.realName" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
          <el-form-item label="手机"><el-input v-model="profile.phone" /></el-form-item>
          <el-form-item label="QQ"><el-input v-model="profile.qqNo" /></el-form-item>
          <el-form-item label="微信"><el-input v-model="profile.wechatNo" /></el-form-item>
          <el-form-item label="学院"><el-input v-model="profile.collegeName" /></el-form-item>
          <el-form-item label="专业"><el-input v-model="profile.majorName" /></el-form-item>
          <el-form-item label="班级"><el-input v-model="profile.className" /></el-form-item>
          <el-form-item label="宿舍"><el-input v-model="profile.dormitoryAddress" /></el-form-item>
          <el-form-item><el-button type="primary" @click="saveProfile">保存资料</el-button></el-form-item>
        </el-form>
      </div>
    </section>
    <section class="glass-card panel">
      <SectionHeading title="修改密码" description="修改后请使用新密码重新登录。" tag="Security" />
      <el-form :model="passwordForm" label-width="90px" class="password-form">
        <el-form-item label="旧密码"><el-input v-model="passwordForm.oldPassword" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="passwordForm.confirmPassword" show-password /></el-form-item>
        <el-form-item><el-button type="primary" @click="changePassword">更新密码</el-button></el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const profile = reactive<any>({});
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

onMounted(async () => {
  Object.assign(profile, await userApi.getProfile());
});

async function saveProfile() {
  await userApi.updateProfile(profile);
  authStore.userProfile = { ...authStore.userProfile, ...profile } as any;
  ElMessage.success('资料已更新');
}

async function changePassword() {
  await userApi.changePassword(passwordForm);
  ElMessage.success('密码已更新');
  passwordForm.oldPassword = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = '';
}

async function handleAvatarChange(file: any) {
  const result = await userApi.uploadAvatar(file.raw);
  profile.avatarUrl = result.fileUrl || result.avatarUrl;
  authStore.userProfile = { ...authStore.userProfile, avatarUrl: profile.avatarUrl } as any;
  ElMessage.success('头像已更新');
}
</script>

<style scoped>
.page-stack { display: grid; gap: 22px; }
.panel { padding: 24px; }
.profile-top { display: grid; grid-template-columns: 220px 1fr; gap: 24px; }
.avatar-card { display: grid; gap: 14px; justify-items: start; }
.avatar-card img, .avatar-fallback { width: 160px; height: 160px; border-radius: 28px; object-fit: cover; }
.avatar-fallback { display: grid; place-items: center; font-size: 56px; color: #fff; background: linear-gradient(135deg, var(--brand), var(--accent)); font-family: var(--font-display); }
@media (max-width: 900px) { .profile-top { grid-template-columns: 1fr; } }
</style>