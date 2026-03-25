<template>
  <section class="glass-card panel">
    <SectionHeading title="订单管理" description="支持订单状态筛选、分页和详情抽屉。" tag="Orders" />
    <div class="toolbar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="订单状态"><el-select v-model="filters.orderStatus" clearable style="width: 180px"><el-option label="待确认" value="pending_confirm" /><el-option label="待配送" value="confirmed" /><el-option label="配送中" value="delivering" /><el-option label="已完成" value="completed" /><el-option label="已取消" value="cancelled" /><el-option label="已关闭" value="closed" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="refresh">筛选</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
      </el-form>
      <div class="toolbar-side">共 {{ total }} 笔订单</div>
    </div>
    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="buyerStudentNo" label="买家学号" width="140" />
      <el-table-column prop="sellerStudentNo" label="卖家学号" width="140" />
      <el-table-column prop="orderStatus" label="状态" width="140"><template #default="scope"><el-tag :type="statusTagType(scope.row.orderStatus)">{{ scope.row.orderStatus }}</el-tag></template></el-table-column>
      <el-table-column prop="orderAmount" label="金额" width="120" />
      <el-table-column label="操作" width="240" fixed="right"><template #default="scope"><el-button link type="primary" @click="showDetail(scope.row.orderId)">详情</el-button><el-button link type="warning" @click="action(scope.row.orderId, 'cancel')">取消</el-button><el-button link type="danger" @click="action(scope.row.orderId, 'close')">关闭</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination layout="prev, pager, next, total" :current-page="page" :page-size="size" :total="total" @current-change="handlePageChange" /></div>
  </section>
  <el-drawer v-model="detailVisible" title="订单详情" size="620px"><el-descriptions v-if="currentDetail" :column="1" border><el-descriptions-item label="订单号">{{ currentDetail.orderNo || '--' }}</el-descriptions-item><el-descriptions-item label="买家学号">{{ currentDetail.buyerStudentNo || '--' }}</el-descriptions-item><el-descriptions-item label="卖家学号">{{ currentDetail.sellerStudentNo || '--' }}</el-descriptions-item><el-descriptions-item label="状态">{{ currentDetail.orderStatus || '--' }}</el-descriptions-item><el-descriptions-item label="金额">{{ currentDetail.orderAmount || '--' }}</el-descriptions-item><el-descriptions-item label="收货人">{{ currentDetail.receiverName || '--' }}</el-descriptions-item><el-descriptions-item label="联系电话">{{ currentDetail.receiverPhone || '--' }}</el-descriptions-item><el-descriptions-item label="配送地址">{{ currentDetail.deliveryAddress || '--' }}</el-descriptions-item><el-descriptions-item label="备注">{{ currentDetail.buyerRemark || '--' }}</el-descriptions-item></el-descriptions></el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';
const filters = reactive({ orderStatus: '' }); const records = ref<any[]>([]); const currentDetail = ref<any>(null); const detailVisible = ref(false); const loading = ref(false); const page = ref(1); const size = ref(10); const total = ref(0);
onMounted(refresh);
async function fetchList() { loading.value = true; try { const data = await adminApi.getOrders({ page: page.value, size: size.value, ...filters }); records.value = data.records || []; total.value = Number(data.total || 0); } finally { loading.value = false; } }
function refresh() { page.value = 1; fetchList(); }
function resetFilters() { filters.orderStatus = ''; refresh(); }
async function showDetail(orderId: number) { currentDetail.value = await adminApi.getOrderDetail(orderId); detailVisible.value = true; }
async function action(orderId: number, type: 'cancel' | 'close') { const { value } = await ElMessageBox.prompt('填写操作备注', '订单处理'); if (type === 'cancel') await adminApi.cancelOrder(orderId, { actionNote: value }); else await adminApi.closeOrder(orderId, { actionNote: value }); ElMessage.success('订单已处理'); fetchList(); }
function handlePageChange(nextPage: number) { page.value = nextPage; fetchList(); }
function statusTagType(status: string) { if (status === 'completed') return 'success'; if (status === 'pending_confirm' || status === 'confirmed') return 'warning'; if (status === 'delivering') return 'primary'; if (status === 'closed' || status === 'cancelled') return 'info'; return 'danger'; }
</script>

<style scoped>
.panel { padding: 24px; }
.toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.toolbar-side { color: var(--text-soft); }
.pager { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 960px) { .toolbar { flex-direction: column; align-items: stretch; } }
</style>