<template>
  <div class="page-shell page-block">
    <SectionHeading title="商品广场" description="按分类、关键词、价格和交易方式快速筛选校园二手物品。" tag="Marketplace" />

    <section class="glass-card filters fade-up">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="名称 / 品牌 / 型号" @keyup.enter="applyFilters" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" clearable filterable style="width: 170px">
            <el-option v-for="item in categories" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="filters.brand" clearable placeholder="例如 Apple / 华为" />
        </el-form-item>
        <el-form-item label="最低价">
          <el-input-number v-model="filters.priceMin" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="最高价">
          <el-input-number v-model="filters.priceMax" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="交易方式">
          <el-select v-model="filters.tradeMode" clearable style="width: 150px">
            <el-option label="线上 + 线下" value="both" />
            <el-option label="仅线下" value="offline" />
            <el-option label="仅线上" value="online" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filters.sortBy" style="width: 150px">
            <el-option label="最新发布" value="latest" />
            <el-option label="价格升序" value="price_asc" />
            <el-option label="价格降序" value="price_desc" />
            <el-option label="热门浏览" value="popular" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" @click="applyFilters">筛选商品</el-button>
          <el-button @click="reset">重置条件</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-alert
      v-if="demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled"
      type="info"
      :closable="false"
      show-icon
      class="demo-alert"
      title="带有“演示条目”标签的商品是答辩样例，可用于讲解评论、推荐和订单流程。"
    />

    <section class="result-bar">
      <div>
        <strong>{{ total }}</strong>
        <span>件公开在售商品</span>
      </div>
      <div class="result-tags">
        <el-tag v-if="filters.keyword" round type="success">关键词：{{ filters.keyword }}</el-tag>
        <el-tag v-if="filters.categoryId" round>分类：{{ currentCategoryName }}</el-tag>
        <el-tag v-if="activeFilterCount > 0" round type="warning">已启用 {{ activeFilterCount }} 个筛选条件</el-tag>
      </div>
    </section>

    <div v-if="isLoading" class="item-grid skeleton-grid" aria-hidden="true">
      <article v-for="index in 8" :key="index" class="glass-card skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-image" />
            <div class="skeleton-body">
              <el-skeleton-item variant="text" style="width: 42%" />
              <el-skeleton-item variant="h3" style="width: 76%; margin-top: 14px" />
              <el-skeleton-item variant="text" style="width: 58%; margin-top: 12px" />
              <div class="skeleton-meta">
                <el-skeleton-item variant="text" style="width: 34%" />
                <el-skeleton-item variant="text" style="width: 22%" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </article>
    </div>

    <div v-else-if="records.length" class="item-grid">
      <ItemCard v-for="item in records" :key="item.itemId" :item="item" :demo-notes-enabled="Boolean(demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled)" />
    </div>

    <EmptyState v-else title="没有找到匹配商品" description="换个关键词，或者放宽价格和分类条件再试试。">
      <el-button type="primary" @click="reset">清空筛选</el-button>
    </EmptyState>

    <div v-if="total > size" class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import EmptyState from '@/components/common/EmptyState.vue';
import ItemCard from '@/components/common/ItemCard.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import type { ItemCategory, ItemSummary } from '@/types/api';

const route = useRoute();
const router = useRouter();

const categories = ref<ItemCategory[]>([]);
const records = ref<ItemSummary[]>([]);
const total = ref(0);
const page = ref(1);
const size = 12;
const isLoading = ref(false);
const demoStatus = ref<any>({ demoModeEnabled: false, demoItemNotesEnabled: true });

const filters = reactive({
  categoryId: undefined as number | undefined,
  keyword: '',
  brand: '',
  priceMin: undefined as number | undefined,
  priceMax: undefined as number | undefined,
  tradeMode: '',
  sortBy: 'latest'
});

