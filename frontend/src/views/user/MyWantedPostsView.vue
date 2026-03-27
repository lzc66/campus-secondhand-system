<template>
  <section class="glass-card panel">
    <SectionHeading title="我的求购" description="发布和维护自己的求购需求。" tag="Wanted">
      <el-button type="primary" @click="openEditor()">发布求购</el-button>
    </SectionHeading>
    <el-table :data="records" stripe>
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="expiresAt" label="截止时间" width="180"><template #default="scope">{{ formatDateTime(scope.row.expiresAt) }}</template></el-table-column>
      <el-table-column label="操作" width="220"><template #default="scope"><el-button link type="primary" @click="openEditor(scope.row)">编辑</el-button><el-button link type="danger" @click="removePost(scope.row.wantedPostId)">删除</el-button></template></el-table-column>
    </el-table>
  </section>
  <el-dialog v-model="editorVisible" :title="currentId ? '编辑求购' : '发布求购'" width="620px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="分类"><el-select v-model="form.categoryId"><el-option v-for="item in categories" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" /></el-select></el-form-item>
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="最低预算"><el-input-number v-model="form.expectedPriceMin" :min="0" /></el-form-item>
      <el-form-item label="最高预算"><el-input-number v-model="form.expectedPriceMax" :min="0" /></el-form-item>
      <el-form-item label="截止时间"><el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" @click="savePost">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import { userApi } from '@/api/user';
import { formatDateTime } from '@/utils/format';
const records = ref<any[]>([]); const categories = ref<any[]>([]); const editorVisible = ref(false); const currentId = ref<number | null>(null);
const form = reactive<any>({ categoryId: undefined, title: '', brand: '', description: '', expectedPriceMin: 0, expectedPriceMax: 0, status: 'open', expiresAt: '' });
onMounted(async () => { categories.value = await publicApi.getCategories(); fetchPosts(); });
async function fetchPosts() { const data = await userApi.getWantedPosts({ page: 1, size: 50 }); records.value = data.records || []; }
function openEditor(row?: any) { currentId.value = row?.wantedPostId || null; Object.assign(form, row || { categoryId: undefined, title: '', brand: '', description: '', expectedPriceMin: 0, expectedPriceMax: 0, status: 'open', expiresAt: '' }); editorVisible.value = true; }
async function savePost() { await userApi.saveWantedPost(form, currentId.value || undefined); ElMessage.success('求购已保存'); editorVisible.value = false; fetchPosts(); }
async function removePost(id: number) { await ElMessageBox.confirm('确认删除该求购吗？', '提示'); await userApi.deleteWantedPost(id); ElMessage.success('求购已删除'); fetchPosts(); }
</script>

<style scoped>
.panel { padding: 24px; }
</style>