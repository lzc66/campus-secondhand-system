<template>
  <section class="glass-card panel">
    <SectionHeading title="商品管理" description="支持状态筛选、卖家学号检索和详情抽屉。" tag="Items" />
    <div class="toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="商品状态"><el-select v-model="filters.status" clearable style="width: 160px"><el-option label="在售" value="on_sale" /><el-option label="下架" value="off_shelf" /><el-option label="售出" value="sold" /><el-option label="删除" value="deleted" /></el-select></el-form-item>
        <el-form-item label="卖家学号"><el-input v-model="filters.sellerStudentNo" /></el-form-item>
        <el-form-item><el-button type="primary" @click="refresh">筛选</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
      </el-form>
      <div class="toolbar-side">共 {{ total }} 件商品</div>
    </div>
    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="title" label="商品" min-width="220" />
      <el-table-column prop="sellerStudentNo" label="卖家学号" width="160" />
      <el-table-column prop="status" label="状态" width="140"><template #default="scope"><el-tag :type="statusTagType(scope.row.status)">{{ scope.row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column label="操作" width="280" fixed="right"><template #default="scope"><el-button link type="primary" @click="showDetail(scope.row.itemId)">详情</el-button><el-button link @click="changeStatus(scope.row.itemId, 'on_sale')">上架</el-button><el-button link type="warning" @click="changeStatus(scope.row.itemId, 'off_shelf')">下架</el-button><el-button link type="danger" @click="changeStatus(scope.row.itemId, 'deleted')">删除</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination layout="prev, pager, next, total" :current-page="page" :page-size="size" :total="total" @current-change="handlePageChange" /></div>
  </section>
  <el-drawer v-model="detailVisible" title="商品详情" size="620px"><template v-if="currentDetail"><div class="detail-cover" v-if="currentDetail.images?.length"><img :src="currentDetail.images[0].imageUrl" :alt="currentDetail.title" /></div><el-descriptions :column="1" border><el-descriptions-item label="标题">{{ currentDetail.title || '--' }}</el-descriptions-item><el-descriptions-item label="分类">{{ currentDetail.categoryName || '--' }}</el-descriptions-item><el-descriptions-item label="卖家">{{ currentDetail.seller?.realName || '--' }}</el-descriptions-item><el-descriptions-item label="价格">{{ currentDetail.price || '--' }}</el-descriptions-item><el-descriptions-item label="库存">{{ currentDetail.stock || '--' }}</el-descriptions-item><el-descriptions-item label="状态">{{ currentDetail.status || '--' }}</el-descriptions-item><el-descriptions-item label="描述">{{ currentDetail.description || '--' }}</el-descriptions-item></el-descriptions></template></el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';
const filters = reactive({ status: '', sellerStudentNo: '' }); const records = ref<any[]>([]); const currentDetail = ref<any>(null); const detailVisible = ref(false); const loading = ref(false); const page = ref(1); const size = ref(10); const total = ref(0);
onMounted(refresh);
async function fetchList() { loading.value = true; try { const data = await adminApi.getItems({ page: page.value, size: size.value, ...filters }); records.value = data.records || []; total.value = Number(data.total || 0); } finally { loading.value = false; } }
function refresh() { page.value = 1; fetchList(); }
function resetFilters() { filters.status = ''; filters.sellerStudentNo = ''; refresh(); }
async function showDetail(itemId: number) { currentDetail.value = await adminApi.getItemDetail(itemId); detailVisible.value = true; }
async function changeStatus(itemId: number, itemStatus: string) { const { value } = await ElMessageBox.prompt('填写操作备注', '更新商品状态'); await adminApi.updateItemStatus(itemId, { itemStatus, actionNote: value }); ElMessage.success('商品状态已更新'); fetchList(); }
function handlePageChange(nextPage: number) { page.value = nextPage; fetchList(); }
function statusTagType(status: string) { if (status === 'on_sale') return 'success'; if (status === 'off_shelf') return 'warning'; if (status === 'sold') return 'info'; return 'danger'; }
</script>

<style scoped>
.panel { padding: 24px; }
.toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.toolbar-side { color: var(--text-soft); }
.pager { display: flex; justify-content: center; margin-top: 20px; }
.detail-cover { margin-bottom: 16px; border-radius: 20px; overflow: hidden; }
.detail-cover img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; }
@media (max-width: 960px) { .toolbar { flex-direction: column; align-items: stretch; } }
</style>