const currentCategoryName = computed(() => categories.value.find((item) => item.categoryId === filters.categoryId)?.categoryName || '--');
const activeFilterCount = computed(() => {
  return [filters.categoryId, filters.keyword, filters.brand, filters.priceMin, filters.priceMax, filters.tradeMode]
    .filter((item) => item !== undefined && item !== null && item !== '').length;
});

onMounted(async () => {
  const [categoryList, demoMode] = await Promise.all([publicApi.getCategories(), publicApi.getDemoModeStatus()]);
  categories.value = categoryList;
  demoStatus.value = demoMode;
  syncFromRoute(route.query as Record<string, unknown>);
  await fetchItems();
});

watch(
  () => route.query,
  async (query, previous) => {
    if (JSON.stringify(query) === JSON.stringify(previous)) return;
    syncFromRoute(query as Record<string, unknown>);
    await fetchItems();
  }
);

function syncFromRoute(query: Record<string, unknown>) {
  page.value = Number(readString(query.page) || 1);
  filters.categoryId = query.categoryId ? Number(readString(query.categoryId)) : undefined;
  filters.keyword = readString(query.keyword);
  filters.brand = readString(query.brand);
  filters.priceMin = query.priceMin ? Number(readString(query.priceMin)) : undefined;
  filters.priceMax = query.priceMax ? Number(readString(query.priceMax)) : undefined;
  filters.tradeMode = readString(query.tradeMode);
  filters.sortBy = readString(query.sortBy) || 'latest';
}

async function fetchItems() {
  isLoading.value = true;
  try {
    const data = await publicApi.getItems({
      ...filters,
      page: page.value,
      size
    });
    records.value = data.records || [];
    total.value = Number(data.total || 0);
  } finally {
    isLoading.value = false;
  }
}

function buildQuery(nextPage = page.value) {
  return {
    page: String(nextPage),
    categoryId: filters.categoryId ? String(filters.categoryId) : undefined,
    keyword: filters.keyword || undefined,
    brand: filters.brand || undefined,
    priceMin: filters.priceMin !== undefined ? String(filters.priceMin) : undefined,
    priceMax: filters.priceMax !== undefined ? String(filters.priceMax) : undefined,
    tradeMode: filters.tradeMode || undefined,
    sortBy: filters.sortBy || undefined
  };
}

function applyFilters() {
  router.push({ name: 'items', query: buildQuery(1) });
}

function handlePageChange(nextPage: number) {
  router.push({ name: 'items', query: buildQuery(nextPage) });
}

function reset() {
  filters.categoryId = undefined;
  filters.keyword = '';
  filters.brand = '';
  filters.priceMin = undefined;
  filters.priceMax = undefined;
  filters.tradeMode = '';
  filters.sortBy = 'latest';
  router.push({ name: 'items' });
}

function readString(value: unknown) {
  if (Array.isArray(value)) return value[0] || '';
  return value ? String(value) : '';
}
</script>

<style scoped>
.page-block {
  padding: 26px 0 40px;
}
.filters {
  padding: 22px;
  margin-bottom: 20px;
}
.demo-alert {
  margin-bottom: 20px;
}
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 8px;
}
.filter-actions :deep(.el-form-item__content) {
  display: flex;
  gap: 10px;
}
.result-bar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  align-items: center;
  color: var(--text-soft);
}
.result-bar strong {
  margin-right: 6px;
  font-size: 28px;
  color: var(--brand);
  font-family: var(--font-display);
}
.result-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
.item-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.skeleton-card {
  overflow: hidden;
  padding: 0;
}
.skeleton-image {
  width: 100%;
  height: 220px;
}
.skeleton-body {
  padding: 18px;
}
.skeleton-meta {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.pager {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
@media (max-width: 1100px) {
  .item-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .result-bar {
    flex-direction: column;
    align-items: flex-start;
  }
  .result-tags {
    justify-content: flex-start;
  }
}
@media (max-width: 680px) {
  .item-grid {
    grid-template-columns: 1fr;
  }
  .filters {
    padding: 18px;
  }
}
</style>