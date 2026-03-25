<template>
  <section class="glass-card panel">
    <SectionHeading title="公告管理" description="创建、发布和下线系统公告。" tag="Announcements"><el-button type="primary" @click="openEditor()">新建公告</el-button></SectionHeading>
    <el-table :data="records" stripe><el-table-column prop="title" label="标题" min-width="220" /><el-table-column prop="publishStatus" label="状态" width="140" /><el-table-column prop="pinned" label="置顶" width="100" /><el-table-column label="操作" width="260"><template #default="scope"><el-button link type="primary" @click="openEditor(scope.row)">编辑</el-button><el-button link type="success" @click="publish(scope.row.announcementId)">发布</el-button><el-button link type="warning" @click="offline(scope.row.announcementId)">下线</el-button></template></el-table-column></el-table>
  </section>
  <el-dialog v-model="editorVisible" :title="currentId ? '编辑公告' : '新建公告'" width="760px">
    <el-form :model="form" label-width="100px"><el-form-item label="标题"><el-input v-model="form.title" /></el-form-item><el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item><el-form-item label="置顶"><el-switch v-model="form.pinned" /></el-form-item><el-form-item label="状态"><el-select v-model="form.publishStatus"><el-option label="草稿" value="draft" /><el-option label="已发布" value="published" /></el-select></el-form-item><el-form-item label="过期时间"><el-date-picker v-model="form.expireAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></el-form>
    <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { adminApi } from '@/api/admin';
const records = ref<any[]>([]); const editorVisible = ref(false); const currentId = ref<number | null>(null); const form = reactive<any>({ title: '', content: '', pinned: false, publishStatus: 'draft', expireAt: '' });
onMounted(fetchList);
async function fetchList() { const data = await adminApi.getAnnouncements({ page: 1, size: 50 }); records.value = data.records || []; }
function openEditor(row?: any) { currentId.value = row?.announcementId || null; Object.assign(form, row || { title: '', content: '', pinned: false, publishStatus: 'draft', expireAt: '' }); editorVisible.value = true; }
async function save() { if (currentId.value) await adminApi.updateAnnouncement(currentId.value, form); else await adminApi.createAnnouncement(form); ElMessage.success('公告已保存'); editorVisible.value = false; fetchList(); }
async function publish(id: number) { await adminApi.publishAnnouncement(id, { actionNote: 'frontend publish' }); ElMessage.success('公告已发布'); fetchList(); }
async function offline(id: number) { await adminApi.offlineAnnouncement(id, { actionNote: 'frontend offline' }); ElMessage.success('公告已下线'); fetchList(); }
</script>

<style scoped>
.panel { padding: 24px; }
</style>