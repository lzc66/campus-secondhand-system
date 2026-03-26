<template>
  <RouterLink :to="`/items/${item.itemId}`" class="item-card glass-card fade-up">
    <div class="thumb">
      <img v-if="item.coverImageUrl" :src="item.coverImageUrl" :alt="item.title" />
      <div v-else class="placeholder">{{ item.title.slice(0, 2) }}</div>
      <span class="badge">{{ item.categoryName || 'Category' }}</span>
      <span v-if="showDemoFlag" class="demo-flag">Demo Item</span>
    </div>
    <div class="body">
      <div class="topline">
        <h3>{{ item.title }}</h3>
        <span>{{ conditionLabel }}</span>
      </div>
      <p>{{ item.brand || 'Campus Pick' }}<template v-if="item.model"> / {{ item.model }}</template></p>
      <div v-if="showDemoFlag" class="demo-note">Sample data for presentation. Useful for showing item cards, recommendation lists, and order entry.</div>
      <div class="meta">
        <strong>{{ formatPrice(item.price) }}</strong>
        <span>{{ tradeModeLabel }}</span>
      </div>
    </div>
  </RouterLink>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { ItemSummary } from '@/types/api';
import { formatPrice, labelize } from '@/utils/format';

const props = withDefaults(defineProps<{
  item: ItemSummary;
  demoNotesEnabled?: boolean;
}>(), {
  demoNotesEnabled: false
});

const showDemoFlag = computed(() => props.demoNotesEnabled && /^\[(演示|Demo)\]/.test(props.item.title || ''));
const conditionLabel = computed(() => labelize(props.item.conditionLevel, 'On Sale'));
const tradeModeLabel = computed(() => labelize(props.item.tradeMode, 'Both'));
</script>

<style scoped>
.item-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.item-card:hover {
  transform: translateY(-6px) rotate(-0.4deg);
  box-shadow: 0 24px 70px rgba(25, 63, 58, 0.14);
}
.thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: linear-gradient(135deg, #eff4ef, #f7f2eb);
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 38px;
  font-family: var(--font-display);
  color: var(--brand);
}
.badge,
.demo-flag {
  position: absolute;
  left: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  backdrop-filter: blur(10px);
  font-size: 12px;
}
.badge {
  top: 14px;
  background: rgba(255, 255, 255, 0.78);
}
.demo-flag {
  top: 48px;
  background: rgba(220, 127, 57, 0.18);
  color: #8b4b17;
}
.body {
  padding: 18px;
}
h3 {
  margin: 0;
  font-size: 18px;
}
p {
  color: var(--text-soft);
  margin: 10px 0 14px;
  min-height: 22px;
}
.demo-note {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(220, 127, 57, 0.1);
  color: #7f4a1a;
  font-size: 13px;
  line-height: 1.6;
}
.topline,
.meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
strong {
  font-size: 24px;
  color: var(--accent);
  font-family: var(--font-display);
}
.meta span {
  color: var(--text-soft);
}
</style>