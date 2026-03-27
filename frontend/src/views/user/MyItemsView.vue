<template>
  <section class="glass-card panel">
    <SectionHeading title="我的商品" description="维护自己发布的物品信息，支持编辑和删除。" tag="Inventory">
      <RouterLink to="/user/publish">发布新商品</RouterLink>
    </SectionHeading>
    <el-table :data="records" stripe>
      <el-table-column prop="title" label="商品" min-width="220" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="price" label="价格" width="120"><template #default="scope">{{ formatPrice(scope.row.price) }}</template></el-table-column>
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="editItem(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="removeItem(scope.row.itemId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { RouterLink, useRouter } from 'vue-router';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { formatPrice } from '@/utils/format';

const router = useRouter();
const records = ref<any[]>([]);
async function fetchItems() { const data = await userApi.getMyItems({ page: 1, size: 50 }); records.value = data.records || []; }
onMounted(fetchItems);
function editItem(row: any) { router.push({ path: '/user/publish', query: { editId: String(row.itemId) } }); }
async function removeItem(itemId: number) { await ElMessageBox.confirm('确认删除该商品？', '提示'); await userApi.deleteItem(itemId); ElMessage.success('商品已删除'); fetchItems(); }
</script>

<style scoped>
.panel { padding: 24px; }
</style>