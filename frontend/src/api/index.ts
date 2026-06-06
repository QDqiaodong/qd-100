import axios, { type AxiosInstance } from 'axios'
import type { Video, User, PageResponse, ApiResponse, Comment, CheckInCalendar, WatchProgress, VideoMilestone, MorningReport } from '@/types'

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const videoApi = {
  getVideos(params: { page?: number; size?: number; tag?: string; sort?: string }) {
    return api.get<ApiResponse<PageResponse<Video>>>('/videos', { params })
  },

  getMorningReport() {
    return api.get<ApiResponse<MorningReport>>('/videos/morning-report')
  },

  getVideo(id: string) {
    return api.get<ApiResponse<Video>>(`/videos/${id}`)
  },

  uploadVideo(formData: FormData) {
    return api.post<ApiResponse<{ id: string; status: string }>>('/videos', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  updateVideo(id: string, data: { title?: string; description?: string }) {
    return api.put<ApiResponse<Video>>(`/videos/${id}`, data)
  },

  deleteVideo(id: string) {
    return api.delete<ApiResponse<void>>(`/videos/${id}`)
  },

  likeVideo(id: string) {
    return api.post<ApiResponse<{ liked: boolean; likeCount: number }>>(`/videos/${id}/like`)
  },

  favoriteVideo(id: string) {
    return api.post<ApiResponse<{ favorited: boolean; favoriteCount: number }>>(`/videos/${id}/favorite`)
  },

  getComments(videoId: string) {
    return api.get<ApiResponse<Comment[]>>(`/videos/${videoId}/comments`)
  },

  addComment(videoId: string, content: string) {
    return api.post<ApiResponse<Comment>>(`/videos/${videoId}/comments`, { content })
  },

  getCheckInCalendar(userId: string, params?: { year?: number; month?: number }) {
    return api.get<ApiResponse<CheckInCalendar>>(`/videos/calendar/${userId}`, { params })
  },

  getUserVideosByDate(userId: string, date: string) {
    return api.get<ApiResponse<Video[]>>(`/videos/user/${userId}/date/${date}`)
  },

  updateWatchProgress(videoId: string, userId: string, currentTime: number) {
    return api.post<ApiResponse<WatchProgress>>(`/videos/${videoId}/watch-progress`, {
      userId,
      currentTime
    })
  },

  getWatchProgress(videoId: string, userId: string) {
    return api.get<ApiResponse<WatchProgress>>(`/videos/${videoId}/watch-progress`, {
      params: { userId }
    })
  },

  getContinueWatchingVideos(userId: string) {
    return api.get<ApiResponse<WatchProgress[]>>('/videos/continue-watching', {
      params: { userId }
    })
  },

  getVideoMilestones(videoId: string) {
    return api.get<ApiResponse<VideoMilestone[]>>(`/videos/${videoId}/milestones`)
  },

  createVideoMilestone(videoId: string, data: {
    title: string
    description?: string
    timestampSeconds: number
    sortOrder?: number
  }) {
    return api.post<ApiResponse<VideoMilestone>>(`/videos/${videoId}/milestones`, data)
  },

  updateVideoMilestone(milestoneId: string, data: {
    title?: string
    description?: string
    timestampSeconds?: number
    sortOrder?: number
  }) {
    return api.put<ApiResponse<VideoMilestone>>(`/videos/milestones/${milestoneId}`, data)
  },

  deleteVideoMilestone(milestoneId: string) {
    return api.delete<ApiResponse<void>>(`/videos/milestones/${milestoneId}`)
  }
}

export const userApi = {
  getCurrentUser() {
    return api.get<ApiResponse<User>>('/users/me')
  },

  updateUser(data: { username?: string; bio?: string }) {
    return api.put<ApiResponse<User>>('/users/me', data)
  },

  getUser(id: string) {
    return api.get<ApiResponse<User>>(`/users/${id}`)
  },

  getUserVideos(userId: string) {
    return api.get<ApiResponse<Video[]>>(`/users/${userId}/videos`)
  },

  getUserFavorites(userId: string) {
    return api.get<ApiResponse<Video[]>>(`/users/${userId}/favorites`)
  }
}

export const adminApi = {
  getPendingVideos(params: { page?: number; size?: number; status?: string }) {
    return api.get<ApiResponse<PageResponse<Video>>>('/admin/videos', { params })
  },

  updateVideoStatus(id: string, status: 'approved' | 'rejected') {
    return api.put<ApiResponse<void>>(`/admin/videos/${id}/status`, { status })
  }
}

export default api
