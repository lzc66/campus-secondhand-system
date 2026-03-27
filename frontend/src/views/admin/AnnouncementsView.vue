<template>
  <section class="glass-card panel">
    <SectionHeading title="公告管理" description="集中维护系统公告的草稿、发布时间、置顶状态和过期时间。" tag="Announcements">
      <el-button type="primary" @click="openEditor()">新建公告</el-button>
    </SectionHeading>

    <div class="toolbar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索标题关键词" class="toolbar-item" @keyup.enter="fetchList" />
      <el-select v-model="filters.publishStatus" clearable placeholder="发布状态" class="toolbar-item" @change="handleFilterChange">
        <el-option label="草稿" value="draft" />
        <el-option label="已发布" value="published" />
        <el-option label="已下线" value="offline" />
      </el-select>
      <el-button type="primary" @click="fetchList">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="isLoading" :data="records" stripe>
      <el-table-column prop="title" label="标题" min-width="240" />
      <el-table-column label="状态" width="130">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.publishStatus)">{{ statusLabel(scope.row.publishStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.pinned ? 'warning' : 'info'">{{ scope.row.pinned ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" min-width="170" />
      <el-table-column label="操作" min-width="300" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="viewDetail(scope.row.announcementId)">详情</el-button>
          <el-button link type="primary" @click="openEditor(scope.row)">编辑</el-button>
          <el-button v-if="scope.row.publishStatus !== 'published'" link type="success" @click="publish(scope.row.announcementId)">发布</el-button>
          <el-button v-if="scope.row.publishStatus === 'published'" link type="warning" @click="offline(scope.row.announcementId)">下线</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > size" class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
  </section>

  <el-dialog v-model="editorVisible" :title="currentId ? '编辑公告' : '新建公告'" width="820px" destroy-on-close>
    <el-alert title="保存时会保留草稿或发布状态；若要正式上线，也可以先保存再点击列表中的发布按钮。" type="info" :closable="false" show-icon />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="editor-form">
      <el-form-item label="公告标题" prop="title">
        <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="例如：宿舍配送时间调整通知" />
      </el-form-item>
      <el-form-item label="公告内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="9" maxlength="2000" show-word-limit placeholder="建议写清生效时间、适用范围、处理方式。" />
      </el-form-item>
      <div class="editor-grid">
        <el-form-item label="发布状态" prop="publishStatus">
          <el-select v-model="form.publishStatus">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.pinned" inline-prompt active-text="置顶" inactive-text="普通" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="form.expireAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选，不填则长期有效" />
        </el-form-item>
      </div>
      <div class="preview-card glass-card">
        <span class="preview-tag">实时预览</span>
        <h3>{{ form.title || '公告标题预览' }}</h3>
        <p>{{ form.content || '这里会显示公告内容预览，方便编辑时确认信息层次。' }}</p>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="editorVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="save">保存公告</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="detailVisible" title="公告详情" size="480px">
    <div v-if="detailLoading" class="drawer-skeleton">
      <el-skeleton animated :rows="8" />
    </div>
    <template v-else-if="currentDetail">
      <div class="detail-topline">
        <el-tag :type="statusType(currentDetail.publishStatus)">{{ statusLabel(currentDetail.publishStatus) }}</el-tag>
        <el-tag :type="currentDetail.pinned ? 'warning' : 'info'">{{ currentDetail.pinned ? '置顶' : '普通' }}</el-tag>
      </div>
      <h3 class="detail-title">{{ currentDetail.title }}</h3>
      <p class="detail-meta">发布时间：{{ currentDetail.publishTime || currentDetail.createdAt || '--' }}</p>
      <p class="detail-meta">过期时间：{{ currentDetail.expireAt || '--' }}</p>
      <div class="detail-content">{{ currentDetail.content }}</div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';

const records = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const size = 10;
const isLoading = ref(false);
const editorVisible = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const currentId = ref<number | null>(null);
const currentDetail = ref<any | null>(null);
const submitting = ref(false);
const formRef = ref<FormInstance>();

const filters = reactive({
  keyword: '',
  publishStatus: ''
});

const form = reactive<any>({
  title: '',
  content: '',
  pinned: false,
  publishStatus: 'draft',
  expireAt: ''
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  publishStatus: [{ required: true, message: '请选择发布状态', trigger: 'change' }]
};

fetchList();

async function fetchList() {
  isLoading.value = true;
  try {
    const data = await adminApi.getAnnouncements({
      page: page.value,
      size,
      keyword: filters.keyword || undefined,
      publishStatus: filters.publishStatus || undefined
    });
    records.value = data.records || [];
    total.value = Number(data.total || 0);
  } finally {
    isLoading.value = false;
  }
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  fetchList();
}

function handleFilterChange() {
  page.value = 1;
  fetchList();
}

function resetFilters() {
  filters.keyword = '';
  filters.publishStatus = '';
  page.value = 1;
  fetchList();
}

function openEditor(row?: any) {
  currentId.value = row?.announcementId || null;
  Object.assign(form, row || { title: '', content: '', pinned: false, publishStatus: 'draft', expireAt: '' });
  editorVisible.value = true;
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (currentId.value) await adminApi.updateAnnouncement(currentId.value, { ...form });
    else await adminApi.createAnnouncement({ ...form });
    ElMessage.success('公告已保存');
    editorVisible.value = false;
    await fetchList();
  } finally {
    submitting.value = false;
  }
}

async function viewDetail(id: number) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    currentDetail.value = await adminApi.getAnnouncementDetail(id);
  } finally {
    detailLoading.value = false;
  }
}

async function publish(id: number) {
  await ElMessageBox.confirm('确认发布这条公告吗？发布后用户侧会立即可见。', '发布公告', { type: 'warning' });
  await adminApi.publishAnnouncement(id, { actionNote: '前端发布公告' });
  ElMessage.success('公告已发布');
  await fetchList();
}

async function offline(id: number) {
  await ElMessageBox.confirm('确认下线这条公告吗？下线后前台将不再展示。', '下线公告', { type: 'warning' });
  await adminApi.offlineAnnouncement(id, { actionNote: '前端下线公告' });
  ElMessage.success('公告已下线');
  await fetchList();
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    draft: '草稿',
    published: '已发布',
    offline: '已下线'
  };
  return map[String(status)] || '未知';
}

function statusType(status?: string) {
  if (status === 'published') return 'success';
  if (status === 'offline') return 'info';
  return 'warning';
}
</script>

<style scoped>
.panel {
  padding: 24px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.toolbar-item {
  width: 220px;
}
.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
.editor-form {
  margin-top: 18px;
}
.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
.preview-card {
  margin-top: 6px;
  padding: 18px;
  border: 1px dashed var(--line);
}
.preview-tag {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(35, 78, 119, 0.08);
  color: var(--brand-deep);
  font-size: 12px;
}
.preview-card h3 {
  margin: 0 0 10px;
  font-size: 24px;
}
.preview-card p {
  margin: 0;
  line-height: 1.85;
  color: var(--text-soft);
  white-space: pre-wrap;
}
.drawer-skeleton {
  padding: 4px 2px;
}
.detail-topline {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.detail-title {
  margin: 0 0 14px;
  font-size: 28px;
}
.detail-meta {
  margin: 0 0 8px;
  color: var(--text-soft);
}
.detail-content {
  margin-top: 18px;
  line-height: 1.9;
  white-space: pre-wrap;
}
@media (max-width: 768px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>