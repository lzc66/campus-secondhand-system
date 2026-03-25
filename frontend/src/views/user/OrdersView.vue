<template>
  <section class="glass-card panel">
    <SectionHeading title="我的订单" description="在买家和卖家视角之间切换，继续跟进接单、配送和完成状态。" tag="Orders" />

    <div class="toolbar">
      <el-radio-group v-model="role" @change="handleRoleChange">
        <el-radio-button value="buyer">我是买家</el-radio-button>
        <el-radio-button value="seller">我是卖家</el-radio-button>
      </el-radio-group>
      <div class="summary-strip">
        <div class="summary-chip"><strong>{{ records.length }}</strong><span>当前页订单</span></div>
        <div class="summary-chip"><strong>{{ pendingCount }}</strong><span>待处理</span></div>
        <div class="summary-chip"><strong>{{ completedCount }}</strong><span>已完成</span></div>
      </div>
    </div>

    <div v-if="isLoading" class="skeleton-stack">
      <div v-for="index in 3" :key="index" class="glass-card skeleton-row">
        <el-skeleton animated :rows="4" />
      </div>
    </div>

    <template v-else-if="records.length">
      <el-table :data="records" stripe class="order-table">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="itemTitle" label="商品" min-width="220" />
        <el-table-column label="状态" width="130">
          <template #default="scope">
            <el-tag :type="orderStatusType(scope.row.orderStatus)">{{ orderStatusLabel(scope.row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130">
          <template #default="scope">{{ formatPrice(scope.row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="300" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewDetail(scope.row.orderId)">详情</el-button>
            <el-button v-if="canConfirm(scope.row)" link type="primary" @click="action(scope.row.orderId, 'confirm')">接单</el-button>
            <el-button v-if="canDeliver(scope.row)" link type="warning" @click="action(scope.row.orderId, 'deliver')">配送中</el-button>
            <el-button v-if="canComplete(scope.row)" link type="success" @click="action(scope.row.orderId, 'complete')">确认完成</el-button>
            <el-button v-if="canCancel(scope.row)" link type="danger" @click="action(scope.row.orderId, 'cancel')">取消</el-button>
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
    </template>

    <EmptyState
      v-else
      title="还没有订单记录"
      :description="role === 'buyer' ? '你还没有作为买家创建订单，可以先去商品广场逛逛。' : '暂时没有买家向你的商品下单。'"
    >
      <el-button v-if="role === 'buyer'" type="primary" @click="router.push('/items')">去看看商品</el-button>
    </EmptyState>
  </section>

  <el-drawer v-model="drawerVisible" size="460px" title="订单详情">
    <div v-if="detailLoading" class="drawer-skeleton">
      <el-skeleton animated :rows="8" />
    </div>
    <el-descriptions v-else-if="currentOrder" :column="1" border>
      <el-descriptions-item label="订单号">{{ currentOrder.orderNo || '--' }}</el-descriptions-item>
      <el-descriptions-item label="商品">{{ currentOrder.itemTitle || '--' }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">{{ orderStatusLabel(currentOrder.orderStatus) }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">{{ formatPrice(currentOrder.orderAmount) }}</el-descriptions-item>
      <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ currentOrder.receiverPhone || '--' }}</el-descriptions-item>
      <el-descriptions-item label="配送方式">{{ currentOrder.deliveryType || '--' }}</el-descriptions-item>
      <el-descriptions-item label="配送地址">{{ currentOrder.deliveryAddress || '--' }}</el-descriptions-item>
      <el-descriptions-item label="买家备注">{{ currentOrder.buyerRemark || '--' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createdAt) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { formatDateTime, formatPrice, labelize } from '@/utils/format';

const router = useRouter();
const role = ref('buyer');
const records = ref<any[]>([]);
const isLoading = ref(false);
const page = ref(1);
const size = 10;
const total = ref(0);
const drawerVisible = ref(false);
const detailLoading = ref(false);
const currentOrder = ref<any | null>(null);

const pendingCount = computed(() => records.value.filter((item) => ['pending', 'confirmed', 'delivering'].includes(String(item.orderStatus))).length);
const completedCount = computed(() => records.value.filter((item) => String(item.orderStatus) === 'completed').length);

onMounted(fetchOrders);

async function fetchOrders() {
  isLoading.value = true;
  try {
    const data = await userApi.getOrders({ role: role.value, page: page.value, size });
    records.value = data.records || [];
    total.value = Number(data.total || 0);
  } finally {
    isLoading.value = false;
  }
}

function handleRoleChange() {
  page.value = 1;
  fetchOrders();
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  fetchOrders();
}

async function viewDetail(orderId: number) {
  drawerVisible.value = true;
  detailLoading.value = true;
  try {
    currentOrder.value = await userApi.getOrderDetail(orderId);
  } finally {
    detailLoading.value = false;
  }
}

async function action(orderId: number, type: 'confirm' | 'deliver' | 'complete' | 'cancel') {
  const note = type === 'cancel' ? '用户主动取消订单' : '前端页面操作';
  if (type === 'cancel') {
    await ElMessageBox.confirm('确认取消这笔订单吗？', '取消订单', { type: 'warning' });
  }
  if (type === 'confirm') await userApi.confirmOrder(orderId, { actionNote: note });
  if (type === 'deliver') await userApi.deliverOrder(orderId, { actionNote: note });
  if (type === 'complete') await userApi.completeOrder(orderId, { actionNote: note });
  if (type === 'cancel') await userApi.cancelOrder(orderId, { actionNote: note, cancelReason: note });
  ElMessage.success('订单状态已更新');
  await fetchOrders();
  if (drawerVisible.value && currentOrder.value?.orderId === orderId) {
    await viewDetail(orderId);
  }
}

function canConfirm(row: any) {
  return role.value === 'seller' && String(row.orderStatus) === 'pending';
}

function canDeliver(row: any) {
  return role.value === 'seller' && ['confirmed', 'accepted'].includes(String(row.orderStatus));
}

function canComplete(row: any) {
  return role.value === 'buyer' && ['delivering', 'shipped'].includes(String(row.orderStatus));
}

function canCancel(row: any) {
  return !['completed', 'cancelled', 'closed'].includes(String(row.orderStatus));
}

function orderStatusLabel(status?: string) {
  const map: Record<string, string> = {
    pending: '待接单',
    accepted: '已接单',
    confirmed: '已确认',
    delivering: '配送中',
    shipped: '配送中',
    completed: '已完成',
    cancelled: '已取消',
    closed: '已关闭'
  };
  return map[String(status)] || labelize(status, '--');
}

function orderStatusType(status?: string) {
  const value = String(status);
  if (value === 'completed') return 'success';
  if (['cancelled', 'closed'].includes(value)) return 'info';
  if (value === 'delivering') return 'warning';
  return 'primary';
}
</script>

<style scoped>
.panel {
  padding: 24px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.summary-strip {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.summary-chip {
  min-width: 116px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(25, 63, 58, 0.06);
}
.summary-chip strong {
  display: block;
  font-size: 22px;
  color: var(--brand);
  font-family: var(--font-display);
}
.summary-chip span {
  color: var(--text-soft);
  font-size: 13px;
}
.skeleton-stack {
  display: grid;
  gap: 14px;
}
.skeleton-row {
  padding: 18px;
}
.order-table {
  margin-top: 6px;
}
.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
.drawer-skeleton {
  padding: 4px 2px;
}
@media (max-width: 768px) {
  .panel {
    padding: 18px;
  }
}
</style>