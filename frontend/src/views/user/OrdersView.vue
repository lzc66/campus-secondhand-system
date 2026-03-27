<template>
  <section class="glass-card panel">
    <SectionHeading
      title="我的订单"
      description="可在买家和卖家视角之间切换，跟进确认、配送和完成流程。"
      tag="Orders"
    />

    <div class="toolbar">
      <el-radio-group v-model="role" @change="handleRoleChange">
        <el-radio-button value="buyer">我是买家</el-radio-button>
        <el-radio-button value="seller">我是卖家</el-radio-button>
      </el-radio-group>

      <div class="summary-strip">
        <div class="summary-chip">
          <strong>{{ records.length }}</strong>
          <span>当前页订单</span>
        </div>
        <div class="summary-chip">
          <strong>{{ pendingCount }}</strong>
          <span>处理中</span>
        </div>
        <div class="summary-chip">
          <strong>{{ completedCount }}</strong>
          <span>已完成</span>
        </div>
      </div>
    </div>

    <el-alert
      v-if="demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled"
      type="info"
      :closable="false"
      show-icon
      title="演示订单会附带说明标签，方便在答辩时讲解订单生命周期。"
      class="demo-alert"
    />

    <div v-if="isLoading" class="skeleton-stack">
      <div v-for="index in 3" :key="index" class="glass-card skeleton-row">
        <el-skeleton animated :rows="4" />
      </div>
    </div>

    <template v-else-if="records.length">
      <el-table :data="records" stripe class="order-table">
        <el-table-column prop="orderNo" label="订单号" min-width="190">
          <template #default="{ row }">
            <div class="order-no-cell">
              <span>{{ row.orderNo }}</span>
              <el-tag v-if="showDemoTag(row)" size="small" effect="plain" type="warning">演示</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="itemTitle" label="商品" min-width="220" />
        <el-table-column label="订单状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusTagType(row.orderStatus)">
              {{ getOrderStatusLabel(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130">
          <template #default="{ row }">{{ formatPrice(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.orderId)">详情</el-button>
            <el-button v-if="canConfirm(row)" link type="primary" @click="action(row.orderId, 'confirm')">确认接单</el-button>
            <el-button v-if="canDeliver(row)" link type="warning" @click="action(row.orderId, 'deliver')">标记配送中</el-button>
            <el-button v-if="canComplete(row)" link type="success" @click="action(row.orderId, 'complete')">确认完成</el-button>
            <el-button v-if="canCancel(row)" link type="danger" @click="action(row.orderId, 'cancel')">取消订单</el-button>
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
      title="还没有订单"
      :description="role === 'buyer' ? '你还没有下单记录，先去逛逛商品列表吧。' : '暂时还没有买家下单，你发布的商品成交后会显示在这里。'"
    >
      <el-button v-if="role === 'buyer'" type="primary" @click="router.push('/items')">去浏览商品</el-button>
    </EmptyState>
  </section>

  <el-drawer v-model="drawerVisible" size="460px" title="订单详情">
    <div v-if="detailLoading" class="drawer-skeleton">
      <el-skeleton animated :rows="8" />
    </div>
    <template v-else-if="currentOrder">
      <el-alert
        v-if="showDemoTag(currentOrder) && demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled"
        type="warning"
        :closable="false"
        show-icon
        title="这是一条演示订单，适合用于展示待确认、待配送、配送中和已完成等状态。"
        class="detail-alert"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ currentOrder.items?.[0]?.itemTitleSnapshot || '--' }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          {{ getOrderStatusLabel(currentOrder.orderStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatPrice(currentOrder.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="配送方式">
          {{ getDeliveryTypeLabel(currentOrder.deliveryType) }}
        </el-descriptions-item>
        <el-descriptions-item label="配送地址">{{ currentOrder.deliveryAddress || '--' }}</el-descriptions-item>
        <el-descriptions-item label="买家备注">{{ currentOrder.buyerRemark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import EmptyState from '@/components/common/EmptyState.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import { userApi } from '@/api/user';
import { formatDateTime, formatPrice } from '@/utils/format';
import { getDeliveryTypeLabel, getOrderStatusLabel, getOrderStatusTagType } from '@/utils/status';

const router = useRouter();
const role = ref<'buyer' | 'seller'>('buyer');
const records = ref<any[]>([]);
const isLoading = ref(false);
const page = ref(1);
const size = 10;
const total = ref(0);
const drawerVisible = ref(false);
const detailLoading = ref(false);
const currentOrder = ref<any | null>(null);
const demoStatus = ref<any>({ demoModeEnabled: false, demoItemNotesEnabled: true });

const pendingStatuses = ['pending_confirm', 'awaiting_delivery', 'delivering'];
const pendingCount = computed(() => records.value.filter((item) => pendingStatuses.includes(String(item.orderStatus))).length);
const completedCount = computed(() => records.value.filter((item) => String(item.orderStatus) === 'completed').length);

onMounted(() => {
  initialize();
});

async function initialize() {
  try {
    await Promise.all([fetchOrders(), fetchDemoStatus()]);
  } catch {
    // errors are surfaced inside fetchers
  }
}

async function fetchDemoStatus() {
  try {
    demoStatus.value = await publicApi.getDemoModeStatus();
  } catch {
    demoStatus.value = { demoModeEnabled: false, demoItemNotesEnabled: true };
  }
}

async function fetchOrders() {
  isLoading.value = true;
  try {
    const data = await userApi.getOrders({ role: role.value, page: page.value, size });
    records.value = data.records || [];
    total.value = Number(data.total || 0);
  } catch (error: any) {
    records.value = [];
    total.value = 0;
    ElMessage.error(error?.response?.data?.message || '订单列表加载失败，请刷新后重试');
    throw error;
  } finally {
    isLoading.value = false;
  }
}

async function handleRoleChange() {
  page.value = 1;
  try {
    await fetchOrders();
  } catch {
    // handled in fetchOrders
  }
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage;
  try {
    await fetchOrders();
  } catch {
    // handled in fetchOrders
  }
}

async function viewDetail(orderId: number) {
  drawerVisible.value = true;
  detailLoading.value = true;
  try {
    currentOrder.value = await userApi.getOrderDetail(orderId);
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '订单详情加载失败');
    drawerVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

async function action(orderId: number, type: 'confirm' | 'deliver' | 'complete' | 'cancel') {
  const noteMap: Record<typeof type, string> = {
    confirm: '前台卖家确认接单',
    deliver: '前台卖家标记配送中',
    complete: '前台买家确认完成',
    cancel: '前台用户取消订单'
  };

  if (type === 'cancel') {
    await ElMessageBox.confirm('确认取消这笔订单吗？', '取消订单', { type: 'warning' });
  }

  if (type === 'confirm') await userApi.confirmOrder(orderId, { actionNote: noteMap.confirm });
  if (type === 'deliver') await userApi.deliverOrder(orderId, { actionNote: noteMap.deliver });
  if (type === 'complete') await userApi.completeOrder(orderId, { actionNote: noteMap.complete });
  if (type === 'cancel') {
    await userApi.cancelOrder(orderId, {
      actionNote: noteMap.cancel,
      cancelReason: '用户主动取消订单'
    });
  }

  ElMessage.success('订单状态已更新');
  try {
    await fetchOrders();
    if (drawerVisible.value && currentOrder.value?.orderId === orderId) {
      await viewDetail(orderId);
    }
  } catch {
    // handled in fetchOrders/viewDetail
  }
}

function canConfirm(row: any) {
  return role.value === 'seller' && String(row.orderStatus) === 'pending_confirm';
}

function canDeliver(row: any) {
  return role.value === 'seller' && String(row.orderStatus) === 'awaiting_delivery';
}

function canComplete(row: any) {
  return role.value === 'buyer' && String(row.orderStatus) === 'delivering';
}

function canCancel(row: any) {
  return ['pending_confirm', 'awaiting_delivery'].includes(String(row.orderStatus));
}

function showDemoTag(row: any) {
  return String(row?.orderNo || '').startsWith('DEMO');
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
.demo-alert,
.detail-alert {
  margin-bottom: 16px;
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
.order-no-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
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