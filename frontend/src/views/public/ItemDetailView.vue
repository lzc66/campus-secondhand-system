<template>
  <div class="page-shell detail-page">
    <template v-if="isLoading && !detail">
      <section class="detail-main">
        <div class="glass-card loading-panel">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" class="hero-skeleton" />
              <div class="thumb-skeletons">
                <el-skeleton-item v-for="index in 4" :key="index" variant="image" class="thumb-skeleton" />
              </div>
            </template>
          </el-skeleton>
        </div>
        <div class="glass-card loading-panel">
          <el-skeleton animated :rows="10" />
        </div>
      </section>
      <section class="detail-lower">
        <div class="glass-card loading-panel"><el-skeleton animated :rows="5" /></div>
        <div class="glass-card loading-panel"><el-skeleton animated :rows="6" /></div>
      </section>
      <section class="related-section loading-panel glass-card">
        <el-skeleton animated :rows="4" />
      </section>
    </template>

    <template v-else-if="detail">
      <div class="detail-main">
        <section class="gallery glass-card fade-up">
          <img v-if="activeImage" class="hero-image" :src="activeImage" :alt="detail.title" />
          <div v-else class="hero-placeholder">{{ detail.title.slice(0, 2) }}</div>
          <div v-if="galleryImages.length" class="thumbs">
            <button
              v-for="image in galleryImages"
              :key="image"
              type="button"
              :class="['thumb-btn', { active: image === activeImage }]"
              @click="activeImage = image"
            >
              <img :src="image" :alt="detail.title" />
            </button>
          </div>
        </section>

        <section class="summary glass-card fade-up">
          <div class="summary-head">
            <div class="app-chip">{{ detail.categoryName }}</div>
            <el-tag v-if="showDemoNote" type="warning" effect="light">演示商品</el-tag>
          </div>
          <h1>{{ detail.title }}</h1>
          <el-alert
            v-if="showDemoNote"
            type="info"
            :closable="false"
            show-icon
            title="这条演示商品适合用于展示评论区、推荐卡片和在线下单流程。"
            class="demo-note"
          />
          <p class="desc">{{ detail.description || '卖家暂未补充更详细的商品说明。' }}</p>
          <div class="meta-line">
            <strong>{{ formatPrice(detail.price) }}</strong>
            <span>{{ tradeModeLabel(detail.tradeMode) }}</span>
            <span>{{ getItemConditionLabel(detail.conditionLevel) }}</span>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="品牌">{{ detail.brand || '--' }}</el-descriptions-item>
            <el-descriptions-item label="型号">{{ detail.model || '--' }}</el-descriptions-item>
            <el-descriptions-item label="库存">{{ detail.stock || 1 }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.contactPhone || '--' }}</el-descriptions-item>
            <el-descriptions-item label="QQ">{{ detail.contactQq || '--' }}</el-descriptions-item>
            <el-descriptions-item label="微信">{{ detail.contactWechat || '--' }}</el-descriptions-item>
            <el-descriptions-item label="取货地址">{{ detail.pickupAddress || '--' }}</el-descriptions-item>
          </el-descriptions>
          <div class="actions">
            <el-button type="primary" @click="openOrder">在线下单</el-button>
            <el-button plain @click="openComment">发表评论</el-button>
          </div>
        </section>
      </div>

      <section class="detail-lower">
        <div class="seller glass-card fade-up">
          <SectionHeading title="卖家信息" description="下单前可先了解卖家信息，也可以下单后再进一步沟通。" tag="Seller" />
          <p class="seller-name">{{ detail.seller?.realName || '校园卖家' }}</p>
          <p>{{ detail.seller?.collegeName || '--' }} {{ detail.seller?.majorName || '' }}</p>
          <p>{{ detail.seller?.className || '未填写班级信息' }}</p>
        </div>

        <div class="comments glass-card fade-up">
          <SectionHeading title="评论区" description="登录后可在这里评论，卖家可在个人中心进行回复。" tag="Comments" />
          <div v-if="commentsLoading" class="comment-skeletons">
            <el-skeleton v-for="index in 3" :key="index" animated :rows="3" />
          </div>
          <div v-else-if="comments.length" class="comment-list">
            <article v-for="comment in comments" :key="comment.commentId" class="comment-item">
              <div class="comment-head">
                <strong>{{ comment.author?.realName || '同学' }}</strong>
                <span>{{ formatDateTime(comment.createdAt) }}</span>
              </div>
              <p>{{ comment.content }}</p>
              <div v-if="comment.reply" class="reply-box">
                <strong>卖家回复</strong>
                <p>{{ comment.reply.content }}</p>
              </div>
            </article>
          </div>
          <EmptyState v-else title="还没有评论" description="你可以成为第一个留言咨询这件商品的人。" />
        </div>
      </section>

      <section class="related-section">
        <SectionHeading title="同类推荐" description="为你展示同分类下的其他公开商品，方便快速比较。" tag="Related" />
        <div v-if="relatedLoading" class="related-grid skeleton-grid">
          <article v-for="index in 4" :key="index" class="glass-card related-skeleton">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" class="related-image" />
                <div class="related-body">
                  <el-skeleton-item variant="h3" style="width: 74%" />
                  <el-skeleton-item variant="text" style="width: 48%; margin-top: 14px" />
                </div>
              </template>
            </el-skeleton>
          </article>
        </div>
        <div v-else-if="relatedItems.length" class="related-grid">
          <ItemCard
            v-for="item in relatedItems"
            :key="item.itemId"
            :item="item"
            :demo-notes-enabled="Boolean(demoStatus.demoModeEnabled && demoStatus.demoItemNotesEnabled)"
          />
        </div>
        <EmptyState v-else title="暂无同类商品" description="当前这个分类下没有更多公开商品了。" />
      </section>
    </template>

    <EmptyState v-else title="商品不存在" description="这件商品可能已被删除，或当前不再公开展示。">
      <el-button type="primary" @click="router.push('/items')">返回商品广场</el-button>
    </EmptyState>
  </div>

  <el-dialog v-model="commentVisible" title="发表评论" width="520px">
    <el-form>
      <el-form-item label="评论内容">
        <el-input v-model="commentContent" type="textarea" :rows="4" maxlength="300" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="commentVisible = false">取消</el-button>
      <el-button type="primary" :loading="submittingComment" @click="submitComment">提交评论</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="orderVisible" title="在线下单" width="560px">
    <el-form :model="orderForm" label-width="110px">
      <el-form-item label="收货人">
        <el-input v-model="orderForm.receiverName" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="orderForm.receiverPhone" />
      </el-form-item>
      <el-form-item label="配送方式">
        <el-select v-model="orderForm.deliveryType">
          <el-option label="送货到宿舍" value="dorm_delivery" />
          <el-option label="宿舍自取" value="self_pickup" />
        </el-select>
      </el-form-item>
      <el-form-item label="收货地址">
        <el-input v-model="orderForm.deliveryAddress" />
      </el-form-item>
      <el-form-item label="买家备注">
        <el-input v-model="orderForm.buyerRemark" type="textarea" :rows="3" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="orderVisible = false">取消</el-button>
      <el-button type="primary" :loading="submittingOrder" @click="submitOrder">提交订单</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
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
import { getItemConditionLabel } from '@/utils/status';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const detail = ref<ItemDetail | null>(null);
const comments = ref<PublicComment[]>([]);
const relatedItems = ref<ItemSummary[]>([]);
const activeImage = ref('');
const commentVisible = ref(false);
const orderVisible = ref(false);
const commentContent = ref('');
const isLoading = ref(false);
const commentsLoading = ref(false);
const relatedLoading = ref(false);
const submittingComment = ref(false);
const submittingOrder = ref(false);
const demoStatus = ref<any>({ demoModeEnabled: false, demoItemNotesEnabled: true });

