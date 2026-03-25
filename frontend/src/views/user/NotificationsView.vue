<template>
  <section class="glass-card panel">
    <SectionHeading title="通知中心" description="查看站内通知，并批量标记已读。" tag="Inbox"><el-button @click="readAll">全部已读</el-button></SectionHeading>
    <el-radio-group v-model="readStatus" @change="fetchNotifications"><el-radio-button label="all">全部</el-radio-button><el-radio-button label="unread">未读</el-radio-button></el-radio-group>
    <div class="notification-list">
      <article v-for="item in records" :key="item.notificationId" class="notice-row glass-card">
        <div><strong>{{ item.title }}</strong><p>{{ item.content }}</p></div>
        <div class="notice-actions"><span>{{ formatDateTime(item.createdAt) }}</span><el-button size="small" @click="readOne(item.notificationId)">已读</el-button></div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { formatDateTime } from '@/utils/format';
const readStatus = ref('all');
const records = ref<any[]>([]);
onMounted(fetchNotifications);
async function fetchNotifications() { const data = await userApi.getNotifications({ page: 1, size: 50, readStatus: readStatus.value === 'all' ? undefined : readStatus.value }); records.value = data.records || []; }
async function readOne(notificationId: number) { await userApi.markNotificationRead(notificationId); ElMessage.success('已标记'); fetchNotifications(); }
async function readAll() { await userApi.markAllNotificationsRead(); ElMessage.success('全部已读'); fetchNotifications(); }
</script>

<style scoped>
.panel { padding: 24px; }
.notification-list { display: grid; gap: 12px; margin-top: 18px; }
.notice-row { padding: 18px; display: flex; justify-content: space-between; gap: 18px; }
.notice-row p { color: var(--text-soft); margin: 10px 0 0; }
.notice-actions { display: grid; gap: 10px; justify-items: end; color: var(--text-soft); }
</style>