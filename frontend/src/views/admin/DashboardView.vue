<template>
  <div class="dashboard-stack" v-loading="isLoading">
    <section class="overview-hero glass-card">
      <div>
        <div class="app-chip">Operations Center</div>
        <h2>校园交易后台看板</h2>
        <p>快速把握用户、商品、订单和搜索热点，作为审核与运营工作的统一入口。</p>
      </div>
      <div class="overview-kpis"><div><small>待审核注册</small><strong>{{ overview.pendingRegistrationCount || 0 }}</strong></div><div><small>今日新增订单</small><strong>{{ overview.todayNewOrders || 0 }}</strong></div></div>
    </section>
    <div class="stats-grid"><StatCard label="总用户" :value="overview.totalUsers || 0" hint="活跃用户与注册审核可从这里快速判断系统热度。" /><StatCard label="在售商品" :value="overview.onSaleItemCount || 0" hint="当前公开交易池中的商品规模。" /><StatCard label="总订单" :value="overview.totalOrders || 0" hint="含进行中与已完成订单。" /><StatCard label="今日成交额" :value="formatPrice(overview.todayCompletedAmount || 0)" hint="今日已完成订单成交金额。" /></div>
    <div class="chart-grid"><section class="glass-card chart-panel"><SectionHeading title="订单趋势" description="近 7 天订单创建、完成和取消走势。" tag="Orders" /><VChart class="chart" :option="orderOption" autoresize /></section><section class="glass-card chart-panel"><SectionHeading title="商品状态分布" description="上架、下架、售出等商品状态占比。" tag="Items" /><VChart class="chart" :option="itemStatusOption" autoresize /></section></div>
    <div class="chart-grid"><section class="glass-card chart-panel"><SectionHeading title="分类成交排行" description="查看成交金额靠前的商品类目。" tag="Ranking" /><VChart class="chart" :option="categoryOption" autoresize /></section><section class="glass-card chart-panel"><SectionHeading title="用户增长" description="近 7 天新增用户与累计用户。" tag="Growth" /><VChart class="chart" :option="userGrowthOption" autoresize /></section></div>
    <div class="chart-grid"><section class="glass-card panel"><SectionHeading title="热门搜索词" description="辅助理解校园当前交易需求热点。" tag="Keywords" /><div class="keyword-grid"><div v-for="item in keywords" :key="item.keyword" class="keyword-card"><strong>{{ item.keyword }}</strong><span>{{ item.searchCount || 0 }} 次搜索</span><small>{{ item.categoryName || '全站' }}</small></div></div></section><section class="glass-card panel"><SectionHeading title="最近后台操作" description="方便快速查看审核、封禁、下架等操作。" tag="Audit" /><el-table :data="activities" stripe><el-table-column prop="operatorName" label="管理员" width="160" /><el-table-column prop="operationType" label="操作类型" width="180" /><el-table-column prop="operationContent" label="操作内容" min-width="260" /><el-table-column prop="createdAt" label="时间" width="180" /></el-table></section></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import VChart from 'vue-echarts';
import SectionHeading from '@/components/common/SectionHeading.vue';
import StatCard from '@/components/common/StatCard.vue';
import { adminApi } from '@/api/admin';
import { formatDate, formatPrice } from '@/utils/format';
const overview = ref<any>({}); const trends = ref<any[]>([]); const itemStatus = ref<any[]>([]); const categories = ref<any[]>([]); const growth = ref<any[]>([]); const activities = ref<any[]>([]); const keywords = ref<any[]>([]); const isLoading = ref(false);
onMounted(async () => { isLoading.value = true; try { const [a, b, c, d, e, f, g] = await Promise.all([adminApi.getDashboardOverview(), adminApi.getOrderTrends({ days: 7 }), adminApi.getItemStatus(), adminApi.getCategorySalesRanking({ days: 30, limit: 8 }), adminApi.getUserGrowth({ days: 7 }), adminApi.getRecentActivities({ limit: 8 }), adminApi.getHotKeywords({ days: 7, limit: 8 })]); overview.value = a || {}; trends.value = b || []; itemStatus.value = c || []; categories.value = d || []; growth.value = e || []; activities.value = f || []; keywords.value = g || []; } finally { isLoading.value = false; } });
const orderOption = computed(() => ({ tooltip: { trigger: 'axis' }, legend: { top: 0 }, xAxis: { type: 'category', data: trends.value.map((i) => formatDate(i.statDate)) }, yAxis: { type: 'value' }, series: [{ name: '创建', type: 'line', smooth: true, data: trends.value.map((i) => i.createdCount || 0) }, { name: '完成', type: 'line', smooth: true, data: trends.value.map((i) => i.completedCount || 0) }, { name: '取消', type: 'line', smooth: true, data: trends.value.map((i) => i.cancelledCount || 0) }] }));
const itemStatusOption = computed(() => ({ tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: ['46%', '72%'], data: itemStatus.value.map((i) => ({ name: i.itemStatus, value: i.itemCount })) }] }));
const categoryOption = computed(() => ({ tooltip: { trigger: 'axis' }, xAxis: { type: 'value' }, yAxis: { type: 'category', data: categories.value.map((i) => i.categoryName) }, series: [{ type: 'bar', data: categories.value.map((i) => i.completedAmount || 0), itemStyle: { color: '#dc7f39' } }] }));
const userGrowthOption = computed(() => ({ tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: growth.value.map((i) => formatDate(i.statDate)) }, yAxis: { type: 'value' }, series: [{ name: '新增用户', type: 'bar', data: growth.value.map((i) => i.newUserCount || 0), itemStyle: { color: '#193f3a' } }, { name: '累计用户', type: 'line', smooth: true, data: growth.value.map((i) => i.totalUserCount || 0), itemStyle: { color: '#234e77' } }] }));
</script>

<style scoped>
.dashboard-stack { display: grid; gap: 22px; }
.overview-hero { display: flex; justify-content: space-between; gap: 20px; align-items: end; padding: 24px 26px; background: linear-gradient(135deg, rgba(25, 63, 58, 0.09), rgba(220, 127, 57, 0.08)); }
.overview-hero h2 { margin: 10px 0 8px; font-family: var(--font-display); font-size: 42px; }
.overview-hero p { margin: 0; color: var(--text-soft); max-width: 620px; }
.overview-kpis { display: flex; gap: 18px; }
.overview-kpis > div { min-width: 150px; padding: 16px 18px; border-radius: 18px; background: rgba(255, 255, 255, 0.56); }
.overview-kpis small { display: block; color: var(--text-soft); margin-bottom: 8px; }
.overview-kpis strong { font-size: 32px; font-family: var(--font-display); }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; }
.chart-panel, .panel { padding: 22px; }
.chart { height: 320px; }
.keyword-grid { display: grid; gap: 12px; }
.keyword-card { padding: 16px 18px; border-radius: 16px; background: rgba(25,63,58,0.06); display: grid; gap: 6px; }
.keyword-card strong { font-size: 20px; }
.keyword-card span, .keyword-card small { color: var(--text-soft); }
@media (max-width: 1100px) { .overview-hero, .stats-grid, .chart-grid { grid-template-columns: 1fr; display: grid; } .overview-kpis { display: grid; grid-template-columns: 1fr 1fr; } }
@media (max-width: 700px) { .overview-kpis { grid-template-columns: 1fr; } }
</style>