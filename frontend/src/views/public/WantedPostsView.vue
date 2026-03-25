<template>
  <div class="page-shell page-block">
    <SectionHeading title="求购广场" description="看看同学们正在寻找哪些物品，也可以登录后发布求购。" tag="Wanted Market"><RouterLink to="/user/wanted-posts">我的求购</RouterLink></SectionHeading>
    <section class="glass-card filters">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词"><el-input v-model="filters.keyword" /></el-form-item>
        <el-form-item label="排序"><el-select v-model="filters.sortBy" style="width: 140px"><el-option label="最新发布" value="latest" /><el-option label="最热浏览" value="popular" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchPosts">筛选</el-button></el-form-item>
      </el-form>
    </section>
    <div v-if="records.length" class="wanted-grid">
      <RouterLink v-for="post in records" :key="post.wantedPostId" :to="`/wanted-posts/${post.wantedPostId}`" class="wanted-card glass-card">
        <span class="app-chip">{{ post.categoryName || 'Wanted' }}</span>
        <h3>{{ post.title }}</h3>
        <p>{{ post.description || '暂无描述' }}</p>
        <strong>{{ formatPrice(post.expectedPriceMin) }} - {{ formatPrice(post.expectedPriceMax) }}</strong>
      </RouterLink>
    </div>
    <EmptyState v-else title="暂无求购信息" description="当前还没有公开求购需求。" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import type { WantedPost } from '@/types/api';
import { formatPrice } from '@/utils/format';

const records = ref<WantedPost[]>([]);
const filters = reactive({ keyword: '', sortBy: 'latest' });
onMounted(fetchPosts);
async function fetchPosts() { const data = await publicApi.getWantedPosts({ page: 1, size: 12, ...filters }); records.value = data.records || []; }
</script>

<style scoped>
.page-block { padding: 26px 0 40px; }
.filters { padding: 18px; margin-bottom: 20px; }
.wanted-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.wanted-card { padding: 22px; }
.wanted-card h3 { margin: 14px 0 10px; font-size: 22px; }
.wanted-card p { color: var(--text-soft); min-height: 72px; }
.wanted-card strong { color: var(--accent); font-family: var(--font-display); font-size: 24px; }
@media (max-width: 900px) { .wanted-grid { grid-template-columns: 1fr; } }
</style>