<template>
  <div class="home-view page-shell" v-loading="isLoading">
    <section class="hero glass-card fade-up">
      <div class="hero-copy">
        <div class="app-chip">Editorial Campus Board</div>
        <h1>把校园闲置，变成下一位同学的刚需。</h1>
        <p>浏览二手商品、发起求购、查看公告和推荐，让校园交易从零散聊天变成可信、清晰、可管理的线上流程。</p>
        <div class="hero-actions">
          <el-input v-model="keyword" placeholder="搜索品牌、名称、型号" @keyup.enter="goSearch"><template #append><el-button @click="goSearch">检索商品</el-button></template></el-input>
          <div class="quick-links"><RouterLink to="/register">提交注册申请</RouterLink><RouterLink to="/items">进入商品广场</RouterLink><RouterLink to="/wanted-posts">查看求购需求</RouterLink></div>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-note glass-card"><small>今日焦点</small><strong>{{ latestAnnouncement?.title || '系统公告待发布' }}</strong><p>{{ latestAnnouncement?.content || '这里会展示管理员最新发布的校园系统公告。' }}</p></div>
        <div class="hero-categories"><button v-for="item in categories.slice(0, 6)" :key="item.categoryId" @click="goCategory(item.categoryId)">{{ item.categoryName }}</button></div>
      </div>
    </section>
    <section class="metric-strip"><div class="metric-card glass-card"><small>分类入口</small><strong>{{ categories.length }}</strong></div><div class="metric-card glass-card"><small>最新商品</small><strong>{{ latestItems.length }}</strong></div><div class="metric-card glass-card"><small>近期公告</small><strong>{{ announcements.length }}</strong></div><div class="metric-card glass-card"><small>热门求购</small><strong>{{ wantedPosts.length }}</strong></div></section>
    <section class="home-section"><SectionHeading title="最新商品" :description="`已接入 ${categories.length} 个分类入口，优先展示最近上架的校园二手物品。`" tag="Fresh In"><RouterLink to="/items">查看全部</RouterLink></SectionHeading><div v-if="latestItems.length" class="item-grid"><ItemCard v-for="item in latestItems" :key="item.itemId" :item="item" /></div><EmptyState v-else title="暂无商品" description="当前没有公开在售商品。" /></section>
    <section class="home-columns"><div><SectionHeading title="近期公告" description="系统和活动通知集中展示。" tag="Notice" /><div class="notice-list"><RouterLink v-for="notice in announcements" :key="notice.announcementId" to="/announcements" class="notice-card glass-card"><strong>{{ notice.title }}</strong><p>{{ notice.content }}</p></RouterLink></div></div><div><SectionHeading title="热门求购" description="看看同学们最近在找什么。" tag="Wanted" /><div class="wanted-list"><RouterLink v-for="post in wantedPosts" :key="post.wantedPostId" :to="`/wanted-posts/${post.wantedPostId}`" class="wanted-card glass-card"><strong>{{ post.title }}</strong><p>{{ post.description || '暂无详细描述' }}</p><span>{{ post.categoryName || '校园求购' }}</span></RouterLink></div></div></section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import EmptyState from '@/components/common/EmptyState.vue';
import ItemCard from '@/components/common/ItemCard.vue';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import type { Announcement, ItemCategory, ItemSummary, WantedPost } from '@/types/api';
const router = useRouter(); const keyword = ref(''); const latestItems = ref<ItemSummary[]>([]); const categories = ref<ItemCategory[]>([]); const announcements = ref<Announcement[]>([]); const latestAnnouncement = ref<Announcement | null>(null); const wantedPosts = ref<WantedPost[]>([]); const isLoading = ref(false);
onMounted(async () => { isLoading.value = true; try { const [itemPage, categoryList, announcementPage, wantedPage] = await Promise.all([publicApi.getItems({ page: 1, size: 8, sortBy: 'latest' }), publicApi.getCategories(), publicApi.getAnnouncements({ page: 1, size: 3 }), publicApi.getWantedPosts({ page: 1, size: 4, sortBy: 'latest' })]); latestItems.value = itemPage.records || []; categories.value = categoryList || []; announcements.value = announcementPage.records || []; latestAnnouncement.value = announcements.value[0] || null; wantedPosts.value = wantedPage.records || []; } finally { isLoading.value = false; } });
function goSearch() { router.push({ path: '/items', query: keyword.value ? { keyword: keyword.value } : undefined }); }
function goCategory(categoryId: number) { router.push({ path: '/items', query: { categoryId: String(categoryId) } }); }
</script>

<style scoped>
.home-view { padding: 24px 0 40px; }
.hero { display: grid; grid-template-columns: 1.7fr 1fr; gap: 24px; padding: clamp(24px, 4vw, 42px); margin-bottom: 22px; position: relative; overflow: hidden; }
.hero::after { content: ''; position: absolute; inset: auto -60px -60px auto; width: 220px; height: 220px; border-radius: 50%; background: radial-gradient(circle, rgba(220, 127, 57, 0.24), transparent 72%); }
.hero-copy { position: relative; z-index: 1; }
h1 { margin: 12px 0 16px; font-size: clamp(40px, 6vw, 74px); line-height: 0.93; font-family: var(--font-display); }
p { color: var(--text-soft); font-size: 16px; line-height: 1.8; }
.hero-actions { margin-top: 26px; display: grid; gap: 16px; }
.quick-links { display: flex; gap: 12px; flex-wrap: wrap; }
.quick-links a { padding: 10px 14px; border-radius: 999px; background: rgba(25,63,58,0.08); }
.hero-side { display: grid; gap: 18px; position: relative; z-index: 1; }
.hero-note { padding: 24px; }
.hero-note small { color: var(--text-soft); text-transform: uppercase; }
.hero-note strong { display: block; margin: 10px 0 12px; font-size: 28px; font-family: var(--font-display); }
.hero-categories { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.hero-categories button { padding: 18px; border-radius: 18px; border: 1px solid var(--line); background: rgba(255,255,255,0.6); cursor: pointer; }
.metric-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 34px; }
.metric-card { padding: 18px 20px; }
.metric-card small { display: block; color: var(--text-soft); margin-bottom: 8px; }
.metric-card strong { font-family: var(--font-display); font-size: 34px; }
.home-section { margin-bottom: 34px; }
.item-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
.home-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.notice-list, .wanted-list { display: grid; gap: 14px; }
.notice-card, .wanted-card { padding: 20px; }
.notice-card strong, .wanted-card strong { display: block; margin-bottom: 8px; font-size: 18px; }
.wanted-card span { color: var(--accent); }
@media (max-width: 1000px) { .hero, .home-columns, .metric-strip { grid-template-columns: 1fr; } .item-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .item-grid { grid-template-columns: 1fr; } }
</style>