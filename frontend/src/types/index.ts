export interface User {
  id: string
  username: string
  email: string
  avatar: string
  bio: string
  followers: number
  following: number
  videoCount: number
  createdAt: string
}

export interface Video {
  id: string
  title: string
  description: string
  tags: string[]
  coverUrl: string
  videoUrl: string
  duration: number
  likeCount: number
  favoriteCount: number
  viewCount: number
  status: 'pending' | 'approved' | 'rejected'
  author: User
  createdAt: string
}

export interface Comment {
  id: string
  userId: string
  videoId: string
  content: string
  author: User
  createdAt: string
}

export interface Favorite {
  id: string
  userId: string
  videoId: string
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface VideoUploadRequest {
  title: string
  description: string
  tags: string[]
}

export interface DayInfo {
  date: string
  dayOfMonth: number
  hasVideo: boolean
  videoCount: number
  isStreakBroken: boolean
  isMostActive: boolean
}

export interface CheckInCalendar {
  yearMonth: string
  totalDays: number
  checkInDays: number
  currentStreak: number
  longestStreak: number
  days: DayInfo[]
}

export interface WatchProgress {
  id: string
  userId: string
  video: Video
  currentTime: number
  isCompleted: boolean
  updatedAt: string
}

export interface VideoMilestone {
  id: string
  videoId: string
  title: string
  description: string
  timestampSeconds: number
  sortOrder: number
  createdAt: string
}

export interface HotTag {
  id: string
  name: string
  videoCount: number
  viewCount: number
  trend: string
}

export interface NewAuthor {
  id: string
  username: string
  avatar: string
  bio: string
  videoCount: number
  followers: number
  createdAt: string
}

export interface TrendingVideo {
  id: string
  title: string
  coverUrl: string
  viewCount: number
  likeCount: number
  growthRate: number
  author: User
  tags: string[]
}

export interface MorningReport {
  hotTags: HotTag[]
  newAuthors: NewAuthor[]
  trendingVideos: TrendingVideo[]
  reportDate: string
}

export type CheckItemSeverity = 'success' | 'warning' | 'error' | 'info'

export interface PublishCheckItem {
  id: string
  title: string
  description: string
  severity: CheckItemSeverity
  suggestion: string
  icon: string
}

export interface PublishCheckResult {
  overallScore: number
  totalItems: number
  passedItems: number
  warningItems: number
  errorItems: number
  checks: PublishCheckItem[]
}
