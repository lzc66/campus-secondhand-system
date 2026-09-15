export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}

export interface UserProfile {
  userId: number;
  studentNo: string;
  realName: string;
  email: string;
  phone: string;
  qqNo?: string;
  wechatNo?: string;
  avatarFileId?: number;
  avatarUrl?: string;
  collegeName?: string;
  majorName?: string;
  className?: string;
  dormitoryAddress?: string;
  accountStatus?: string;
}

export interface AdminProfile {
  adminId?: number;
  adminNo: string;
  adminName: string;
  email?: string;
  roleCode: string;
  accountStatus?: string;
}

export interface LoginResponse<TProfile> {
  token: string;
  tokenType: string;
  expiresIn: number;
  userProfile?: TProfile;
  adminProfile?: TProfile;
}

export interface UserCaptcha {
  captchaKey: string;
  imageData: string;
  expiresInSeconds: number;
}

export interface SmtpSettings {
  enabled: boolean;
  host?: string;
  port?: number;
  username?: string;
  fromAddress?: string;
  authEnabled: boolean;
  starttlsEnabled: boolean;
  sslEnabled: boolean;
  passwordConfigured: boolean;
  smtpReady: boolean;
  updatedAt?: string;
  updatedByAdminId?: number;
}

export interface SmtpTestResult {
  toEmail: string;
  subject: string;
  testedAt: string;
}
export interface ItemSummary {
  itemId: number;
  categoryId: number;
  categoryName: string;
  title: string;
  brand?: string;
  model?: string;
  conditionLevel?: string;
  price: number;
  tradeMode?: string;
  negotiable: boolean;
  coverImageUrl?: string;
  viewCount?: number;
  publishedAt?: string;
  status?: string;
}

export interface ItemImage {
  fileId?: number;
  imageUrl: string;
  sortOrder?: number;
}

export interface ItemDetail extends ItemSummary {
  description?: string;
  originalPrice?: number;
  stock?: number;
  contactPhone?: string;
  contactQq?: string;
  contactWechat?: string;
  pickupAddress?: string;
  commentCount?: number;
  createdAt?: string;
  seller?: {
    userId?: number;
    studentNo?: string;
    realName?: string;
    avatarUrl?: string;
    collegeName?: string;
    majorName?: string;
    className?: string;
  };
  images?: ItemImage[];
}

export interface ItemCategory {
  categoryId: number;
  categoryCode?: string;
  categoryName: string;
  sortOrder?: number;
}

export interface PublicCommentReply {
  replyId?: number;
  commentId?: number;
  content: string;
  createdAt?: string;
  author?: { realName?: string; avatarUrl?: string };
}

export interface PublicComment {
  commentId: number;
  itemId?: number;
  content: string;
  createdAt?: string;
  author?: { realName?: string; avatarUrl?: string };
  reply?: PublicCommentReply;
}

export interface WantedPost {
  wantedPostId: number;
  categoryId?: number;
  categoryName?: string;
  title: string;
  brand?: string;
  description?: string;
  expectedPriceMin?: number;
  expectedPriceMax?: number;
  status?: string;
  expiresAt?: string;
  viewCount?: number;
  requester?: { realName?: string; avatarUrl?: string; collegeName?: string };
}

export interface Announcement {
  announcementId: number;
  title: string;
  content: string;
  pinned?: boolean;
  publishStatus?: string;
  publishTime?: string;
  expireAt?: string;
  createdAt?: string;
}

export interface NotificationItem {
  notificationId: number;
  title: string;
  content: string;
  readStatus?: string;
  createdAt?: string;
  businessType?: string;
}

export interface RecommendationItem {
  recommendationId: number;
  itemId: number;
  title: string;
  coverImageUrl?: string;
  price?: number;
  reasonCode?: string;
  reasonText?: string;
  score?: number;
}
export interface DemoDataSummary {
  users: number;
  items: number;
  orders: number;
  wantedPosts: number;
  announcements: number;
  pendingRegistrations: number;
}

export interface DemoModeStatus {
  demoModeEnabled: boolean;
  demoItemNotesEnabled: boolean;
  demoDataSeeded: boolean;
  demoDataSeededAt?: string;
  demoSummary: DemoDataSummary;
}

/** 公共端演示模式状态:只包含展示开关,无注入情况与业务统计 */
export interface PublicDemoModeStatus {
  demoModeEnabled: boolean;
  demoItemNotesEnabled: boolean;
}

export interface DemoDataSeedResult {
  createdCounts: DemoDataSummary;
  totalCounts: DemoDataSummary;
  demoDataSeeded: boolean;
  demoDataSeededAt?: string;
}

// ---------- 管理端看板 ----------

export interface AdminDashboardOverview {
  totalUsers: number;
  activeUsers: number;
  pendingRegistrationCount: number;
  totalItems: number;
  onSaleItemCount: number;
  totalOrders: number;
  completedOrderCount: number;
  totalWantedPosts: number;
  publishedAnnouncementCount: number;
  todayNewUsers: number;
  todayNewItems: number;
  todayNewOrders: number;
  todayCompletedAmount: number;
}

export interface OrderTrendPoint {
  date: string;
  createdOrderCount: number;
  completedOrderCount: number;
  cancelledOrderCount: number;
  completedAmount: number;
}

export interface ItemStatusCount {
  itemStatus: string;
  count: number;
}

export interface CategorySalesRanking {
  categoryId: number | null;
  categoryName: string | null;
  soldQuantity: number;
  completedOrderCount: number;
  completedAmount: number;
}

export interface UserGrowthPoint {
  date: string;
  newUserCount: number;
  cumulativeUserCount: number;
}

export interface RecentActivity {
  adminOperationLogId: number | null;
  adminId: number | null;
  adminNo: string | null;
  adminName: string | null;
  targetType: string | null;
  targetId: number | null;
  operationType: string | null;
  operationDetail: string | null;
  createdAt: string;
}

export interface HotKeyword {
  keyword: string;
  searchCount: number;
  categoryId: number | null;
  categoryName: string | null;
}

