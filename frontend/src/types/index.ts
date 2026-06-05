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
