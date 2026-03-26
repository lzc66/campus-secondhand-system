<template>
  <section class="glass-card panel">
    <SectionHeading title="My Orders" description="Switch between buyer and seller views to follow confirmation, delivery, and completion states." tag="Orders" />

    <div class="toolbar">
      <el-radio-group v-model="role" @change="handleRoleChange">
        <el-radio-button value="buyer">Buyer</el-radio-button>
        <el-radio-button value="seller">Seller</el-radio-button>
      </el-radio-group>
      <div class="summary-strip">
        <div class="summary-chip"><strong>{{ records.length }}</strong><span>Current Page</span></div>
        <div class="summary-chip"><strong>{{ pendingCount }}</strong><span>Pending</span></div>
        <div class="summary-chip"><strong>{{ completedCount }}</strong><span>Completed</span></div>
      </div>
    </div>

    <el-alert
      v-if="demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled"
      type="info"
      :closable="false"
      show-icon
      title="Demo orders include helper labels so you can explain the order lifecycle during presentation."
      class="demo-alert"
    />

    <div v-if="isLoading" class="skeleton-stack">
      <div v-for="index in 3" :key="index" class="glass-card skeleton-row">
        <el-skeleton animated :rows="4" />
      </div>
    </div>

    <template v-else-if="records.length">
      <el-table :data="records" stripe class="order-table">
        <el-table-column prop="orderNo" label="Order No." min-width="190">
          <template #default="scope">
            <div class="order-no-cell">
              <span>{{ scope.row.orderNo }}</span>
              <el-tag v-if="showDemoTag(scope.row)" size="small" effect="plain" type="warning">Demo</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="itemTitle" label="Item" min-width="220" />
        <el-table-column label="Status" width="130">
          <template #default="scope">
            <el-tag :type="orderStatusType(scope.row.orderStatus)">{{ orderStatusLabel(scope.row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Amount" width="130">
          <template #default="scope">{{ formatPrice(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="Created At" min-width="160">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Actions" min-width="320" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewDetail(scope.row.orderId)">Detail</el-button>
            <el-button v-if="canConfirm(scope.row)" link type="primary" @click="action(scope.row.orderId, 'confirm')">Confirm</el-button>
            <el-button v-if="canDeliver(scope.row)" link type="warning" @click="action(scope.row.orderId, 'deliver')">Delivering</el-button>
            <el-button v-if="canComplete(scope.row)" link type="success" @click="action(scope.row.orderId, 'complete')">Complete</el-button>
            <el-button v-if="canCancel(scope.row)" link type="danger" @click="action(scope.row.orderId, 'cancel')">Cancel</el-button>
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
      title="No orders yet"
      :description="role === 'buyer' ? 'You have not created any order yet. Browse public items first.' : 'No buyer has placed an order for your items yet.'"
    >
      <el-button v-if="role === 'buyer'" type="primary" @click="router.push('/items')">Browse items</el-button>
    </EmptyState>
  </section>

  <el-drawer v-model="drawerVisible" size="460px" title="Order Detail">
    <div v-if="detailLoading" class="drawer-skeleton">
      <el-skeleton animated :rows="8" />
    </div>
    <template v-else-if="currentOrder">
      <el-alert
        v-if="showDemoTag(currentOrder) && demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled"
        type="warning"
        :closable="false"
        show-icon
        title="This is a seeded demo order, useful for showing pending confirm, awaiting delivery, delivering, and completed states."
        class="detail-alert"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Order No.">{{ currentOrder.orderNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Item">{{ currentOrder.items?.[0]?.itemTitleSnapshot || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ orderStatusLabel(currentOrder.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="Amount">{{ formatPrice(currentOrder.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="Receiver">{{ currentOrder.receiverName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Phone">{{ currentOrder.receiverPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Delivery">{{ deliveryTypeLabel(currentOrder.deliveryType) }}</el-descriptions-item>
        <el-descriptions-item label="Address">{{ currentOrder.deliveryAddress || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Buyer Note">{{ currentOrder.buyerRemark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Created At">{{ formatDateTime(currentOrder.createdAt) }}</el-descriptions-item>
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
const demoStatus = ref<any>({ demoModeEnabled: false, demoItemNotesEnabled: true });

const pendingStatuses = ['pending_confirm', 'awaiting_delivery', 'delivering'];
const pendingCount = computed(() => records.value.filter((item) => pendingStatuses.includes(String(item.orderStatus))).length);
const completedCount = computed(() => records.value.filter((item) => String(item.orderStatus) === 'completed').length);

onMounted(async () => {
  await Promise.all([fetchOrders(), fetchDemoStatus()]);
});

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
  const note = type === 'cancel' ? 'User cancelled the order from the frontend page' : 'Frontend page action';
  if (type === 'cancel') {
    await ElMessageBox.confirm('Confirm cancelling this order?', 'Cancel order', { type: 'warning' });
  }
  if (type === 'confirm') await userApi.confirmOrder(orderId, { actionNote: note });
  if (type === 'deliver') await userApi.deliverOrder(orderId, { actionNote: note });
  if (type === 'complete') await userApi.completeOrder(orderId, { actionNote: note });
  if (type === 'cancel') await userApi.cancelOrder(orderId, { actionNote: note, cancelReason: note });
  ElMessage.success('Order status updated');
  await fetchOrders();
  if (drawerVisible.value && currentOrder.value?.orderId === orderId) {
    await viewDetail(orderId);
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
  return ['pending_confirm', 'awaiting_delivery', 'delivering'].includes(String(row.orderStatus));
}

function showDemoTag(row: any) {
  return String(row?.orderNo || '').startsWith('DEMO');
}

function orderStatusLabel(status?: string) {
  const map: Record<string, string> = {
    pending_confirm: 'Pending Confirm',
    awaiting_delivery: 'Awaiting Delivery',
    delivering: 'Delivering',
    completed: 'Completed',
    cancelled: 'Cancelled',
    closed: 'Closed'
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

function deliveryTypeLabel(value?: string) {
  const map: Record<string, string> = {
    dorm_delivery: 'Dorm Delivery',
    self_pickup: 'Self Pickup',
    face_to_face: 'Face to Face'
  };
  return map[String(value)] || labelize(value, '--');
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