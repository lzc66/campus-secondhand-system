<template>
  <div class="dashboard-stack" v-loading="isLoading">
    <section class="overview-hero glass-card fade-up">
      <div class="hero-copy">
        <div class="app-chip">Operations Center</div>
        <h2>校园二手交易后台</h2>
        <p>统一查看注册审核、商品交易、搜索热词和用户增长，并在答辩演示前快速切换演示模式。</p>
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
        <small>公开在售商品</small>
        <strong>{{ overview.onSaleItemCount || 0 }} 件</strong>
      </article>
      <article class="status-card glass-card">
        <small>系统状态</small>
        <strong>{{ hasAnyData ? '已有业务数据沉淀' : '适合先注入演示样例' }}</strong>
      </article>
    </section>

    <section class="demo-panel glass-card">
      <div class="demo-header">
        <div>
          <SectionHeading title="演示模式" description="向系统注入一批离线可用的演示商品、订单、求购、公告和推荐数据。" tag="Demo" />
          <p class="demo-meta">当前状态：{{ demoStatus.demoModeEnabled ? '已开启' : '已关闭' }}，条目说明：{{ demoStatus.demoItemNotesEnabled ? '已显示' : '已隐藏' }}</p>
        </div>
        <div class="demo-actions">
          <el-button type="primary" :loading="seedLoading" :disabled="!canManageDemoMode || demoStatus.demoDataSeeded" @click="handleSeedDemoData">
            {{ demoStatus.demoDataSeeded ? '演示数据已注入' : '注入演示数据' }}
          </el-button>
          <el-button :loading="saveLoading" :disabled="!canManageDemoMode" @click="saveDemoModeSettings">保存开关</el-button>
        </div>
      </div>

      <div class="demo-switches">
        <div class="switch-card">
          <div>
            <strong>演示模式总开关</strong>
            <p>控制前台和用户端是否按“演示环境”展示样例提示。</p>
          </div>
          <el-switch v-model="demoForm.demoModeEnabled" :disabled="!canManageDemoMode" />
        </div>
        <div class="switch-card">
          <div>
            <strong>演示条目说明</strong>
            <p>开启后，会在演示商品和演示订单附近显示说明标签，方便答辩讲解。</p>
          </div>
          <el-switch v-model="demoForm.demoItemNotesEnabled" :disabled="!canManageDemoMode" />
        </div>
      </div>

      <el-alert
        v-if="demoForm.demoModeEnabled && demoForm.demoItemNotesEnabled"
        type="warning"
        :closable="false"
        show-icon
        title="当前已开启演示条目说明，前台商品卡片、商品详情和订单页会显示“演示条目”提示。"
      />

      <div class="demo-summary-grid">
        <article class="demo-summary-card">
          <small>演示用户</small>
          <strong>{{ demoStatus.demoSummary?.userCount || 0 }}</strong>
        </article>
        <article class="demo-summary-card">
          <small>演示商品</small>
          <strong>{{ demoStatus.demoSummary?.itemCount || 0 }}</strong>
        </article>
        <article class="demo-summary-card">
          <small>演示订单</small>
          <strong>{{ demoStatus.demoSummary?.orderCount || 0 }}</strong>
        </article>
        <article class="demo-summary-card">
          <small>待审核样例</small>
          <strong>{{ demoStatus.demoSummary?.pendingRegistrationCount || 0 }}</strong>
        </article>
      </div>

      <p class="demo-meta">演示数据注入时间：{{ demoStatus.demoDataSeededAt ? formatDateTime(demoStatus.demoDataSeededAt) : '未注入' }}</p>
    </section>

    <div class="stats-grid">
      <StatCard label="总用户数" :value="overview.totalUsers || 0" hint="含已审核通过的在校学生。" />
      <StatCard label="在售商品" :value="overview.onSaleItemCount || 0" hint="当前公开交易池中的商品规模。" />
      <StatCard label="总订单数" :value="overview.totalOrders || 0" hint="包含进行中与已完成订单。" />
      <StatCard label="今日成交额" :value="formatPrice(overview.todayCompletedAmount || 0)" hint="今日已完成订单对应的成交金额。" />
    </div>

    <div class="chart-grid">
      <section class="glass-card chart-panel">
        <SectionHeading title="订单趋势" description="近 7 天订单创建、完成和取消趋势。" tag="Orders" />
        <VChart v-if="trends.length" class="chart" :option="orderOption" autoresize />
        <EmptyState v-else title="暂无订单趋势数据" description="待用户下单后，这里会显示订单走势。" />
      </section>
      <section class="glass-card chart-panel">
        <SectionHeading title="商品状态分布" description="上架、预留、售出等状态占比。" tag="Items" />
        <VChart v-if="itemStatus.length" class="chart" :option="itemStatusOption" autoresize />
        <EmptyState v-else title="暂无商品状态数据" description="可先注入演示数据或发布几件商品。" />
      </section>
    </div>

    <div class="chart-grid">
      <section class="glass-card chart-panel">
        <SectionHeading title="分类成交排行" description="按完成订单统计成交金额靠前的分类。" tag="Ranking" />
        <VChart v-if="categories.length" class="chart" :option="categoryOption" autoresize />
        <EmptyState v-else title="暂无分类成交数据" description="订单完成后，这里会出现分类成交排名。" />
      </section>
      <section class="glass-card chart-panel">
        <SectionHeading title="用户增长" description="近 7 天新增用户和累计用户。" tag="Growth" />
        <VChart v-if="growth.length" class="chart" :option="userGrowthOption" autoresize />
        <EmptyState v-else title="暂无用户增长数据" description="审核通过新用户后，这里会形成增长曲线。" />
      </section>
    </div>

    <div class="chart-grid">
      <section class="glass-card panel">
        <SectionHeading title="热门搜索词" description="辅助判断当前校园内最热的交易需求。" tag="Keywords" />
        <div v-if="keywords.length" class="keyword-grid">
          <div v-for="item in keywords" :key="item.keyword" class="keyword-card">
            <strong>{{ item.keyword }}</strong>
            <span>{{ item.searchCount || 0 }} 次搜索</span>
            <small>{{ item.categoryName || '全站' }}</small>
          </div>
        </div>
        <EmptyState v-else title="暂无热门搜索词" description="搜索行为积累后，这里会显示站内热词。" />
      </section>

      <section class="glass-card panel">
        <SectionHeading title="最近后台操作" description="快速回看审核、下架、关闭订单等操作。" tag="Audit" />
        <el-table v-if="activities.length" :data="activities" stripe>
          <el-table-column prop="operatorName" label="管理员" width="150" />
          <el-table-column label="操作类型" width="150">
            <template #default="scope">{{ labelize(scope.row.operationType, '--') }}</template>
          </el-table-column>
          <el-table-column prop="operationContent" label="操作内容" min-width="240" />
          <el-table-column label="时间" width="180">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <EmptyState v-else title="暂无后台操作记录" description="完成审核、公告发布或订单干预后，这里会出现最近操作。" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ElMessage } from 'element-plus';
