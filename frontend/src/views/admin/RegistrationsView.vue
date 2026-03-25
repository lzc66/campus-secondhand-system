<template>
  <section class="glass-card panel">
    <SectionHeading title="注册审核" description="查看待审核申请，支持筛选、分页和详情抽屉。" tag="Review" />
    <div class="toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="状态"><el-select v-model="filters.status" clearable style="width: 160px"><el-option label="待审核" value="pending" /><el-option label="已通过" value="approved" /><el-option label="已驳回" value="rejected" /></el-select></el-form-item>
        <el-form-item label="学号"><el-input v-model="filters.studentNo" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="filters.email" /></el-form-item>
        <el-form-item><el-button type="primary" @click="refresh">筛选</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
      </el-form>
      <div class="toolbar-side">共 {{ total }} 条申请</div>
    </div>
    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="applicationNo" label="申请编号" min-width="160" />
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="status" label="状态" width="120"><template #default="scope"><el-tag :type="statusTagType(scope.row.status)">{{ scope.row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right"><template #default="scope"><el-button link type="primary" @click="showDetail(scope.row.applicationId)">详情</el-button><el-button link type="success" :disabled="scope.row.status !== 'pending'" @click="review(scope.row.applicationId, 'approve')">通过</el-button><el-button link type="danger" :disabled="scope.row.status !== 'pending'" @click="review(scope.row.applicationId, 'reject')">驳回</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination layout="prev, pager, next, total" :current-page="page" :page-size="size" :total="total" @current-change="handlePageChange" /></div>
  </section>
  <el-drawer v-model="detailVisible" title="申请详情" size="560px">
    <el-descriptions v-if="currentDetail" :column="1" border>
      <el-descriptions-item label="申请编号">{{ currentDetail.applicationNo || '--' }}</el-descriptions-item>
      <el-descriptions-item label="学号">{{ currentDetail.studentNo || '--' }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ currentDetail.realName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ currentDetail.email || '--' }}</el-descriptions-item>
      <el-descriptions-item label="手机">{{ currentDetail.phone || '--' }}</el-descriptions-item>
      <el-descriptions-item label="学院">{{ currentDetail.collegeName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="专业">{{ currentDetail.majorName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="班级">{{ currentDetail.className || '--' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentDetail.status || '--' }}</el-descriptions-item>
      <el-descriptions-item label="学生证"><a v-if="currentDetail.studentCardImageUrl" :href="currentDetail.studentCardImageUrl" target="_blank">查看图片</a><span v-else>--</span></el-descriptions-item>
      <el-descriptions-item label="审核备注">{{ currentDetail.reviewRemark || '--' }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';
const filters = reactive({ status: '', studentNo: '', email: '' }); const records = ref<any[]>([]); const detailVisible = ref(false); const currentDetail = ref<any>(null); const loading = ref(false); const page = ref(1); const size = ref(10); const total = ref(0);
onMounted(refresh);
async function fetchList() { loading.value = true; try { const data = await adminApi.getRegistrations({ page: page.value, size: size.value, ...filters }); records.value = data.records || []; total.value = Number(data.total || 0); } finally { loading.value = false; } }
function refresh() { page.value = 1; fetchList(); }
function resetFilters() { filters.status = ''; filters.studentNo = ''; filters.email = ''; refresh(); }
async function showDetail(id: number) { currentDetail.value = await adminApi.getRegistrationDetail(id); detailVisible.value = true; }
async function review(id: number, type: 'approve' | 'reject') { const { value } = await ElMessageBox.prompt('填写审核备注', type === 'approve' ? '通过申请' : '驳回申请'); if (type === 'approve') await adminApi.approveRegistration(id, { reviewRemark: value }); else await adminApi.rejectRegistration(id, { reviewRemark: value }); ElMessage.success('审核完成'); fetchList(); }
function handlePageChange(nextPage: number) { page.value = nextPage; fetchList(); }
function statusTagType(status: string) { if (status === 'approved') return 'success'; if (status === 'rejected') return 'danger'; return 'warning'; }
</script>

<style scoped>
.panel { padding: 24px; }
.toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.toolbar-side { color: var(--text-soft); }
.pager { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 960px) { .toolbar { flex-direction: column; align-items: stretch; } }
</style>