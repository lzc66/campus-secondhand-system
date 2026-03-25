<template>
  <div class="dashboard-stack" v-loading="isLoading">
    <section class="overview-hero glass-card fade-up">
      <div class="hero-copy">
        <div class="app-chip">Operations Center</div>
        <h2>校园交易管理后台</h2>
        <p>快速把握用户、商品、订单和搜索热点，作为审核、运营与答辩展示时的数据总入口。</p>
        <div class="hero-actions">
          <el-button type="primary" @click="fetchDashboard">刷新看板</el-button>
          <RouterLink class="hero-link" to="/admin/reports">查看报表导出</RouterLink>
          <RouterLink class="hero-link" to="/admin/announcements">管理系统公告</RouterLink>
        </div>
      </div>
      <div class="overview-kpis">
        <div>
          <small>待审核注册</small>
          <strong>{{ overview.pendingRegistrationCount || 0 }}</strong>
        </div>
        <div>
          <small>今日新增订单</small>
          <strong>{{ overview.todayNewOrders || 0 }}</strong>
        </div>
      </div>
    </section>

    <section class="status-ribbon">
      <article class="status-card glass-card">
        <small>最近刷新</small>
        <strong>{{ lastRefreshText }}</strong>
      </article>
      <article class="status-card glass-card">
        <small>公开商品池</small>
        <strong>{{ overview.onSaleItemCount || 0 }} 件在售</strong>
      </article>
      <article class="status-card glass-card">
        <small>系统提示</small>
        <strong>{{ hasAnyData ? '已有业务数据沉淀' : '当前更适合演示流程' }}</strong>
      </article>
    </section>

    <div class="stats-grid">
      <StatCard label="总用户" :value="overview.totalUsers || 0" hint="活跃用户与注册审核可从这里快速判断系统热度。" />
      <StatCard label="在售商品" :value="overview.onSaleItemCount || 0" hint="当前公开交易池中的商品规模。" />
      <StatCard label="总订单" :value="overview.totalOrders || 0" hint="含进行中与已完成订单。" />
      <StatCard label="今日成交额" :value="formatPrice(overview.todayCompletedAmount || 0)" hint="今日已完成订单成交金额。" />
    </div>

    <div class="chart-grid">
      <section class="glass-card chart-panel">
        <SectionHeading title="订单趋势" description="近 7 天订单创建、完成和取消走势。" tag="Orders" />
        <VChart v-if="trends.length" class="chart" :option="orderOption" autoresize />
        <EmptyState v-else title="暂无订单趋势数据" description="当前还没有订单记录，待用户下单后这里会展示趋势变化。" />
      </section>
      <section class="glass-card chart-panel">
        <SectionHeading title="商品状态分布" description="上架、下架、售出等商品状态占比。" tag="Items" />
        <VChart v-if="itemStatus.length" class="chart" :option="itemStatusOption" autoresize />
        <EmptyState v-else title="暂无商品状态数据" description="可以先发布商品，再回到看板查看状态分布。" />
      </section>
    </div>

    <div class="chart-grid">
      <section class="glass-card chart-panel">
        <SectionHeading title="分类成交排行" description="查看成交金额靠前的商品类目。" tag="Ranking" />
        <VChart v-if="categories.length" class="chart" :option="categoryOption" autoresize />
        <EmptyState v-else title="暂无分类成交数据" description="订单完成后，这里会显示各分类的成交金额排行。" />
      </section>
      <section class="glass-card chart-panel">
        <SectionHeading title="用户增长" description="近 7 天新增用户与累计用户。" tag="Growth" />
        <VChart v-if="growth.length" class="chart" :option="userGrowthOption" autoresize />
        <EmptyState v-else title="暂无用户增长数据" description="管理员审核通过用户注册后，这里会开始形成增长曲线。" />
      </section>
    </div>

    <div class="chart-grid">
      <section class="glass-card panel">
        <SectionHeading title="热门搜索词" description="辅助理解校园当前交易需求热点。" tag="Keywords" />
        <div v-if="keywords.length" class="keyword-grid">
          <div v-for="item in keywords" :key="item.keyword" class="keyword-card">
            <strong>{{ item.keyword }}</strong>
            <span>{{ item.searchCount || 0 }} 次搜索</span>
            <small>{{ item.categoryName || '全站' }}</small>
          </div>
        </div>
        <EmptyState v-else title="暂无搜索热词" description="商品搜索行为累积后，这里会显示站内热度关键词。" />
      </section>

      <section class="glass-card panel">
        <SectionHeading title="最近后台操作" description="方便快速查看审核、封禁、下架等操作。" tag="Audit" />
        <el-table v-if="activities.length" :data="activities" stripe>
          <el-table-column prop="operatorName" label="管理员" width="160" />
          <el-table-column prop="operationType" label="操作类型" width="180" />
          <el-table-column prop="operationContent" label="操作内容" min-width="260" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
        <EmptyState v-else title="暂无后台操作记录" description="完成注册审核、公告发布或订单干预后，这里会出现最近操作。" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import VChart from 'vue-echarts';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import StatCard from '@/components/common/StatCard.vue';
