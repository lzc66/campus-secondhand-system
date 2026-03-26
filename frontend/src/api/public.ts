import { apiGet, apiPost } from './http';
import type { Announcement, DemoModeStatus, ItemCategory, ItemDetail, ItemSummary, PageResponse, PublicComment, WantedPost } from '@/types/api';

export const publicApi = {
  getCategories: () => apiGet<ItemCategory[]>('/api/v1/public/item-categories'),
  getDemoModeStatus: () => apiGet<DemoModeStatus>('/api/v1/public/demo-mode'),
  getAnnouncements: (params?: Record<string, unknown>) => apiGet<PageResponse<Announcement>>('/api/v1/public/announcements', { params }),
  getAnnouncementDetail: (announcementId: number) => apiGet<Announcement>(`/api/v1/public/announcements/${announcementId}`),
  getItems: (params?: Record<string, unknown>) => apiGet<PageResponse<ItemSummary>>('/api/v1/public/items', { params }),
  getItemDetail: (itemId: number) => apiGet<ItemDetail>(`/api/v1/public/items/${itemId}`),
  getItemComments: (itemId: number, params?: Record<string, unknown>) => apiGet<PageResponse<PublicComment>>(`/api/v1/public/items/${itemId}/comments`, { params }),
  getWantedPosts: (params?: Record<string, unknown>) => apiGet<PageResponse<WantedPost>>('/api/v1/public/wanted-posts', { params }),
  getWantedPostDetail: (wantedPostId: number) => apiGet<WantedPost>(`/api/v1/public/wanted-posts/${wantedPostId}`),
  uploadStudentCard: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiPost<any>('/api/v1/public/files/student-card', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
  submitRegistration: (payload: Record<string, unknown>) => apiPost<any>('/api/v1/public/registration-applications', payload)
};