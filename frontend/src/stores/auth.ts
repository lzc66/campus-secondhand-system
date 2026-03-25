import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { AdminProfile, UserProfile } from '@/types/api';

const USER_TOKEN_KEY = 'campus:user-token';
const USER_PROFILE_KEY = 'campus:user-profile';
const ADMIN_TOKEN_KEY = 'campus:admin-token';
const ADMIN_PROFILE_KEY = 'campus:admin-profile';

export const useAuthStore = defineStore('auth', () => {
  const userToken = ref(localStorage.getItem(USER_TOKEN_KEY) || '');
  const adminToken = ref(localStorage.getItem(ADMIN_TOKEN_KEY) || '');
  const userProfile = ref<UserProfile | null>(readJson(USER_PROFILE_KEY));
  const adminProfile = ref<AdminProfile | null>(readJson(ADMIN_PROFILE_KEY));

  const isUserLoggedIn = computed(() => Boolean(userToken.value));
  const isAdminLoggedIn = computed(() => Boolean(adminToken.value));

  function setUserAuth(token: string, profile: UserProfile | null) {
    userToken.value = token;
    userProfile.value = profile;
    localStorage.setItem(USER_TOKEN_KEY, token);
    localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile || null));
  }

  function setAdminAuth(token: string, profile: AdminProfile | null) {
    adminToken.value = token;
    adminProfile.value = profile;
    localStorage.setItem(ADMIN_TOKEN_KEY, token);
    localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile || null));
  }

  function clearUserAuth() {
    userToken.value = '';
    userProfile.value = null;
    localStorage.removeItem(USER_TOKEN_KEY);
    localStorage.removeItem(USER_PROFILE_KEY);
  }

  function clearAdminAuth() {
    adminToken.value = '';
    adminProfile.value = null;
    localStorage.removeItem(ADMIN_TOKEN_KEY);
    localStorage.removeItem(ADMIN_PROFILE_KEY);
  }

  return {
    userToken,
    adminToken,
    userProfile,
    adminProfile,
    isUserLoggedIn,
    isAdminLoggedIn,
    setUserAuth,
    setAdminAuth,
    clearUserAuth,
    clearAdminAuth
  };
});

function readJson(key: string) {
  const raw = localStorage.getItem(key);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}