import VChart from 'vue-echarts';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import StatCard from '@/components/common/StatCard.vue';
import { adminApi } from '@/api/admin';
import { useAuthStore } from '@/stores/auth';
import { formatDate, formatDateTime, formatPrice, labelize } from '@/utils/format';

const authStore = useAuthStore();
const overview = ref<any>({});
const trends = ref<any[]>([]);
const itemStatus = ref<any[]>([]);
const categories = ref<any[]>([]);
const growth = ref<any[]>([]);
const activities = ref<any[]>([]);
const keywords = ref<any[]>([]);
const demoStatus = ref<any>({
  demoModeEnabled: false,
  demoItemNotesEnabled: true,
  demoDataSeeded: false,
  demoSummary: {}
});
const demoForm = reactive({
  demoModeEnabled: false,
  demoItemNotesEnabled: true
});
const isLoading = ref(false);
const seedLoading = ref(false);
const saveLoading = ref(false);
const lastRefreshAt = ref('');

const canManageDemoMode = computed(() => ['super_admin', 'operator'].includes(authStore.adminProfile?.roleCode || ''));
const hasAnyData = computed(() => Boolean((overview.value.totalUsers || 0) + (overview.value.totalOrders || 0) + (overview.value.onSaleItemCount || 0)));
const lastRefreshText = computed(() => (lastRefreshAt.value ? formatDateTime(lastRefreshAt.value) : '--'));

