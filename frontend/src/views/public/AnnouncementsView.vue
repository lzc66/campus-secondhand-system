<template>
  <div class="page-shell page-block">
    <SectionHeading title="系统公告" description="管理员发布的重要消息、交易提醒和校园平台通知都会集中展示在这里。" tag="Notices" />

    <div v-if="isLoading" class="notice-list skeleton-list">
      <article v-for="index in 4" :key="index" class="notice-card glass-card">
        <el-skeleton animated :rows="4" />
      </article>
    </div>

    <div v-else-if="records.length" class="notice-list">
      <article v-for="item in records" :key="item.announcementId" class="notice-card glass-card fade-up">
        <div class="notice-topline">
          <div>
            <div class="chips">
              <span v-if="item.pinned" class="pin-chip">置顶</span>
              <span class="time-chip">{{ formatDateTime(item.publishTime || item.createdAt) }}</span>
            </div>
            <h3>{{ item.title }}</h3>
          </div>
          <el-button text type="primary" @click="openPreview(item)">查看详情</el-button>
        </div>
        <p>{{ previewText(item.content) }}</p>
      </article>
    </div>

    <EmptyState v-else title="暂无公告" description="管理员还没有发布新的系统公告，稍后再来查看。" />
  </div>

  <el-dialog v-model="previewVisible" width="760px" :title="currentAnnouncement?.title || '公告详情'">
    <div v-if="currentAnnouncement" class="dialog-content">
      <div class="dialog-meta">
        <el-tag v-if="currentAnnouncement.pinned" round type="danger">置顶公告</el-tag>
        <span>{{ formatDateTime(currentAnnouncement.publishTime || currentAnnouncement.createdAt) }}</span>
      </div>
      <p class="content-text">{{ currentAnnouncement.content }}</p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import type { Announcement } from '@/types/api';
import { formatDateTime } from '@/utils/format';

const records = ref<Announcement[]>([]);
const isLoading = ref(false);
const previewVisible = ref(false);
const currentAnnouncement = ref<Announcement | null>(null);

onMounted(fetchAnnouncements);

async function fetchAnnouncements() {
  isLoading.value = true;
  try {
    const data = await publicApi.getAnnouncements({ page: 1, size: 20 });
    records.value = data.records || [];
  } finally {
    isLoading.value = false;
  }
}

function openPreview(item: Announcement) {
  currentAnnouncement.value = item;
  previewVisible.value = true;
}

function previewText(content?: string) {
  if (!content) return '暂无内容';
  return content.length > 110 ? `${content.slice(0, 110)}...` : content;
}
</script>

<style scoped>
.page-block {
  padding: 26px 0 40px;
}
.notice-list {
  display: grid;
  gap: 16px;
}
.notice-card {
  padding: 22px;
}
.notice-topline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}
.chips {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.pin-chip,
.time-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
}
.pin-chip {
  background: rgba(220, 127, 57, 0.14);
  color: var(--accent);
}
.time-chip {
  background: rgba(25, 63, 58, 0.08);
  color: var(--text-soft);
}
h3 {
  margin: 0;
  font-size: 26px;
}
p {
  margin: 14px 0 0;
  color: var(--text-soft);
  line-height: 1.9;
}
.dialog-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
  color: var(--text-soft);
}
.content-text {
  white-space: pre-wrap;
  line-height: 1.9;
}
@media (max-width: 720px) {
  .notice-topline,
  .dialog-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>