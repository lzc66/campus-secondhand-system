<template>
  <section class="glass-card panel">
    <SectionHeading
      title="用户管理"
      description="支持按账号状态筛选、查看资料详情，并快速调整用户账号状态。"
      tag="Users"
    />

    <div class="toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="账号状态">
          <el-select v-model="filters.accountStatus" clearable style="width: 160px">
            <el-option label="正常" value="active" />
            <el-option label="已锁定" value="locked" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="filters.studentNo" placeholder="输入学号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refresh">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="toolbar-side">共 {{ total }} 位用户</div>
    </div>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="studentNo" label="学号" width="150" />
      <el-table-column prop="realName" label="姓名" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="220" />
      <el-table-column prop="phone" label="手机" width="140" />
      <el-table-column label="账号状态" width="140">
        <template #default="{ row }">
          <el-tag :type="getUserAccountStatusTagType(row.accountStatus)">
            {{ getUserAccountStatusLabel(row.accountStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.userId)">详情</el-button>
          <el-button link @click="changeStatus(row.userId, 'active')">启用</el-button>
          <el-button link type="warning" @click="changeStatus(row.userId, 'locked')">锁定</el-button>
          <el-button link type="danger" @click="changeStatus(row.userId, 'disabled')">禁用</el-button>
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

  <el-drawer v-model="detailVisible" title="用户详情" size="560px">
    <el-descriptions v-if="currentDetail" :column="1" border>
      <el-descriptions-item label="学号">{{ currentDetail.studentNo || '--' }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ currentDetail.realName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ currentDetail.email || '--' }}</el-descriptions-item>
      <el-descriptions-item label="手机">{{ currentDetail.phone || '--' }}</el-descriptions-item>
      <el-descriptions-item label="学院">{{ currentDetail.collegeName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="专业">{{ currentDetail.majorName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="班级">{{ currentDetail.className || '--' }}</el-descriptions-item>
      <el-descriptions-item label="宿舍">{{ currentDetail.dormitoryAddress || '--' }}</el-descriptions-item>
      <el-descriptions-item label="账号状态">
        {{ getUserAccountStatusLabel(currentDetail.accountStatus) }}
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';
import { getUserAccountStatusLabel, getUserAccountStatusTagType } from '@/utils/status';

const filters = reactive({
  accountStatus: '',
  studentNo: ''
});

const records = ref<any[]>([]);
const currentDetail = ref<any | null>(null);
const detailVisible = ref(false);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const total = ref(0);

onMounted(refresh);

async function fetchList() {
  loading.value = true;
  try {
    const data = await adminApi.getUsers({
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
  filters.accountStatus = '';
  filters.studentNo = '';
  refresh();
}

async function showDetail(userId: number) {
  currentDetail.value = await adminApi.getUserDetail(userId);
  detailVisible.value = true;
}

async function changeStatus(userId: number, accountStatus: string) {
  const { value } = await ElMessageBox.prompt('请填写操作备注', '更新用户状态', {
    inputPlaceholder: '例如：异常登录锁定、毕业停用'
  });
  await adminApi.updateUserStatus(userId, { accountStatus, actionNote: value });
  ElMessage.success('用户状态已更新');
  await fetchList();
  if (currentDetail.value?.userId === userId) {
    await showDetail(userId);
  }
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  fetchList();
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

@media (max-width: 960px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
