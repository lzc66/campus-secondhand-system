import type { TagProps } from 'element-plus';

const REGISTRATION_STATUS_LABELS: Record<string, string> = {
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  cancelled: '已取消'
};

const USER_ACCOUNT_STATUS_LABELS: Record<string, string> = {
  active: '正常',
  locked: '已锁定',
  disabled: '已禁用'
};

const ITEM_STATUS_LABELS: Record<string, string> = {
  on_sale: '在售',
  off_shelf: '已下架',
  sold: '已售出',
  deleted: '已删除',
  reserved: '已锁定'
};

const ORDER_STATUS_LABELS: Record<string, string> = {
  pending_confirm: '待确认',
  confirmed: '待配送',
  awaiting_delivery: '待配送',
  delivering: '配送中',
  completed: '已完成',
  cancelled: '已取消',
  closed: '已关闭'
};

const ITEM_CONDITION_LABELS: Record<string, string> = {
  new: '全新',
  almost_new: '几乎全新',
  lightly_used: '轻微使用',
  used: '正常使用',
  well_used: '明显使用痕迹'
};

const DELIVERY_TYPE_LABELS: Record<string, string> = {
  dorm_delivery: '送货到宿舍',
  self_pickup: '宿舍自取',
  face_to_face: '当面交易'
};

const GENDER_LABELS: Record<string, string> = {
  male: '男',
  female: '女',
  unknown: '保密'
};

export function getRegistrationStatusLabel(status?: string) {
  return REGISTRATION_STATUS_LABELS[String(status || '')] || '未知状态';
}

export function getRegistrationStatusTagType(status?: string): TagProps['type'] {
  if (status === 'approved') return 'success';
  if (status === 'rejected') return 'danger';
  if (status === 'cancelled') return 'info';
  return 'warning';
}

export function getUserAccountStatusLabel(status?: string) {
  return USER_ACCOUNT_STATUS_LABELS[String(status || '')] || '未知状态';
}

export function getUserAccountStatusTagType(status?: string): TagProps['type'] {
  if (status === 'active') return 'success';
  if (status === 'locked') return 'warning';
  if (status === 'disabled') return 'danger';
  return 'info';
}

export function getItemStatusLabel(status?: string) {
  return ITEM_STATUS_LABELS[String(status || '')] || '未知状态';
}

export function getItemStatusTagType(status?: string): TagProps['type'] {
  if (status === 'on_sale') return 'success';
  if (status === 'off_shelf' || status === 'reserved') return 'warning';
  if (status === 'sold') return 'info';
  if (status === 'deleted') return 'danger';
  return 'info';
}

export function getOrderStatusLabel(status?: string) {
  return ORDER_STATUS_LABELS[String(status || '')] || '未知状态';
}

export function getOrderStatusTagType(status?: string): TagProps['type'] {
  if (status === 'completed') return 'success';
  if (status === 'pending_confirm' || status === 'confirmed' || status === 'awaiting_delivery') return 'warning';
  if (status === 'delivering') return 'primary';
  if (status === 'cancelled' || status === 'closed') return 'info';
  return 'danger';
}

export function getItemConditionLabel(condition?: string) {
  return ITEM_CONDITION_LABELS[String(condition || '')] || '成色待补充';
}

export function getDeliveryTypeLabel(type?: string) {
  return DELIVERY_TYPE_LABELS[String(type || '')] || '待确认';
}

export function getGenderLabel(gender?: string) {
  return GENDER_LABELS[String(gender || '')] || '保密';
}
