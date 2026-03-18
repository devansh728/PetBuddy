// User Types
export interface UserProfile {
  userId: string;
  fullName: string;
  email: string;
  isVerified: boolean;
  isVolunteer?: boolean;
  latitude?: number;
  longitude?: number;
  createdAt?: string;
}

export interface Pet {
  petId: string;
  name: string;
  breed?: string;
  ageMonths?: number;
  imageUrl?: string;
  createdAt?: string;
}

// Social Types
export interface Post {
  postId: string;
  userId: string;
  authorUsername?: string;
  caption?: string;
  mediaUrl?: string;
  locationLat?: number;
  locationLon?: number;
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  createdAt?: string;
}

export interface FeedResult {
  posts: Post[];
  totalPages: number;
  hasMore: boolean;
}

export interface Notification {
  notificationId: string;
  actorUsername: string;
  notificationType: string;
  postId?: string;
  postCaption?: string;
  mediaUrl?: string;
  isRead: boolean;
  createdAt?: string;
}

export interface NotificationsResult {
  notifications: Notification[];
  totalPages: number;
  hasMore: boolean;
  unreadCount: number;
}

export interface LikeResult {
  isLiked: boolean;
  likeCount: number;
}

export interface UploadUrlResult {
  uploadUrl: string;
  mediaKey: string;
  expiresInSeconds: number;
}

// Rescue Types
export enum IncidentType {
  UNKNOWN = 'UNKNOWN',
  ACCIDENT = 'ACCIDENT',
  ABANDONED = 'ABANDONED',
  INJURED = 'INJURED',
  LOST = 'LOST',
  ABUSE = 'ABUSE',
}

export enum IncidentStatus {
  OPEN = 'OPEN',
  ASSIGNED = 'ASSIGNED',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
  CANCELLED = 'CANCELLED',
}

export interface Incident {
  incidentId: string;
  reporterId: string;
  latitude: number;
  longitude: number;
  type: IncidentType;
  status: IncidentStatus;
  description?: string;
  animalType?: string;
  imageUrl?: string;
  volunteersNotified: number;
  createdAt?: string;
}

// Input Types
export interface RegisterUserInput {
  fullName: string;
  email: string;
  fcmToken?: string;
}

export interface CreatePostInput {
  caption?: string;
  mediaKey: string;
  locationLat?: number;
  locationLon?: number;
}

export interface ReportIncidentInput {
  latitude: number;
  longitude: number;
  type: IncidentType;
  description?: string;
  animalType?: string;
  imageUrl?: string;
}
