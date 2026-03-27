<template>
  <div class="page-shell detail-page" v-if="detail">
    <section class="glass-card detail-card">
      <div class="app-chip">{{ detail.categoryName || 'Wanted' }}</div>
      <h1>{{ detail.title }}</h1>
      <p class="desc">{{ detail.description || '暂无描述' }}</p>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="期望价格区间">{{ formatPrice(detail.expectedPriceMin) }} - {{ formatPrice(detail.expectedPriceMax) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDateTime(detail.expiresAt) }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ detail.requester?.realName || '同学' }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ detail.requester?.collegeName || '--' }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { publicApi } from '@/api/public';
import type { WantedPost } from '@/types/api';
import { formatDateTime, formatPrice } from '@/utils/format';
const route = useRoute();
const detail = ref<WantedPost | null>(null);
onMounted(async () => { detail.value = await publicApi.getWantedPostDetail(Number(route.params.id)); });
</script>

<style scoped>
.detail-page { padding: 28px 0 40px; }
.detail-card { padding: 28px; }
h1 { margin: 14px 0; font-family: var(--font-display); font-size: 44px; }
.desc { color: var(--text-soft); margin-bottom: 22px; }
</style>