const orderForm = ref({
  receiverName: '',
  receiverPhone: '',
  deliveryType: 'dorm_delivery',
  deliveryAddress: '',
  buyerRemark: '',
  quantity: 1
});

const galleryImages = computed(() => {
  if (!detail.value) return [] as string[];
  const images = (detail.value.images || []).map((item) => item.imageUrl).filter(Boolean);
  if (detail.value.coverImageUrl && !images.includes(detail.value.coverImageUrl)) {
    images.unshift(detail.value.coverImageUrl);
  }
  return images;
});

const showDemoNote = computed(() => Boolean(demoStatus.value.demoModeEnabled && demoStatus.value.demoItemNotesEnabled && /^\[(婕旂ず|Demo)\]/.test(detail.value?.title || '')));

watch(
  () => route.params.id,
  async (value) => {
    const itemId = Number(value);
    if (!itemId) {
      detail.value = null;
      return;
    }
    await loadDetail(itemId);
  },
  { immediate: true }
);

async function loadDetail(itemId: number) {
  isLoading.value = true;
  detail.value = null;
  comments.value = [];
  relatedItems.value = [];
  try {
    const [data, demoMode] = await Promise.all([publicApi.getItemDetail(itemId), publicApi.getDemoModeStatus()]);
    detail.value = data;
    demoStatus.value = demoMode;
    activeImage.value = galleryImages.value[0] || '';
    orderForm.value.receiverName = authStore.userProfile?.realName || '';
    orderForm.value.receiverPhone = authStore.userProfile?.phone || '';
    orderForm.value.deliveryAddress = authStore.userProfile?.dormitoryAddress || '';
    await Promise.all([loadComments(itemId), loadRelated(data)]);
  } catch {
    detail.value = null;
  } finally {
    isLoading.value = false;
  }
}

