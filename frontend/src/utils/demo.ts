/**
 * 演示数据标记工具。
 * 后端演示数据标题统一以 "[演示] " 或 "[Demo] " 开头。
 * 此前 ItemCard 与 ItemDetailView 中的正则含编码乱码(婕旂ず),导致标记永远不命中,已统一收口到这里。
 */
const DEMO_TITLE_PREFIX_PATTERN = /^\[(演示|Demo)\]/;

export function isDemoTitle(title: string | null | undefined): boolean {
  return DEMO_TITLE_PREFIX_PATTERN.test(title || '');
}

export const DEMO_ORDER_NO_PREFIX = 'DEMO';

export function isDemoOrderNo(orderNo: string | null | undefined): boolean {
  return Boolean(orderNo && orderNo.startsWith(DEMO_ORDER_NO_PREFIX));
}
