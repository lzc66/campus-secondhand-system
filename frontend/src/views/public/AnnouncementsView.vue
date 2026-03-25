<template>
  <div class="page-shell page-block">
    <SectionHeading title="系统公告" description="管理员发布的重要消息与校园交易通知。" tag="Notices" />
    <div v-if="records.length" class="notice-list">
      <article v-for="item in records" :key="item.announcementId" class="notice-card glass-card">
        <div class="notice-topline"><h3>{{ item.title }}</h3><span>{{ formatDateTime(item.publishTime || item.createdAt) }}</span></div>
        <p>{{ item.content }}</p>
      </article>
    </div>
    <EmptyState v-else title="暂无公告" description="等待管理员发布新的系统公告。" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import type { Announcement } from '@/types/api';
import { formatDateTime } from '@/utils/format';

const records = ref<Announcement[]>([]);
onMounted(async () => { const data = await publicApi.getAnnouncements({ page: 1, size: 20 }); records.value = data.records || []; });
</script>

<style scoped>
.page-block { padding: 26px 0 40px; }
.notice-list { display: grid; gap: 16px; }
.notice-card { padding: 22px; }
.notice-topline { display: flex; justify-content: space-between; gap: 16px; }
h3 { margin: 0 0 12px; font-size: 24px; }
p { margin: 0; color: var(--text-soft); line-height: 1.9; }
span { color: var(--text-soft); }
</style>