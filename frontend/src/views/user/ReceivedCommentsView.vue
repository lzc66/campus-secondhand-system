<template>
  <section class="glass-card panel">
    <SectionHeading title="评论与回复" description="查看别人给你商品的留言，并以卖家身份回复。" tag="Comments" />
    <div class="comment-list">
      <article v-for="item in records" :key="item.commentId" class="comment-row glass-card">
        <div>
          <strong>{{ item.itemTitle || '商品评论' }}</strong>
          <p>{{ item.content }}</p>
        </div>
        <div class="comment-side">
          <span>{{ formatDateTime(item.createdAt) }}</span>
          <el-button size="small" @click="openReply(item)">回复</el-button>
        </div>
      </article>
    </div>
  </section>
  <el-dialog v-model="replyVisible" title="回复评论" width="520px">
    <el-input v-model="replyContent" type="textarea" :rows="4" />
    <template #footer><el-button @click="replyVisible = false">取消</el-button><el-button type="primary" @click="submitReply">发送回复</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { userApi } from '@/api/user';
import { formatDateTime } from '@/utils/format';
const records = ref<any[]>([]); const replyVisible = ref(false); const replyContent = ref(''); const currentCommentId = ref<number | null>(null);
onMounted(fetchComments);
async function fetchComments() { const data = await userApi.getReceivedComments({ page: 1, size: 50 }); records.value = data.records || []; }
function openReply(item: any) { currentCommentId.value = item.commentId; replyVisible.value = true; }
async function submitReply() { if (!currentCommentId.value) return; await userApi.replyComment(currentCommentId.value, { content: replyContent.value }); ElMessage.success('回复已发送'); replyVisible.value = false; replyContent.value = ''; fetchComments(); }
</script>

<style scoped>
.panel { padding: 24px; }
.comment-list { display: grid; gap: 12px; }
.comment-row { padding: 18px; display: flex; justify-content: space-between; gap: 18px; }
.comment-row p { margin: 10px 0 0; color: var(--text-soft); }
.comment-side { display: grid; justify-items: end; gap: 10px; color: var(--text-soft); }
</style>