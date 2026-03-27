import dayjs from 'dayjs';

export function formatDateTime(value?: string | null, fallback = '--') {
  if (!value) return fallback;
  return dayjs(value).format('YYYY-MM-DD HH:mm');
}

export function formatDate(value?: string | null, fallback = '--') {
  if (!value) return fallback;
  return dayjs(value).format('YYYY-MM-DD');
}

export function formatPrice(value?: number | string | null, fallback = '--') {
  if (value === null || value === undefined || value === '') return fallback;
  return `¥${Number(value).toFixed(2)}`;
}

export function labelize(value?: string | null, fallback = '--') {
  if (!value) return fallback;
  return value
    .split('_')
    .map((item) => item.charAt(0).toUpperCase() + item.slice(1))
    .join(' ');
}