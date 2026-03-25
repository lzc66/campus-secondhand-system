<template>
  <section class="glass-card panel">
    <SectionHeading
      title="报表导出"
      description="支持按默认天数导出，也支持按时间范围生成 CSV，适合答辩演示和运营复盘。"
      tag="Reports"
    />

    <div class="config-shell">
      <div class="config-card glass-card">
        <div class="mode-switch">
          <span class="config-label">时间模式</span>
          <el-radio-group v-model="exportMode">
            <el-radio-button value="preset">按天数</el-radio-button>
            <el-radio-button value="range">按时间范围</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="exportMode === 'preset'" class="preset-block">
          <div class="preset-actions">
            <el-button
              v-for="preset in [7, 30, 90]"
              :key="preset"
              :type="form.days === preset ? 'primary' : 'default'"
              @click="form.days = preset"
            >
              最近 {{ preset }} 天
            </el-button>
          </div>
          <el-form label-width="90px" class="inline-form">
            <el-form-item label="导出天数">
              <el-input-number v-model="form.days" :min="1" :max="180" />
            </el-form-item>
            <el-form-item label="Top N">
              <el-input-number v-model="form.limit" :min="1" :max="50" />
            </el-form-item>
          </el-form>
        </div>

        <div v-else class="range-block">
          <el-alert title="自定义范围需要同时选择开始和结束日期。" type="info" :closable="false" show-icon />
          <el-form label-width="90px" class="inline-form range-form">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="rangeValue"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item label="Top N">
              <el-input-number v-model="form.limit" :min="1" :max="50" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="summary-card glass-card">
        <span class="summary-tag">当前设置</span>
        <h3>{{ exportMode === 'preset' ? `最近 ${form.days} 天` : rangeSummary }}</h3>
        <p>排行类报表将导出 Top {{ form.limit }} 数据；概览报表不受时间条件影响。</p>
      </div>
    </div>

    <div class="report-grid">
      <article v-for="report in reports" :key="report.key" class="report-card glass-card fade-up">
        <div>
          <span class="report-file">{{ report.filename }}</span>
          <h3>{{ report.title }}</h3>
          <p>{{ report.description }}</p>
        </div>
        <el-button type="primary" :loading="activeExportKey === report.key" @click="download(report)">
          导出 CSV
        </el-button>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { useAuthStore } from '@/stores/auth';
import { downloadFile } from '@/utils/download';

const authStore = useAuthStore();
const exportMode = ref<'preset' | 'range'>('preset');
const rangeValue = ref<string[]>([]);
const activeExportKey = ref('');

const form = reactive({
  days: 7,
  limit: 20
});

const reports = [
  {
    key: 'overview',
    title: '总览报表',
    description: '导出后台概览指标，适合答辩和整体运营汇报。',
    filename: 'dashboard-overview.csv',
    path: '/api/v1/admin/reports/dashboard-overview.csv',
    withTime: false,
    withLimit: false
  },
  {
    key: 'order-trends',
    title: '订单趋势',
    description: '导出每日创建单、完成单、取消单和成交金额。',
    filename: 'order-trends.csv',
    path: '/api/v1/admin/reports/order-trends.csv',
    withTime: true,
    withLimit: false
  },
  {
    key: 'category-sales',
    title: '分类成交排行',
    description: '按分类统计销量、成交单量和成交金额。',
    filename: 'category-sales-ranking.csv',
    path: '/api/v1/admin/reports/category-sales-ranking.csv',
    withTime: true,
    withLimit: true
  },
  {
    key: 'hot-keywords',
    title: '热门搜索词',
    description: '导出高频搜索词及其最常落到的分类。',
    filename: 'hot-search-keywords.csv',
    path: '/api/v1/admin/reports/hot-search-keywords.csv',
    withTime: true,
    withLimit: true
  },
  {
    key: 'user-growth',
    title: '用户增长趋势',
    description: '导出每日新增用户数和累计用户数。',
    filename: 'user-growth-trends.csv',
    path: '/api/v1/admin/reports/user-growth-trends.csv',
    withTime: true,
    withLimit: false
  }
] as const;

const rangeSummary = computed(() => (rangeValue.value.length === 2 ? `${rangeValue.value[0]} 至 ${rangeValue.value[1]}` : '请选择开始和结束日期'));

async function download(report: (typeof reports)[number]) {
  if (report.withTime && !validateTimeMode()) {
    return;
  }
  activeExportKey.value = report.key;
  try {
    await downloadFile(buildUrl(report), authStore.adminToken);
    ElMessage.success('报表已开始下载');
  } catch {
    ElMessage.error('报表导出失败，请稍后重试');
  } finally {
    activeExportKey.value = '';
  }
}

function buildUrl(report: (typeof reports)[number]) {
  if (!report.withTime) return report.path;
  const params = new URLSearchParams();
  if (exportMode.value === 'range') {
    params.set('startDate', rangeValue.value[0]);
    params.set('endDate', rangeValue.value[1]);
  } else {
    params.set('days', String(form.days));
  }
  if (report.withLimit) {
    params.set('limit', String(form.limit));
  }
  return `${report.path}?${params.toString()}`;
}

function validateTimeMode() {
  if (exportMode.value === 'range') {
    if (rangeValue.value.length !== 2) {
      ElMessage.warning('请选择完整的开始和结束日期');
      return false;
    }
    if (rangeValue.value[0] > rangeValue.value[1]) {
      ElMessage.warning('结束日期不能早于开始日期');
      return false;
    }
  }
  return true;
}
</script>

<style scoped>
.panel {
  padding: 24px;
}
.config-shell {
  display: grid;
  grid-template-columns: 1.45fr 0.95fr;
  gap: 18px;
  margin-bottom: 22px;
}
.config-card,
.summary-card,
.report-card {
  padding: 20px;
}
.mode-switch {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.config-label,
.summary-tag,
.report-file {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(25, 63, 58, 0.08);
  color: var(--brand-deep);
  font-size: 12px;
}
.inline-form {
  margin-top: 16px;
}
.preset-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.range-form {
  margin-top: 10px;
}
.summary-card {
  background: linear-gradient(145deg, rgba(247, 242, 235, 0.9), rgba(237, 244, 239, 0.96));
}
.summary-card h3 {
  margin: 14px 0 10px;
  font-size: 28px;
  font-family: var(--font-display);
}
.summary-card p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.8;
}
.report-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}
.report-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
}
.report-card h3 {
  margin: 12px 0 10px;
  font-size: 24px;
}
.report-card p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.8;
}
@media (max-width: 960px) {
  .config-shell,
  .report-grid {
    grid-template-columns: 1fr;
  }
}
</style>