onMounted(fetchDashboard);

async function fetchDashboard() {
  isLoading.value = true;
  try {
    const [a, b, c, d, e, f, g, h] = await Promise.all([
      adminApi.getDashboardOverview(),
      adminApi.getOrderTrends({ days: 7 }),
      adminApi.getItemStatus(),
      adminApi.getCategorySalesRanking({ days: 30, limit: 8 }),
      adminApi.getUserGrowth({ days: 7 }),
      adminApi.getRecentActivities({ limit: 8 }),
      adminApi.getHotKeywords({ days: 7, limit: 8 }),
      adminApi.getDemoModeStatus()
    ]);
    overview.value = a || {};
    trends.value = b || [];
    itemStatus.value = c || [];
    categories.value = d || [];
    growth.value = e || [];
    activities.value = f || [];
    keywords.value = g || [];
    demoStatus.value = h || demoStatus.value;
    demoForm.demoModeEnabled = Boolean(h?.demoModeEnabled);
    demoForm.demoItemNotesEnabled = Boolean(h?.demoItemNotesEnabled);
    lastRefreshAt.value = new Date().toISOString();
  } finally {
    isLoading.value = false;
  }
}

async function handleSeedDemoData() {
  seedLoading.value = true;
  try {
    const result = await adminApi.seedDemoData();
    ElMessage.success(`演示数据已准备：新增 ${result.createdCounts.itemCount || 0} 件商品，${result.createdCounts.orderCount || 0} 笔订单`);
    await fetchDashboard();
  } finally {
    seedLoading.value = false;
  }
}

async function saveDemoModeSettings() {
  saveLoading.value = true;
  try {
    await adminApi.updateDemoMode({
      demoModeEnabled: demoForm.demoModeEnabled,
      demoItemNotesEnabled: demoForm.demoItemNotesEnabled
    });
    ElMessage.success('演示模式设置已保存');
    await fetchDashboard();
  } finally {
    saveLoading.value = false;
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
  series: [{
    type: 'pie',
    radius: ['46%', '72%'],
    data: itemStatus.value.map((i) => ({ name: labelize(i.itemStatus, '--'), value: i.itemCount }))
  }]
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
  background: linear-gradient(135deg, rgba(25, 63, 58, 0.1), rgba(220, 127, 57, 0.08)), linear-gradient(180deg, rgba(255, 255, 255, 0.52), rgba(255, 255, 255, 0.18));
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
.overview-kpis small,
.status-card small,
.demo-summary-card small {
  display: block;
  color: var(--text-soft);
  margin-bottom: 8px;
}
.overview-kpis strong,
.status-card strong,
.demo-summary-card strong {
  font-size: 32px;
  font-family: var(--font-display);
}
.status-ribbon {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.status-card,
.demo-summary-card {
  padding: 16px 18px;
}
.demo-panel {
  padding: 22px;
  display: grid;
  gap: 18px;
}
.demo-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: start;
}
.demo-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.demo-switches,
.demo-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.demo-summary-grid {
  grid-template-columns: repeat(4, 1fr);
}
.switch-card,
.demo-summary-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(25, 63, 58, 0.05);
}
.switch-card {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}
.switch-card strong {
  display: block;
  margin-bottom: 6px;
}
.switch-card p,
.demo-meta {
  margin: 0;
  color: var(--text-soft);
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
  .status-ribbon,
  .demo-summary-grid,
  .demo-switches {
    grid-template-columns: 1fr;
    display: grid;
  }
  .overview-kpis {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .demo-header {
    flex-direction: column;
  }
}
@media (max-width: 700px) {
  .overview-kpis {
    grid-template-columns: 1fr;
  }
  .switch-card {
    align-items: start;
  }
}
</style>