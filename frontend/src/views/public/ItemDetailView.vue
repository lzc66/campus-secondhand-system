<template>
  <div class="page-shell detail-page" v-loading="isLoading" v-if="detail">
    <div class="detail-main">
      <section class="gallery glass-card">
        <img v-if="activeImage" class="hero-image" :src="activeImage" :alt="detail.title" />
        <div v-else class="hero-placeholder">{{ detail.title.slice(0, 2) }}</div>
        <div class="thumbs"><button v-for="image in detail.images || []" :key="image.imageUrl" @click="activeImage = image.imageUrl"><img :src="image.imageUrl" :alt="detail.title" /></button></div>
      </section>
      <section class="summary glass-card">
        <div class="app-chip">{{ detail.categoryName }}</div>
        <h1>{{ detail.title }}</h1>
        <p class="desc">{{ detail.description || '暂无描述' }}</p>
        <div class="meta-line"><strong>{{ formatPrice(detail.price) }}</strong><span>{{ detail.tradeMode }}</span><span>{{ detail.conditionLevel }}</span></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="品牌">{{ detail.brand || '--' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ detail.model || '--' }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ detail.stock || 1 }}</el-descriptions-item>
          <el-descriptions-item label="联系手机">{{ detail.contactPhone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="QQ">{{ detail.contactQq || '--' }}</el-descriptions-item>
          <el-descriptions-item label="微信">{{ detail.contactWechat || '--' }}</el-descriptions-item>
          <el-descriptions-item label="取货地点">{{ detail.pickupAddress || '--' }}</el-descriptions-item>
        </el-descriptions>
        <div class="actions"><el-button type="primary" @click="openOrder">在线下单</el-button><el-button plain @click="replyVisible = true">写评论</el-button></div>
      </section>
    </div>
    <section class="detail-lower">
      <div class="seller glass-card">
        <SectionHeading title="卖家信息" description="可直接按联系方式线下沟通。" tag="Seller" />
        <p><strong>{{ detail.seller?.realName || '校园卖家' }}</strong></p>
        <p>{{ detail.seller?.collegeName || '--' }} {{ detail.seller?.majorName || '' }}</p>
      </div>
      <div class="comments glass-card">
        <SectionHeading title="评论区" description="登录后可发表评论，卖家可回复。" tag="Comments" />
        <div v-if="comments.length" class="comment-list"><article v-for="comment in comments" :key="comment.commentId" class="comment-item"><strong>{{ comment.author?.realName || '同学' }}</strong><span>{{ formatDateTime(comment.createdAt) }}</span><p>{{ comment.content }}</p><div v-if="comment.reply" class="reply-box"><strong>卖家回复</strong><p>{{ comment.reply.content }}</p></div></article></div>
        <EmptyState v-else title="还没有评论" description="成为第一个提问的人。" />
      </div>
    </section>
    <section class="related-section">
      <SectionHeading title="同类推荐" description="根据当前类目补充一些公开在售商品。" tag="Related" />
      <div v-if="relatedItems.length" class="related-grid"><ItemCard v-for="item in relatedItems" :key="item.itemId" :item="item" /></div>
      <EmptyState v-else title="暂无同类推荐" description="当前分类下还没有更多公开商品。" />
    </section>
  </div>
  <el-dialog v-model="replyVisible" title="发表评论" width="520px"><el-form><el-form-item label="评论内容"><el-input v-model="commentContent" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button @click="replyVisible = false">取消</el-button><el-button type="primary" @click="submitComment">提交</el-button></template></el-dialog>
  <el-dialog v-model="orderVisible" title="在线下单" width="560px"><el-form :model="orderForm" label-width="90px"><el-form-item label="收货人"><el-input v-model="orderForm.receiverName" /></el-form-item><el-form-item label="手机号"><el-input v-model="orderForm.receiverPhone" /></el-form-item><el-form-item label="配送方式"><el-select v-model="orderForm.deliveryType"><el-option label="宿舍配送" value="dorm_delivery" /><el-option label="自提" value="self_pickup" /></el-select></el-form-item><el-form-item label="地址"><el-input v-model="orderForm.deliveryAddress" /></el-form-item><el-form-item label="备注"><el-input v-model="orderForm.buyerRemark" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="orderVisible = false">取消</el-button><el-button type="primary" @click="submitOrder">提交订单</el-button></template></el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import EmptyState from '@/components/common/EmptyState.vue';
import ItemCard from '@/components/common/ItemCard.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import { userApi } from '@/api/user';
import { useAuthStore } from '@/stores/auth';
import type { ItemDetail, ItemSummary, PublicComment } from '@/types/api';
import { formatDateTime, formatPrice } from '@/utils/format';
const route = useRoute(); const router = useRouter(); const authStore = useAuthStore(); const detail = ref<ItemDetail | null>(null); const comments = ref<PublicComment[]>([]); const relatedItems = ref<ItemSummary[]>([]); const activeImage = ref(''); const replyVisible = ref(false); const orderVisible = ref(false); const commentContent = ref(''); const isLoading = ref(false);
const orderForm = ref({ receiverName: '', receiverPhone: '', deliveryType: 'dorm_delivery', deliveryAddress: '', buyerRemark: '', quantity: 1 });
onMounted(async () => { isLoading.value = true; try { const itemId = Number(route.params.id); detail.value = await publicApi.getItemDetail(itemId); activeImage.value = detail.value.images?.[0]?.imageUrl || detail.value.coverImageUrl || ''; const [commentPage, relatedPage] = await Promise.all([publicApi.getItemComments(itemId, { page: 1, size: 10 }), publicApi.getItems({ page: 1, size: 4, categoryId: detail.value.categoryId, sortBy: 'latest' })]); comments.value = commentPage.records || []; relatedItems.value = (relatedPage.records || []).filter((item) => item.itemId !== itemId); orderForm.value.receiverName = authStore.userProfile?.realName || ''; orderForm.value.receiverPhone = authStore.userProfile?.phone || ''; } finally { isLoading.value = false; } });
function openOrder() { if (!authStore.isUserLoggedIn) { router.push('/login'); return; } orderVisible.value = true; }
async function submitComment() { if (!authStore.isUserLoggedIn) { router.push('/login'); return; } await userApi.createComment(Number(route.params.id), { content: commentContent.value }); ElMessage.success('评论已提交'); replyVisible.value = false; commentContent.value = ''; const commentPage = await publicApi.getItemComments(Number(route.params.id), { page: 1, size: 10 }); comments.value = commentPage.records || []; }
async function submitOrder() { if (!detail.value) return; await userApi.createOrder({ itemId: detail.value.itemId, ...orderForm.value }); ElMessage.success('订单已创建'); orderVisible.value = false; router.push('/user/orders'); }
</script>

<style scoped>
.detail-page { padding: 28px 0 40px; }
.detail-main { display: grid; grid-template-columns: 1.2fr 1fr; gap: 24px; }
.gallery, .summary, .seller, .comments { padding: 22px; }
.hero-image, .hero-placeholder { width: 100%; aspect-ratio: 4/3; border-radius: 18px; }
.hero-image { object-fit: cover; }
.hero-placeholder { display: grid; place-items: center; background: rgba(25,63,58,0.08); font-size: 56px; font-family: var(--font-display); color: var(--brand); }
.thumbs { margin-top: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.thumbs button { width: 84px; padding: 0; border: none; background: transparent; cursor: pointer; }
.thumbs img { width: 84px; height: 84px; object-fit: cover; border-radius: 14px; }
h1 { margin: 12px 0; font-size: 42px; font-family: var(--font-display); }
.desc { color: var(--text-soft); }
.meta-line { display: flex; gap: 14px; align-items: center; margin: 18px 0 22px; }
.meta-line strong { font-size: 34px; color: var(--accent); font-family: var(--font-display); }
.actions { display: flex; gap: 12px; margin-top: 18px; }
.detail-lower { margin-top: 24px; display: grid; grid-template-columns: 320px 1fr; gap: 24px; }
.comment-list { display: grid; gap: 16px; }
.comment-item { padding-bottom: 16px; border-bottom: 1px solid var(--line); }
.comment-item span { color: var(--text-soft); margin-left: 8px; font-size: 13px; }
.reply-box { margin-top: 12px; padding: 14px; border-radius: 16px; background: rgba(25,63,58,0.06); }
.related-section { margin-top: 28px; }
.related-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
@media (max-width: 960px) { .detail-main, .detail-lower, .related-grid { grid-template-columns: 1fr; } }
</style>