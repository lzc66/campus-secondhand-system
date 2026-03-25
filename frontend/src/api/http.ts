import axios, { type AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { useAuthStore } from '@/stores/auth';
import type { ApiResponse } from '@/types/api';

const http = axios.create({
  baseURL: '/',
  timeout: 20000
});

http.interceptors.request.use((config) => {
  const authStore = useAuthStore();
  const isAdminRequest = config.url?.startsWith('/api/v1/admin');
  const token = isAdminRequest ? authStore.adminToken : authStore.userToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    if (payload && typeof payload.code === 'number') {
      if (payload.code !== 0) {
        ElMessage.error(payload.message || '请求失败');
        return Promise.reject(payload);
      }
      return payload.data;
    }
    return response.data;
  },
  (error) => {
    const authStore = useAuthStore();
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    if (status === 401) {
      if (url.startsWith('/api/v1/admin')) {
        authStore.clearAdminAuth();
        router.push('/admin/login');
      } else {
        authStore.clearUserAuth();
        router.push('/login');
      }
      ElMessage.warning('登录状态已失效，请重新登录');
    } else if (status === 403) {
      ElMessage.error('当前没有权限执行该操作');
    } else {
      ElMessage.error(error?.response?.data?.message || error?.message || '网络异常');
    }
    return Promise.reject(error);
  }
);

export function apiGet<T>(url: string, config?: AxiosRequestConfig) {
  return http.get<any, T>(url, config);
}

export function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return http.post<any, T>(url, data, config);
}

export function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return http.put<any, T>(url, data, config);
}

export function apiDelete<T>(url: string, config?: AxiosRequestConfig) {
  return http.delete<any, T>(url, config);
}