import { adminApi } from '@/api/admin';
import { formatDate, formatDateTime, formatPrice } from '@/utils/format';

const overview = ref<any>({});
const trends = ref<any[]>([]);
const itemStatus = ref<any[]>([]);
const categories = ref<any[]>([]);
const growth = ref<any[]>([]);
const activities = ref<any[]>([]);
const keywords = ref<any[]>([]);
const isLoading = ref(false);
const lastRefreshAt = ref<string>('');

const hasAnyData = computed(() => {
  return Boolean((overview.value.totalUsers || 0) + (overview.value.totalOrders || 0) + (overview.value.onSaleItemCount || 0));
});
const lastRefreshText = computed(() => (lastRefreshAt.value ? formatDateTime(lastRefreshAt.value) : '--'));

onMounted(fetchDashboard);

async function fetchDashboard() {
  isLoading.value = true;
  try {
    const [a, b, c, d, e, f, g] = await Promise.all([
      adminApi.getDashboardOverview(),
      adminApi.getOrderTrends({ days: 7 }),
      adminApi.getItemStatus(),
      adminApi.getCategorySalesRanking({ days: 30, limit: 8 }),
      adminApi.getUserGrowth({ days: 7 }),
      adminApi.getRecentActivities({ limit: 8 }),
      adminApi.getHotKeywords({ days: 7, limit: 8 })
    ]);
    overview.value = a || {};
    trends.value = b || [];
    itemStatus.value = c || [];
    categories.value = d || [];
    growth.value = e || [];
    activities.value = f || [];
    keywords.value = g || [];
    lastRefreshAt.value = new Date().toISOString();
  } finally {
    isLoading.value = false;
  }
}

const orderOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  xAxis: { type: 'category', data: trends.value.map((i) => formatDate(i.statDate)) },
  yAxis: { type: 'value' },
  series: [
    { name: '创建', type: 'line', smooth: true, data: trends.value.map((i) => i.createdCount || 0) },
    { name: '完成', type: 'line', smooth: true, data: trends.value.map((i) => i.completedCount || 0) },
    { name: '取消', type: 'line', smooth: true, data: trends.value.map((i) => i.cancelledCount || 0) }
  ]
}));

const itemStatusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['46%', '72%'], data: itemStatus.value.map((i) => ({ name: i.itemStatus, value: i.itemCount })) }]
}));

const categoryOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value' },
  yAxis: { type: 'category', data: categories.value.map((i) => i.categoryName) },
  series: [{ type: 'bar', data: categories.value.map((i) => i.completedAmount || 0), itemStyle: { color: '#dc7f39' } }]
}));

const userGrowthOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: growth.value.map((i) => formatDate(i.statDate)) },
  yAxis: { type: 'value' },
  series: [
    { name: '新增用户', type: 'bar', data: growth.value.map((i) => i.newUserCount || 0), itemStyle: { color: '#193f3a' } },
    { name: '累计用户', type: 'line', smooth: true, data: growth.value.map((i) => i.totalUserCount || 0), itemStyle: { color: '#234e77' } }
  ]
}));
</script>

<style scoped>
.dashboard-stack {
  display: grid;
  gap: 22px;
}
.overview-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: end;
  padding: 26px 28px;
  background:
    linear-gradient(135deg, rgba(25, 63, 58, 0.1), rgba(220, 127, 57, 0.08)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.52), rgba(255, 255, 255, 0.18));
}
.hero-copy h2 {
  margin: 10px 0 8px;
  font-family: var(--font-display);
  font-size: 42px;
}
.hero-copy p {
  margin: 0;
  color: var(--text-soft);
  max-width: 620px;
}
.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.hero-link {
  display: inline-flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
}
.overview-kpis {
  display: flex;
  gap: 18px;
}
.overview-kpis > div {
  min-width: 160px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.56);
}
.overview-kpis small {
  display: block;
  color: var(--text-soft);
  margin-bottom: 8px;
}
.overview-kpis strong {
  font-size: 32px;
  font-family: var(--font-display);
}
.status-ribbon {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.status-card {
  padding: 16px 18px;
}
.status-card small {
  display: block;
  color: var(--text-soft);
  margin-bottom: 8px;
}
.status-card strong {
  font-size: 22px;
  font-family: var(--font-display);
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
}
.chart-panel,
.panel {
  padding: 22px;
}
.chart {
  height: 320px;
}
.keyword-grid {
  display: grid;
  gap: 12px;
}
.keyword-card {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(25, 63, 58, 0.06);
  display: grid;
  gap: 6px;
}
.keyword-card strong {
  font-size: 20px;
}
.keyword-card span,
.keyword-card small {
  color: var(--text-soft);
}
@media (max-width: 1100px) {
  .overview-hero,
  .stats-grid,
  .chart-grid,
  .status-ribbon {
    grid-template-columns: 1fr;
    display: grid;
  }
  .overview-kpis {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 700px) {
  .overview-kpis {
    grid-template-columns: 1fr;
  }
}
</style>