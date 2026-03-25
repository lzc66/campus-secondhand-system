<template>
  <section class="glass-card panel">
    <SectionHeading title="我的订单" description="买家和卖家视角切换查看订单状态。" tag="Orders" />
    <el-radio-group v-model="role" @change="fetchOrders"><el-radio-button label="buyer">我是买家</el-radio-button><el-radio-button label="seller">我是卖家</el-radio-button></el-radio-group>
    <el-table :data="records" stripe style="margin-top: 18px;">
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="itemTitle" label="商品" min-width="180" />
      <el-table-column prop="orderStatus" label="状态" width="150" />
      <el-table-column prop="orderAmount" label="金额" width="120"><template #default="scope">{{ formatPrice(scope.row.orderAmount) }}</template></el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="scope">
          <el-button v-if="role === 'seller'" link type="primary" @click="action(scope.row.orderId, 'confirm')">接单</el-button>
          <el-button v-if="role === 'seller'" link type="primary" @click="action(scope.row.orderId, 'deliver')">配送中</el-button>
          <el-button v-if="role === 'buyer'" link type="success" @click="action(scope.row.orderId, 'complete')">确认完成</el-button>
          <el-button link type="danger" @click="action(scope.row.orderId, 'cancel')">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { formatPrice } from '@/utils/format';

const role = ref('buyer');
const records = ref<any[]>([]);
onMounted(fetchOrders);
async function fetchOrders() { const data = await userApi.getOrders({ role: role.value, page: 1, size: 50 }); records.value = data.records || []; }
async function action(orderId: number, type: string) { const payload = { actionNote: 'frontend action', cancelReason: 'frontend cancel' }; if (type === 'confirm') await userApi.confirmOrder(orderId, payload); if (type === 'deliver') await userApi.deliverOrder(orderId, payload); if (type === 'complete') await userApi.completeOrder(orderId, payload); if (type === 'cancel') await userApi.cancelOrder(orderId, payload); ElMessage.success('订单操作成功'); fetchOrders(); }
</script>

<style scoped>
.panel { padding: 24px; }
</style>