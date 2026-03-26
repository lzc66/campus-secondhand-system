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
  userCount: number;
  itemCount: number;
  orderCount: number;
  wantedPostCount: number;
  announcementCount: number;
  pendingRegistrationCount: number;
}

export interface DemoModeStatus {
  demoModeEnabled: boolean;
  demoItemNotesEnabled: boolean;
  demoDataSeeded: boolean;
  demoDataSeededAt?: string;
  demoSummary: DemoDataSummary;
}

export interface DemoDataSeedResult {
  createdCounts: DemoDataSummary;
  totalCounts: DemoDataSummary;
  demoDataSeeded: boolean;
  demoDataSeededAt?: string;
}