import axios, { type AxiosInstance } from 'axios'
import type { Video, User, PageResponse, ApiResponse, Comment, CheckInCalendar, WatchProgress, VideoMilestone, MorningReport, VideoDraft, Tag, TagWithSynonyms, VideoAppeal } from '@/types'

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

  getDrafts(params: { page?: number; size?: number; userId?: number }) {
    return api.get<ApiResponse<PageResponse<VideoDraft>>>('/videos/drafts', { params })
  },

  getDraft(id: string, userId?: number) {
    return api.get<ApiResponse<VideoDraft>>(`/videos/drafts/${id}`, {
      params: { userId }
    })
  },

  getDraftCount(userId?: number) {
    return api.get<ApiResponse<number>>('/videos/drafts/count', {
      params: { userId }
    })
  },

  createDraft(data: {
    title?: string
    description?: string
    tags?: string[]
    duration?: number
    userId?: number
  }) {
    const formData = new FormData()
    if (data.title) formData.append('title', data.title)
    if (data.description) formData.append('description', data.description)
    if (data.tags) {
      data.tags.forEach((tag, index) => {
        formData.append(`tags[${index}]`, tag)
      })
    }
    if (data.duration) formData.append('duration', data.duration.toString())
    if (data.userId) formData.append('userId', data.userId.toString())
    return api.post<ApiResponse<VideoDraft>>('/videos/drafts', formData)
  },

  updateDraft(id: string, data: {
    title?: string
    description?: string
    tags?: string[]
    duration?: number
    userId?: number
  }) {
    const formData = new FormData()
    if (data.title) formData.append('title', data.title)
    if (data.description) formData.append('description', data.description)
    if (data.tags) {
      data.tags.forEach((tag, index) => {
        formData.append(`tags[${index}]`, tag)
      })
    }
    if (data.duration) formData.append('duration', data.duration.toString())
    if (data.userId) formData.append('userId', data.userId.toString())
    return api.put<ApiResponse<VideoDraft>>(`/videos/drafts/${id}`, formData)
  },

  uploadDraftVideo(draftId: string, file: File, userId?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (userId) formData.append('userId', userId.toString())
    return api.post<ApiResponse<VideoDraft>>(`/videos/drafts/${draftId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  saveDraft(data: {
    draftId?: string
    title?: string
    description?: string
    tags?: string[]
    duration?: number
    file?: File
    coverFile?: File
    userId?: number
  }) {
    const formData = new FormData()
    if (data.draftId) formData.append('draftId', data.draftId)
    if (data.title) formData.append('title', data.title)
    if (data.description) formData.append('description', data.description)
    if (data.tags) {
      data.tags.forEach((tag, index) => {
        formData.append(`tags[${index}]`, tag)
      })
    }
    if (data.duration) formData.append('duration', data.duration.toString())
    if (data.file) formData.append('file', data.file)
    if (data.coverFile) formData.append('coverFile', data.coverFile)
    if (data.userId) formData.append('userId', data.userId.toString())
    return api.post<ApiResponse<VideoDraft>>('/videos/drafts/save', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  deleteDraft(id: string, userId?: number) {
    return api.delete<ApiResponse<void>>(`/videos/drafts/${id}`, {
      params: { userId }
    })
  },

  publishDraft(id: string, userId?: number) {
    return api.post<ApiResponse<{ id: string; status: string }>>(`/videos/drafts/${id}/publish`, null, {
      params: { userId }
    })
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
  },

  submitAppeal(videoId: string, data: {
    userId: number
    appealType: string
    content: string
  }) {
    return api.post<ApiResponse<VideoAppeal>>(`/videos/${videoId}/appeals`, data)
  },

  getVideoAppeals(videoId: string, params?: { page?: number; size?: number }) {
    return api.get<ApiResponse<PageResponse<VideoAppeal>>>(`/videos/${videoId}/appeals`, { params })
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

  getUserAllVideos(userId: string) {
    return api.get<ApiResponse<Video[]>>(`/users/${userId}/videos/all`)
  },

  getUserFavorites(userId: string) {
    return api.get<ApiResponse<Video[]>>(`/users/${userId}/favorites`)
  },

  getUserAppeals(userId: string, params?: { page?: number; size?: number }) {
    return api.get<ApiResponse<PageResponse<VideoAppeal>>>(`/users/${userId}/appeals`, { params })
  }
}

export const adminApi = {
  getPendingVideos(params: { page?: number; size?: number; status?: string }) {
    return api.get<ApiResponse<PageResponse<Video>>>('/admin/videos', { params })
  },

  updateVideoStatus(id: string, status: 'approved' | 'rejected') {
    return api.put<ApiResponse<void>>(`/admin/videos/${id}/status`, { status })
  },

  getAppeals(params: { page?: number; size?: number; status?: string }) {
    return api.get<ApiResponse<PageResponse<VideoAppeal>>>('/admin/appeals', { params })
  },

  getPendingAppealCount() {
    return api.get<ApiResponse<number>>('/admin/appeals/stats')
  },

  getAppealDetail(id: string) {
    return api.get<ApiResponse<VideoAppeal>>(`/admin/appeals/${id}`)
  },

  reviewAppeal(id: string, data: {
    reviewResult: 'upheld' | 'rejected'
    reviewComment: string
  }) {
    return api.put<ApiResponse<VideoAppeal>>(`/admin/appeals/${id}/review`, data)
  }
}

export const tagApi = {
  getAllTags() {
    return api.get<ApiResponse<TagWithSynonyms[]>>('/tags')
  },

  getCanonicalTags() {
    return api.get<ApiResponse<Tag[]>>('/tags/canonical')
  },

  searchTags(keyword: string) {
    return api.get<ApiResponse<Tag[]>>('/tags/search', { params: { keyword } })
  },

  getTagWithSynonyms(id: string) {
    return api.get<ApiResponse<TagWithSynonyms>>(`/tags/${id}/synonyms`)
  },

  getCanonicalTag(name: string) {
    return api.get<ApiResponse<Tag>>('/tags/canonicalize', { params: { name } })
  },

  addSynonym(canonicalName: string, synonymName: string) {
    return api.post<ApiResponse<any>>('/tags/synonyms', { canonicalName, synonymName })
  },

  removeSynonym(synonymTagId: string) {
    return api.delete<ApiResponse<void>>(`/tags/synonyms/${synonymTagId}`)
  },

  mergeTags(sourceTagId: string, targetTagId: string) {
    return api.post<ApiResponse<void>>('/tags/merge', { sourceTagId, targetTagId })
  }
}

export default api
