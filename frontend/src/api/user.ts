import { apiDelete, apiGet, apiPost, apiPut } from './http';
import type { ItemDetail, NotificationItem, PageResponse, RecommendationItem, UserCaptcha, UserProfile, WantedPost } from '@/types/api';

export const userApi = {
  getLoginCaptcha: () => apiGet<UserCaptcha>('/api/v1/user/auth/captcha'),
  login: (payload: Record<string, unknown>) => apiPost<any>('/api/v1/user/auth/login', payload),
  getCurrentUser: () => apiGet<UserProfile>('/api/v1/user/auth/me'),
  getProfile: () => apiGet<UserProfile>('/api/v1/user/profile'),
  updateProfile: (payload: Record<string, unknown>) => apiPut<any>('/api/v1/user/profile', payload),
  changePassword: (payload: Record<string, unknown>) => apiPut<any>('/api/v1/user/profile/password', payload),
  uploadAvatar: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiPost<any>('/api/v1/user/profile/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
  uploadItemImage: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiPost<any>('/api/v1/user/items/images', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
  getMyItems: (params?: Record<string, unknown>) => apiGet<PageResponse<ItemDetail>>('/api/v1/user/items', { params }),
  getMyItemDetail: (itemId: number) => apiGet<ItemDetail>(`/api/v1/user/items/${itemId}`),
  createItem: (payload: Record<string, unknown>) => apiPost<any>('/api/v1/user/items', payload),
  updateItem: (itemId: number, payload: Record<string, unknown>) => apiPut<any>(`/api/v1/user/items/${itemId}`, payload),
  deleteItem: (itemId: number) => apiDelete<any>(`/api/v1/user/items/${itemId}`),
  createComment: (itemId: number, payload: Record<string, unknown>) => apiPost<any>(`/api/v1/user/items/${itemId}/comments`, payload),
  replyComment: (commentId: number, payload: Record<string, unknown>) => apiPost<any>(`/api/v1/user/comments/${commentId}/reply`, payload),
  getReceivedComments: (params?: Record<string, unknown>) => apiGet<any>('/api/v1/user/comments/received', { params }),
  getWantedPosts: (params?: Record<string, unknown>) => apiGet<PageResponse<WantedPost>>('/api/v1/user/wanted-posts', { params }),
  getWantedPostDetail: (wantedPostId: number) => apiGet<WantedPost>(`/api/v1/user/wanted-posts/${wantedPostId}`),
  saveWantedPost: (payload: Record<string, unknown>, wantedPostId?: number) => wantedPostId ? apiPut<any>(`/api/v1/user/wanted-posts/${wantedPostId}`, payload) : apiPost<any>('/api/v1/user/wanted-posts', payload),
  deleteWantedPost: (wantedPostId: number) => apiDelete<any>(`/api/v1/user/wanted-posts/${wantedPostId}`),
  createOrder: (payload: Record<string, unknown>) => apiPost<any>('/api/v1/user/orders', payload),
  getOrders: (params?: Record<string, unknown>) => apiGet<any>('/api/v1/user/orders', { params }),
  getOrderDetail: (orderId: number) => apiGet<any>(`/api/v1/user/orders/${orderId}`),
  confirmOrder: (orderId: number, payload: Record<string, unknown>) => apiPost<any>(`/api/v1/user/orders/${orderId}/confirm`, payload),
  deliverOrder: (orderId: number, payload: Record<string, unknown>) => apiPost<any>(`/api/v1/user/orders/${orderId}/deliver`, payload),
  completeOrder: (orderId: number, payload: Record<string, unknown>) => apiPost<any>(`/api/v1/user/orders/${orderId}/complete`, payload),
  cancelOrder: (orderId: number, payload: Record<string, unknown>) => apiPost<any>(`/api/v1/user/orders/${orderId}/cancel`, payload),
  getNotifications: (params?: Record<string, unknown>) => apiGet<PageResponse<NotificationItem>>('/api/v1/user/notifications', { params }),
  markNotificationRead: (notificationId: number) => apiPost<any>(`/api/v1/user/notifications/${notificationId}/read`),
  markAllNotificationsRead: () => apiPost<any>('/api/v1/user/notifications/read-all'),
  getRecommendations: (params?: Record<string, unknown>) => apiGet<PageResponse<RecommendationItem>>('/api/v1/user/recommendations', { params }),
  refreshRecommendations: (limit = 12) => apiPost<any>(`/api/v1/user/recommendations/refresh?limit=${limit}`),
  clickRecommendation: (recommendationId: number) => apiPost<any>(`/api/v1/user/recommendations/${recommendationId}/click`)
};