async function loadComments(itemId: number) {
  commentsLoading.value = true;
  try {
    const commentPage = await publicApi.getItemComments(itemId, { page: 1, size: 10 });
    comments.value = commentPage.records || [];
  } finally {
    commentsLoading.value = false;
  }
}

async function loadRelated(data: ItemDetail) {
  relatedLoading.value = true;
  try {
    const relatedPage = await publicApi.getItems({ page: 1, size: 4, categoryId: data.categoryId, sortBy: 'latest' });
    relatedItems.value = (relatedPage.records || []).filter((item) => item.itemId !== data.itemId).slice(0, 4);
  } finally {
    relatedLoading.value = false;
  }
}

function tradeModeLabel(value?: string) {
  const map: Record<string, string> = {
    both: '线上 + 线下',
    online: '仅线上',
    offline: '仅线下'
  };
  return map[String(value || '')] || '交易方式待确认';
}

function openOrder() {
  if (!authStore.isUserLoggedIn) {
    router.push('/login');
    return;
  }
  orderVisible.value = true;
}

function openComment() {
  if (!authStore.isUserLoggedIn) {
    router.push('/login');
    return;
  }
  commentVisible.value = true;
}

async function submitComment() {
  const itemId = Number(route.params.id);
  if (!commentContent.value.trim()) {
    ElMessage.warning('请先填写评论内容');
    return;
  }
  submittingComment.value = true;
  try {
    await userApi.createComment(itemId, { content: commentContent.value.trim() });
    ElMessage.success('评论提交成功');
    commentVisible.value = false;
    commentContent.value = '';
    await loadComments(itemId);
  } finally {
    submittingComment.value = false;
  }
}

async function submitOrder() {
  if (!detail.value) return;
  if (!orderForm.value.receiverName.trim() || !orderForm.value.receiverPhone.trim()) {
    ElMessage.warning('请先填写收货人和联系电话');
    return;
  }
  submittingOrder.value = true;
  try {
    await userApi.createOrder({ itemId: detail.value.itemId, ...orderForm.value });
    ElMessage.success('订单创建成功');
    orderVisible.value = false;
    router.push('/orders');
  } finally {
    submittingOrder.value = false;
  }
}
</script>

<style scoped>
.detail-page {
  padding: 28px 0 40px;
}
.detail-main {
  display: grid;
  grid-template-columns: 1.18fr 1fr;
  gap: 24px;
}
.gallery,
.summary,
.seller,
.comments,
.loading-panel {
  padding: 22px;
}
.summary-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.demo-note {
  margin-bottom: 16px;
}
.hero-image,
.hero-placeholder,
.hero-skeleton {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 20px;
}
.hero-image {
  object-fit: cover;
}
.hero-placeholder {
  display: grid;
  place-items: center;
  background: rgba(25, 63, 58, 0.08);
  font-size: 56px;
  font-family: var(--font-display);
  color: var(--brand);
}
.thumbs,
.thumb-skeletons {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.thumb-btn {
  width: 84px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 16px;
  background: transparent;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}
.thumb-btn:hover,
.thumb-btn.active {
  transform: translateY(-2px);
  border-color: rgba(35, 78, 119, 0.28);
}
.thumb-btn img,
.thumb-skeleton {
  width: 84px;
  height: 84px;
  object-fit: cover;
  border-radius: 14px;
}
h1 {
  margin: 12px 0;
  font-size: 42px;
  font-family: var(--font-display);
}
.desc {
  color: var(--text-soft);
  line-height: 1.9;
}
.meta-line {
  display: flex;
  gap: 14px;
  align-items: center;
  margin: 18px 0 22px;
  flex-wrap: wrap;
}
.meta-line strong {
  font-size: 34px;
  color: var(--accent);
  font-family: var(--font-display);
}
.actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}
.detail-lower {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
}
.seller-name {
  font-size: 28px;
  font-family: var(--font-display);
  margin: 8px 0 12px;
}
.comment-skeletons,
.comment-list {
  display: grid;
  gap: 16px;
}
.comment-item {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}
.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.comment-head span {
  color: var(--text-soft);
  font-size: 13px;
}
.comment-item p {
  margin: 12px 0 0;
  line-height: 1.8;
}
.reply-box {
  margin-top: 12px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(25, 63, 58, 0.06);
}
.related-section {
  margin-top: 28px;
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.related-skeleton {
  overflow: hidden;
  padding: 0;
}
.related-image {
  width: 100%;
  height: 220px;
}
.related-body {
  padding: 18px;
}
@media (max-width: 960px) {
  .detail-main,
  .detail-lower,
  .related-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  h1 {
    font-size: 32px;
  }
  .actions {
    flex-direction: column;
  }
}
</style>