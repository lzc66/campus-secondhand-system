<template>
  <section class="glass-card panel">
    <SectionHeading
      title="注册审核"
      description="集中处理学生注册申请，支持筛选、分页和学生证预览。"
      tag="Review"
    />

    <div class="toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable style="width: 160px">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="filters.studentNo" placeholder="输入学号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="filters.email" placeholder="输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refresh">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="toolbar-side">共 {{ total }} 条申请</div>
    </div>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="applicationNo" label="申请编号" min-width="180" />
      <el-table-column prop="studentNo" label="学号" width="150" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" min-width="220" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getRegistrationStatusTagType(row.status)">
            {{ getRegistrationStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.applicationId)">详情</el-button>
          <el-button link type="success" :disabled="row.status !== 'pending'" @click="review(row.applicationId, 'approve')">
            通过
          </el-button>
          <el-button link type="danger" :disabled="row.status !== 'pending'" @click="review(row.applicationId, 'reject')">
            驳回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
  </section>

  <el-drawer v-model="detailVisible" title="申请详情" size="860px" class="registration-drawer">
    <div v-if="currentDetail" class="detail-shell">
      <section class="glass-card detail-card">
        <div class="detail-head">
          <div>
            <p class="eyebrow">申请概览</p>
            <h3>{{ currentDetail.realName }} · {{ currentDetail.studentNo }}</h3>
            <p class="detail-subtitle">
              提交于 {{ currentDetail.submittedAt || '--' }}
              <span v-if="currentDetail.reviewedAt"> · 审核于 {{ currentDetail.reviewedAt }}</span>
            </p>
          </div>
          <el-tag size="large" :type="getRegistrationStatusTagType(currentDetail.status)">
            {{ getRegistrationStatusLabel(currentDetail.status) }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ currentDetail.applicationNo || '--' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ getGenderLabel(currentDetail.gender) }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentDetail.email || '--' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ currentDetail.phone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ currentDetail.collegeName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ currentDetail.majorName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ currentDetail.className || '--' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">
            {{ currentDetail.reviewerAdminId ? `管理员 #${currentDetail.reviewerAdminId}` : '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">
            {{ currentDetail.reviewRemark || '暂无审核备注' }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="glass-card preview-card">
        <div class="preview-head">
          <div>
            <p class="eyebrow">学生证照片</p>
            <h3>{{ currentDetail.studentCardFile?.originalName || '未上传' }}</h3>
          </div>
          <el-link
            v-if="studentCardUrl"
            :href="studentCardUrl"
            target="_blank"
          >
            查看原图
          </el-link>
        </div>

        <div v-if="studentCardUrl" class="image-frame">
          <el-image
            :src="studentCardUrl"
            fit="contain"
            :preview-src-list="[studentCardUrl]"
            preview-teleported
          />
        </div>
        <EmptyState
          v-else
          title="暂无学生证照片"
          description="该申请没有关联到可预览的学生证图片。"
          compact
        />

        <div class="file-meta" v-if="currentDetail.studentCardFile">
          <span>文件类型：{{ currentDetail.studentCardFile.mimeType || '--' }}</span>
          <span>文件大小：{{ formatFileSize(currentDetail.studentCardFile.fileSize) }}</span>
        </div>
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { adminApi } from '@/api/admin';
import {
  getGenderLabel,
  getRegistrationStatusLabel,
  getRegistrationStatusTagType
} from '@/utils/status';

const filters = reactive({
  status: '',
  studentNo: '',
  email: ''
});

const records = ref<any[]>([]);
const detailVisible = ref(false);
const currentDetail = ref<any | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const total = ref(0);

const studentCardUrl = computed(() => currentDetail.value?.studentCardFile?.fileUrl || '');

onMounted(refresh);

async function fetchList() {
  loading.value = true;
  try {
    const data = await adminApi.getRegistrations({
      page: page.value,
      size: size.value,
      ...filters
    });
    records.value = data.records || [];
    total.value = Number(data.total || 0);
  } finally {
    loading.value = false;
  }
}

function refresh() {
  page.value = 1;
  fetchList();
}

function resetFilters() {
  filters.status = '';
  filters.studentNo = '';
  filters.email = '';
  refresh();
}

async function showDetail(id: number) {
  currentDetail.value = await adminApi.getRegistrationDetail(id);
  detailVisible.value = true;
}

async function review(id: number, type: 'approve' | 'reject') {
  const { value } = await ElMessageBox.prompt(
    '请填写审核备注',
    type === 'approve' ? '通过申请' : '驳回申请',
    {
      inputPlaceholder: '可选填写审核意见'
    }
  );

  if (type === 'approve') {
    await adminApi.approveRegistration(id, { reviewRemark: value });
  } else {
    await adminApi.rejectRegistration(id, { reviewRemark: value });
  }

  ElMessage.success('审核完成');
  await fetchList();
  if (currentDetail.value?.applicationId === id) {
    await showDetail(id);
  }
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  fetchList();
}

function formatFileSize(sizeValue?: number) {
  if (!sizeValue) return '--';
  if (sizeValue < 1024) return `${sizeValue} B`;
  if (sizeValue < 1024 * 1024) return `${(sizeValue / 1024).toFixed(1)} KB`;
  return `${(sizeValue / 1024 / 1024).toFixed(2)} MB`;
}
</script>

<style scoped>
.panel {
  padding: 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-side {
  color: var(--text-soft);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.detail-card,
.preview-card {
  padding: 20px;
}

.detail-head,
.preview-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--text-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.detail-head h3,
.preview-head h3 {
  margin: 0;
  font-size: 24px;
}

.detail-subtitle {
  margin: 8px 0 0;
  color: var(--text-soft);
}

.image-frame {
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(35, 78, 119, 0.08), rgba(220, 127, 57, 0.08));
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-frame :deep(.el-image) {
  width: 100%;
  min-height: 360px;
}

.file-meta {
  margin-top: 14px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: var(--text-soft);
  font-size: 13px;
}

@media (max-width: 960px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-shell {
    grid-template-columns: 1fr;
  }
}
</style>
