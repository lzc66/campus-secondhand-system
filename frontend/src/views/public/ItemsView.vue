<template>
  <div class="page-shell page-block" v-loading="isLoading">
    <SectionHeading title="商品广场" description="按分类、关键词、价格和交易方式检索校园二手物品。" tag="Marketplace" />
    <section class="glass-card filters">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词"><el-input v-model="filters.keyword" placeholder="名称 / 品牌 / 型号" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="filters.categoryId" clearable style="width: 160px"><el-option v-for="item in categories" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" /></el-select></el-form-item>
        <el-form-item label="品牌"><el-input v-model="filters.brand" /></el-form-item>
        <el-form-item label="最低价"><el-input-number v-model="filters.priceMin" :min="0" /></el-form-item>
        <el-form-item label="最高价"><el-input-number v-model="filters.priceMax" :min="0" /></el-form-item>
        <el-form-item label="交易方式"><el-select v-model="filters.tradeMode" clearable style="width: 140px"><el-option label="线上+线下" value="both" /><el-option label="仅线下" value="offline" /><el-option label="仅在线" value="online" /></el-select></el-form-item>
        <el-form-item label="排序"><el-select v-model="filters.sortBy" style="width: 140px"><el-option label="最新发布" value="latest" /><el-option label="价格升序" value="price_asc" /><el-option label="价格降序" value="price_desc" /><el-option label="浏览热度" value="popular" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchItems">筛选</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </section>
    <div class="result-bar"><span>当前结果：{{ total }} 件商品</span><span v-if="filters.keyword">关键词“{{ filters.keyword }}”</span></div>
    <div v-if="records.length" class="item-grid"><ItemCard v-for="item in records" :key="item.itemId" :item="item" /></div>
    <EmptyState v-else title="没有找到匹配商品" description="换个关键词或放宽筛选条件试试。" />
    <div class="pager"><el-pagination layout="prev, pager, next, total" :current-page="page" :page-size="size" :total="total" @current-change="handlePageChange" /></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import EmptyState from '@/components/common/EmptyState.vue';
import ItemCard from '@/components/common/ItemCard.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import type { ItemCategory, ItemSummary } from '@/types/api';
const route = useRoute(); const router = useRouter(); const categories = ref<ItemCategory[]>([]); const records = ref<ItemSummary[]>([]); const total = ref(0); const page = ref(Number(route.query.page || 1)); const size = ref(12); const isLoading = ref(false);
const filters = reactive({ categoryId: route.query.categoryId ? Number(route.query.categoryId) : undefined, keyword: String(route.query.keyword || ''), brand: '', priceMin: undefined as number | undefined, priceMax: undefined as number | undefined, tradeMode: '', sortBy: String(route.query.sortBy || 'latest') });
onMounted(async () => { categories.value = await publicApi.getCategories(); fetchItems(); });
async function fetchItems() { isLoading.value = true; try { const data = await publicApi.getItems({ ...filters, page: page.value, size }); records.value = data.records || []; total.value = Number(data.total || 0); router.replace({ query: { ...route.query, page: String(page.value), keyword: filters.keyword || undefined, categoryId: filters.categoryId ? String(filters.categoryId) : undefined, sortBy: filters.sortBy || undefined } }); } finally { isLoading.value = false; } }
function handlePageChange(nextPage: number) { page.value = nextPage; fetchItems(); }
function reset() { filters.categoryId = undefined; filters.keyword = ''; filters.brand = ''; filters.priceMin = undefined; filters.priceMax = undefined; filters.tradeMode = ''; filters.sortBy = 'latest'; page.value = 1; fetchItems(); }
</script>

<style scoped>
.page-block { padding: 26px 0 40px; }
.filters { padding: 20px; margin-bottom: 22px; }
.result-bar { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 16px; color: var(--text-soft); }
.item-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
.pager { margin-top: 24px; display: flex; justify-content: center; }
@media (max-width: 1000px) { .item-grid { grid-template-columns: repeat(2, 1fr); } .result-bar { flex-direction: column; } }
@media (max-width: 640px) { .item-grid { grid-template-columns: 1fr; } }
</style>