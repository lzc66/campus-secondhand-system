<template>
  <section class="glass-card panel">
    <SectionHeading title="报表导出" description="支持默认天数导出，也支持按日期范围自定义导出。" tag="Reports" />
    <el-form :model="form" label-width="100px" class="report-form">
      <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="默认天数"><el-input-number v-model="form.days" :min="1" :max="90" /></el-form-item>
      <el-form-item label="Top N"><el-input-number v-model="form.limit" :min="1" :max="50" /></el-form-item>
    </el-form>
    <div class="report-grid">
      <button class="report-card glass-card" @click="download('/api/v1/admin/reports/dashboard-overview.csv')"><strong>总览报表</strong><span>overview.csv</span></button>
      <button class="report-card glass-card" @click="download(buildTimeUrl('/api/v1/admin/reports/order-trends.csv'))"><strong>订单趋势</strong><span>order-trends.csv</span></button>
      <button class="report-card glass-card" @click="download(buildTimeUrl('/api/v1/admin/reports/category-sales-ranking.csv', true))"><strong>分类成交排行</strong><span>category-sales-ranking.csv</span></button>
      <button class="report-card glass-card" @click="download(buildTimeUrl('/api/v1/admin/reports/hot-search-keywords.csv', true))"><strong>热门搜索词</strong><span>hot-search-keywords.csv</span></button>
      <button class="report-card glass-card" @click="download(buildTimeUrl('/api/v1/admin/reports/user-growth-trends.csv'))"><strong>用户增长趋势</strong><span>user-growth-trends.csv</span></button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { useAuthStore } from '@/stores/auth';
import { downloadFile } from '@/utils/download';
const authStore = useAuthStore();
const form = reactive({ startDate: '', endDate: '', days: 7, limit: 20 });
function buildTimeUrl(path: string, withLimit = false) { const params = new URLSearchParams(); if (form.startDate && form.endDate) { params.set('startDate', form.startDate); params.set('endDate', form.endDate); } else { params.set('days', String(form.days)); } if (withLimit) params.set('limit', String(form.limit)); return `${path}?${params.toString()}`; }
function download(url: string) { downloadFile(url, authStore.adminToken); }
</script>

<style scoped>
.panel { padding: 24px; }
.report-form { max-width: 760px; }
.report-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18px; }
.report-card { padding: 24px; border: 1px solid var(--line); text-align: left; cursor: pointer; }
.report-card strong { display: block; font-size: 22px; margin-bottom: 10px; }
.report-card span { color: var(--text-soft); }
@media (max-width: 900px) { .report-grid { grid-template-columns: 1fr; } }
</style>