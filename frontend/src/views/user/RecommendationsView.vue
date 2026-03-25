<template>
  <section class="glass-card panel">
    <SectionHeading title="为你推荐" description="根据浏览、搜索、求购和交易行为生成的推荐结果。" tag="Recommendations"><el-button type="primary" @click="refreshList">刷新推荐</el-button></SectionHeading>
    <div v-if="records.length" class="item-grid">
      <article v-for="item in records" :key="item.recommendationId" class="recommend-card glass-card" @click="openItem(item)">
        <img v-if="item.coverImageUrl" :src="item.coverImageUrl" :alt="item.title" />
        <div class="fallback" v-else>{{ item.title.slice(0,2) }}</div>
        <div class="body"><strong>{{ item.title }}</strong><p>{{ item.reasonText || item.reasonCode || '行为推荐' }}</p><span>{{ formatPrice(item.price) }}</span></div>
      </article>
    </div>
    <EmptyState v-else title="推荐尚未生成" description="先多浏览几件商品，系统会逐步形成推荐。" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { formatPrice } from '@/utils/format';
const router = useRouter();
const records = ref<any[]>([]);
onMounted(fetchList);
async function fetchList() { const data = await userApi.getRecommendations({ page: 1, size: 20 }); records.value = data.records || []; }
async function refreshList() { await userApi.refreshRecommendations(12); ElMessage.success('推荐已刷新'); fetchList(); }
async function openItem(item: any) { await userApi.clickRecommendation(item.recommendationId); router.push(`/items/${item.itemId}`); }
</script>

<style scoped>
.panel { padding: 24px; }
.item-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.recommend-card { overflow: hidden; cursor: pointer; }
.recommend-card img, .fallback { width: 100%; aspect-ratio: 4/3; object-fit: cover; }
.fallback { display: grid; place-items: center; background: rgba(25,63,58,0.08); font-size: 34px; font-family: var(--font-display); }
.body { padding: 16px; }
.body p { color: var(--text-soft); min-height: 44px; }
.body span { color: var(--accent); font-family: var(--font-display); font-size: 24px; }
@media (max-width: 900px) { .item-grid { grid-template-columns: 1fr; } }
</style>