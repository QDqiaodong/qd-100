<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import {
  CheckCircle, XCircle, Eye, Play, LayoutGrid, Users, BarChart,
  Search, Filter, ChevronLeft, ChevronRight, Clock, Tag,
  SkipBack, SkipForward, Keyboard, Zap, AlertCircle, Pause,
  Volume2, VolumeX, User, Calendar, Hash, Plus, X,
  FileText, MessageSquare, ThumbsUp, ThumbsDown
} from 'lucide-vue-next'
import Navbar from '@/components/Navbar.vue'
import { adminApi, tagApi } from '@/api'
import type { Video, PageResponse, TagWithSynonyms, VideoAppeal } from '@/types'

const videos = ref<Video[]>([])
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const activeStatus = ref<'pending' | 'approved' | 'rejected'>('pending')
const stats = ref({
  total: 128,
  pending: 24,
  approved: 89,
  rejected: 15
})

const searchQuery = ref('')
const selectedTags = ref<string[]>([])
const durationFilter = ref<'all' | 'short' | 'medium' | 'long'>('all')
const showFilterPanel = ref(false)
const reviewMode = ref(false)
const currentReviewIndex = ref(-1)
const isPlaying = ref(false)
const isMuted = ref(false)
const videoRef = ref<HTMLVideoElement | null>(null)
const currentTime = ref(0)
const duration = ref(0)
const showShortcuts = ref(false)
const autoNext = ref(true)
const actionInProgress = ref(false)

const activeTab = ref<'videos' | 'tags' | 'appeals'>('videos')

const tagList = ref<TagWithSynonyms[]>([])
const tagsLoading = ref(false)
const showAddSynonymModal = ref(false)
const selectedCanonicalTag = ref<TagWithSynonyms | null>(null)
const newSynonymName = ref('')
const tagSearchQuery = ref('')

const appeals = ref<VideoAppeal[]>([])
const appealsLoading = ref(false)
const appealsPage = ref(0)
const appealsHasMore = ref(true)
const appealStatus = ref<'all' | 'pending' | 'reviewed'>('all')
const selectedAppeal = ref<VideoAppeal | null>(null)
const showAppealDetail = ref(false)
const showReviewModal = ref(false)
const reviewResult = ref<'upheld' | 'rejected'>('upheld')
const reviewComment = ref('')
const reviewInProgress = ref(false)

function fetchTags() {
  tagsLoading.value = true
  tagApi.getAllTags().then(res => {
    tagList.value = res.data.data
    tagsLoading.value = false
  }).catch(() => {
    tagsLoading.value = false
  })
}

const filteredTagList = computed(() => {
  if (!tagSearchQuery.value.trim()) return tagList.value
  const query = tagSearchQuery.value.toLowerCase()
  return tagList.value.filter(tag =>
    tag.name.toLowerCase().includes(query) ||
    tag.synonyms.some(s => s.name.toLowerCase().includes(query))
  )
})

function openAddSynonymModal(tag: TagWithSynonyms) {
  selectedCanonicalTag.value = tag
  newSynonymName.value = ''
  showAddSynonymModal.value = true
}

function closeAddSynonymModal() {
  showAddSynonymModal.value = false
  selectedCanonicalTag.value = null
  newSynonymName.value = ''
}

function addSynonym() {
  if (!selectedCanonicalTag.value || !newSynonymName.value.trim()) return
  
  tagApi.addSynonym(selectedCanonicalTag.value.name, newSynonymName.value.trim())
    .then(() => {
      fetchTags()
      closeAddSynonymModal()
    })
    .catch(err => {
      alert('添加失败：' + (err.response?.data?.message || err.message))
    })
}

function removeSynonym(synonymId: string) {
  if (!confirm('确定要移除此同义词吗？')) return
  
  tagApi.removeSynonym(synonymId).then(() => {
    fetchTags()
  }).catch(err => {
    alert('移除失败：' + (err.response?.data?.message || err.message))
  })
}

function fetchAppeals() {
  if (appealsLoading.value || !appealsHasMore.value) return

  appealsLoading.value = true
  const params: Record<string, any> = {
    page: appealsPage.value,
    size: 20
  }
  if (appealStatus.value !== 'all') {
    params.status = appealStatus.value
  }

  adminApi.getAppeals(params).then(res => {
    const data = res.data.data as PageResponse<VideoAppeal>
    if (data.content.length === 0) {
      appealsHasMore.value = false
    } else {
      appeals.value = [...appeals.value, ...data.content]
      appealsPage.value++
    }
    appealsLoading.value = false
  }).catch(() => {
    appealsLoading.value = false
  })
}

function openAppealDetail(appeal: VideoAppeal) {
  selectedAppeal.value = appeal
  showAppealDetail.value = true
}

function closeAppealDetail() {
  showAppealDetail.value = false
  selectedAppeal.value = null
}

function openReviewModal(appeal: VideoAppeal) {
  selectedAppeal.value = appeal
  reviewResult.value = 'upheld'
  reviewComment.value = ''
  showReviewModal.value = true
}

function closeReviewModal() {
  showReviewModal.value = false
  selectedAppeal.value = null
  reviewComment.value = ''
}

function submitReview() {
  if (!selectedAppeal.value || !reviewComment.value.trim() || reviewInProgress.value) return

  reviewInProgress.value = true

  adminApi.reviewAppeal(selectedAppeal.value.id, {
    reviewResult: reviewResult.value,
    reviewComment: reviewComment.value.trim()
  }).then(() => {
    alert('复核完成')
    appeals.value = appeals.value.filter(a => a.id !== selectedAppeal.value?.id)
    closeReviewModal()
    closeAppealDetail()
    stats.value.pending--
    stats.value.approved++
  }).catch(err => {
    alert('复核失败：' + (err.response?.data?.message || err.message))
  }).finally(() => {
    reviewInProgress.value = false
  })
}

function getAppealTypeLabel(type: string): string {
  const map: Record<string, string> = {
    supplement: '补充说明',
    explain: '解释意图',
    review: '申请复核'
  }
  return map[type] || type
}

function getAppealStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    reviewed: '已处理'
  }
  return map[status] || status
}

function getAppealResultLabel(result?: string): string {
  if (!result) return '-'
  const map: Record<string, string> = {
    upheld: '申诉成立',
    rejected: '驳回申诉'
  }
  return map[result] || result
}

function getAppealStatusClass(status: string): string {
  const map: Record<string, string> = {
    pending: 'bg-yellow-100 text-yellow-700',
    reviewed: 'bg-green-100 text-green-700'
  }
  return map[status] || 'bg-gray-100 text-gray-700'
}

const allTags = computed(() => {
  const tagSet = new Set<string>()
  videos.value.forEach(v => (v.tags ?? []).forEach(t => tagSet.add(t)))
  return Array.from(tagSet)
})

const filteredVideos = computed(() => {
  let result = videos.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(v =>
      v.title.toLowerCase().includes(query) ||
      (v.author?.username ?? '').toLowerCase().includes(query) ||
      (v.description ?? '').toLowerCase().includes(query)
    )
  }

  if (selectedTags.value.length > 0) {
    result = result.filter(v =>
      selectedTags.value.some(tag => v.tags.includes(tag))
    )
  }

  if (durationFilter.value !== 'all') {
    result = result.filter(v => {
      if (durationFilter.value === 'short') return v.duration < 60
      if (durationFilter.value === 'medium') return v.duration >= 60 && v.duration < 180
      if (durationFilter.value === 'long') return v.duration >= 180
      return true
    })
  }

  return result
})

const currentReviewVideo = computed(() => {
  if (currentReviewIndex.value >= 0 && currentReviewIndex.value < filteredVideos.value.length) {
    return filteredVideos.value[currentReviewIndex.value]
  }
  return null
})

const hasPrev = computed(() => currentReviewIndex.value > 0)
const hasNext = computed(() => currentReviewIndex.value < filteredVideos.value.length - 1)

function fetchVideos() {
  if (loading.value || !hasMore.value) return

  loading.value = true
  adminApi.getPendingVideos({
    page: page.value,
    size: 20,
    status: activeStatus.value
  }).then(res => {
    const data = res.data.data as PageResponse<Video>
    if (data.content.length === 0) {
      hasMore.value = false
    } else {
      videos.value = [...videos.value, ...data.content]
      page.value++
    }
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function approveVideo(id: string) {
  if (actionInProgress.value) return
  actionInProgress.value = true

  adminApi.updateVideoStatus(id, 'approved').then(() => {
    videos.value = videos.value.filter(v => v.id !== id)
    stats.value.pending--
    stats.value.approved++

    if (reviewMode.value && autoNext.value) {
      nextTick(() => {
        if (currentReviewIndex.value >= filteredVideos.value.length) {
          currentReviewIndex.value = filteredVideos.value.length - 1
        }
        if (filteredVideos.value.length === 0) {
          reviewMode.value = false
          currentReviewIndex.value = -1
        }
        actionInProgress.value = false
      })
    } else {
      actionInProgress.value = false
    }
  }).catch(() => {
    actionInProgress.value = false
  })
}

function rejectVideo(id: string) {
  if (actionInProgress.value) return
  actionInProgress.value = true

  adminApi.updateVideoStatus(id, 'rejected').then(() => {
    videos.value = videos.value.filter(v => v.id !== id)
    stats.value.pending--
    stats.value.rejected++

    if (reviewMode.value && autoNext.value) {
      nextTick(() => {
        if (currentReviewIndex.value >= filteredVideos.value.length) {
          currentReviewIndex.value = filteredVideos.value.length - 1
        }
        if (filteredVideos.value.length === 0) {
          reviewMode.value = false
          currentReviewIndex.value = -1
        }
        actionInProgress.value = false
      })
    } else {
      actionInProgress.value = false
    }
  }).catch(() => {
    actionInProgress.value = false
  })
}

function openReview(video: Video) {
  const idx = filteredVideos.value.findIndex(v => v.id === video.id)
  if (idx !== -1) {
    currentReviewIndex.value = idx
    reviewMode.value = true
    nextTick(() => {
      playVideo()
    })
  }
}

function closeReview() {
  pauseVideo()
  reviewMode.value = false
  currentReviewIndex.value = -1
}

function prevVideo() {
  if (hasPrev.value) {
    pauseVideo()
    currentReviewIndex.value--
    nextTick(() => {
      playVideo()
    })
  }
}

function nextVideo() {
  if (hasNext.value) {
    pauseVideo()
    currentReviewIndex.value++
    nextTick(() => {
      playVideo()
    })
  }
}

function playVideo() {
  if (videoRef.value) {
    videoRef.value.play().catch(() => {})
    isPlaying.value = true
  }
}

function pauseVideo() {
  if (videoRef.value) {
    videoRef.value.pause()
    isPlaying.value = false
  }
}

function togglePlay() {
  if (isPlaying.value) {
    pauseVideo()
  } else {
    playVideo()
  }
}

function toggleMute() {
  if (videoRef.value) {
    videoRef.value.muted = !videoRef.value.muted
    isMuted.value = videoRef.value.muted
  }
}

function handleTimeUpdate() {
  if (videoRef.value) {
    currentTime.value = videoRef.value.currentTime
  }
}

function handleLoadedMetadata() {
  if (videoRef.value) {
    duration.value = videoRef.value.duration
  }
}

function handleVideoEnded() {
  if (autoNext.value && hasNext.value) {
    nextVideo()
  } else {
    isPlaying.value = false
  }
}

function toggleTag(tag: string) {
  const idx = selectedTags.value.indexOf(tag)
  if (idx === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(idx, 1)
  }
}

function clearFilters() {
  searchQuery.value = ''
  selectedTags.value = []
  durationFilter.value = 'all'
}

function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

function handleKeydown(e: KeyboardEvent) {
  if (!reviewMode.value) return

  switch (e.key) {
    case 'ArrowLeft':
    case 'j':
    case 'J':
      e.preventDefault()
      prevVideo()
      break
    case 'ArrowRight':
    case 'k':
    case 'K':
      e.preventDefault()
      nextVideo()
      break
    case ' ':
      e.preventDefault()
      togglePlay()
      break
    case 'a':
    case 'A':
      e.preventDefault()
      if (currentReviewVideo.value) {
        approveVideo(currentReviewVideo.value.id)
      }
      break
    case 'd':
    case 'D':
      e.preventDefault()
      if (currentReviewVideo.value) {
        rejectVideo(currentReviewVideo.value.id)
      }
      break
    case 'Escape':
      e.preventDefault()
      closeReview()
      break
    case 'm':
    case 'M':
      e.preventDefault()
      toggleMute()
      break
    case '?':
      e.preventDefault()
      showShortcuts.value = !showShortcuts.value
      break
  }
}

watch(() => activeStatus.value, () => {
  videos.value = []
  page.value = 0
  hasMore.value = true
  currentReviewIndex.value = -1
  reviewMode.value = false
  clearFilters()
  fetchVideos()
})

watch(() => appealStatus.value, () => {
  appeals.value = []
  appealsPage.value = 0
  appealsHasMore.value = true
  fetchAppeals()
})

watch(() => activeTab.value, (newTab) => {
  if (newTab === 'appeals' && appeals.value.length === 0) {
    fetchAppeals()
  }
})

onMounted(() => {
  fetchVideos()
  fetchTags()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />

    <div class="pt-20 pb-10">
      <div class="max-w-6xl mx-auto px-4">
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-3">
            <LayoutGrid class="w-8 h-8 text-primary" />
            <h1 class="text-2xl font-bold text-gray-900">管理后台</h1>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg font-medium hover:bg-orange-600 transition-colors"
              @click="showShortcuts = !showShortcuts"
            >
              <Keyboard class="w-4 h-4" />
              <span class="text-sm">快捷键</span>
            </button>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm mb-6 p-1 inline-flex gap-1">
          <button
            class="px-6 py-2.5 rounded-lg font-medium transition-colors flex items-center gap-2"
            :class="activeTab === 'videos' ? 'bg-primary text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'"
            @click="activeTab = 'videos'"
          >
            <LayoutGrid class="w-4 h-4" />
            视频审核
          </button>
          <button
            class="px-6 py-2.5 rounded-lg font-medium transition-colors flex items-center gap-2"
            :class="activeTab === 'appeals' ? 'bg-primary text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'"
            @click="activeTab = 'appeals'"
          >
            <FileText class="w-4 h-4" />
            申诉管理
          </button>
          <button
            class="px-6 py-2.5 rounded-lg font-medium transition-colors flex items-center gap-2"
            :class="activeTab === 'tags' ? 'bg-primary text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'"
            @click="activeTab = 'tags'"
          >
            <Tag class="w-4 h-4" />
            标签管理
          </button>
        </div>

        <div v-if="activeTab === 'videos'">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div class="bg-white rounded-xl p-4 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
                <BarChart class="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <p class="text-2xl font-bold text-gray-900">{{ stats.total }}</p>
                <p class="text-sm text-gray-500">总视频数</p>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-xl p-4 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 bg-yellow-100 rounded-xl flex items-center justify-center">
                <Eye class="w-6 h-6 text-yellow-600" />
              </div>
              <div>
                <p class="text-2xl font-bold text-gray-900">{{ stats.pending }}</p>
                <p class="text-sm text-gray-500">待审核</p>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-xl p-4 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center">
                <CheckCircle class="w-6 h-6 text-green-600" />
              </div>
              <div>
                <p class="text-2xl font-bold text-gray-900">{{ stats.approved }}</p>
                <p class="text-sm text-gray-500">已通过</p>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-xl p-4 shadow-sm">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
                <XCircle class="w-6 h-6 text-red-600" />
              </div>
              <div>
                <p class="text-2xl font-bold text-gray-900">{{ stats.rejected }}</p>
                <p class="text-sm text-gray-500">已拒绝</p>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-lg overflow-hidden">
          <div class="flex border-b border-gray-100">
            <button
              class="flex-1 py-4 text-center font-medium transition-colors relative"
              :class="activeStatus === 'pending' ? 'text-primary' : 'text-gray-500 hover:text-gray-700'"
              @click="activeStatus = 'pending'"
            >
              <Eye class="w-5 h-5 mx-auto mb-1" />
              <span>待审核</span>
              <div
                v-if="activeStatus === 'pending'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"
              />
            </button>
            <button
              class="flex-1 py-4 text-center font-medium transition-colors relative"
              :class="activeStatus === 'approved' ? 'text-green-600' : 'text-gray-500 hover:text-gray-700'"
              @click="activeStatus = 'approved'"
            >
              <CheckCircle class="w-5 h-5 mx-auto mb-1" />
              <span>已通过</span>
              <div
                v-if="activeStatus === 'approved'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-green-600"
              />
            </button>
            <button
              class="flex-1 py-4 text-center font-medium transition-colors relative"
              :class="activeStatus === 'rejected' ? 'text-red-600' : 'text-gray-500 hover:text-gray-700'"
              @click="activeStatus = 'rejected'"
            >
              <XCircle class="w-5 h-5 mx-auto mb-1" />
              <span>已拒绝</span>
              <div
                v-if="activeStatus === 'rejected'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-red-600"
              />
            </button>
          </div>

          <div class="p-4 border-b border-gray-100 bg-gray-50">
            <div class="flex flex-wrap items-center gap-3">
              <div class="flex-1 min-w-[200px] relative">
                <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="搜索标题、作者或描述..."
                  class="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary bg-white"
                />
              </div>

              <button
                class="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-medium transition-colors bg-white"
                :class="showFilterPanel ? 'bg-primary/10 border-primary/30 text-primary' : 'text-gray-600 hover:bg-gray-100'"
                @click="showFilterPanel = !showFilterPanel"
              >
                <Filter class="w-4 h-4" />
                <span>筛选</span>
                <span
                  v-if="selectedTags.length > 0 || durationFilter !== 'all'"
                  class="w-5 h-5 bg-primary text-white text-xs rounded-full flex items-center justify-center"
                >
                  {{ selectedTags.length + (durationFilter !== 'all' ? 1 : 0) }}
                </span>
              </button>

              <button
                v-if="activeStatus === 'pending' && filteredVideos.length > 0"
                class="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition-colors"
                @click="openReview(filteredVideos[0])"
              >
                <Zap class="w-4 h-4" />
                <span>密集审核</span>
              </button>

              <div
                v-if="(selectedTags.length > 0 || searchQuery || durationFilter !== 'all')"
                class="flex items-center gap-2"
              >
                <span class="text-sm text-gray-500">
                  筛选结果: {{ filteredVideos.length }} 条
                </span>
                <button
                  class="text-sm text-primary hover:underline"
                  @click="clearFilters"
                >
                  清除筛选
                </button>
              </div>
            </div>

            <div
              v-if="showFilterPanel"
              class="mt-4 pt-4 border-t border-gray-200 space-y-4"
            >
              <div>
                <div class="flex items-center gap-2 mb-2">
                  <Clock class="w-4 h-4 text-gray-500" />
                  <span class="text-sm font-medium text-gray-700">视频时长</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="opt in [
                      { value: 'all', label: '全部' },
                      { value: 'short', label: '< 1分钟' },
                      { value: 'medium', label: '1-3分钟' },
                      { value: 'long', label: '> 3分钟' }
                    ]"
                    :key="opt.value"
                    class="px-3 py-1.5 rounded-full text-sm transition-colors"
                    :class="durationFilter === opt.value
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                    @click="durationFilter = opt.value as any"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              </div>

              <div v-if="allTags.length > 0">
                <div class="flex items-center gap-2 mb-2">
                  <Tag class="w-4 h-4 text-gray-500" />
                  <span class="text-sm font-medium text-gray-700">标签筛选</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="tag in allTags"
                    :key="tag"
                    class="px-3 py-1.5 rounded-full text-sm transition-colors flex items-center gap-1"
                    :class="selectedTags.includes(tag)
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                    @click="toggleTag(tag)"
                  >
                    <Hash class="w-3 h-3" />
                    {{ tag }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="divide-y divide-gray-100">
            <div
              v-for="video in filteredVideos"
              :key="video.id"
              class="flex items-center gap-4 p-4 hover:bg-gray-50 transition-colors cursor-pointer group"
              @click="openReview(video)"
            >
              <div class="relative w-32 h-18 bg-black rounded-lg overflow-hidden flex-shrink-0">
                <img
                  :src="video.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=landscape_4_3'"
                  :alt="video.title"
                  class="w-full h-full object-cover"
                />
                <button
                  class="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity"
                  @click.stop="openReview(video)"
                >
                  <Play class="w-8 h-8 text-white" />
                </button>
                <div class="absolute bottom-1 right-1 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded">
                  {{ formatDuration(video.duration) }}
                </div>
              </div>

              <div class="flex-1 min-w-0">
                <h3 class="font-medium text-gray-900 truncate">{{ video.title }}</h3>
                <div class="flex items-center gap-4 mt-1 text-sm text-gray-500">
                  <span class="flex items-center gap-1">
                    <Users class="w-4 h-4" />
                    {{ video.author?.username || '未知作者' }}
                  </span>
                  <span>{{ formatDate(video.createdAt) }}</span>
                </div>
                <div class="flex flex-wrap gap-1 mt-2">
                  <span
                    v-for="tag in (video.tags ?? []).slice(0, 3)"
                    :key="tag"
                    class="text-xs px-2 py-0.5 bg-gray-100 text-gray-500 rounded-full"
                  >
                    #{{ tag }}
                  </span>
                </div>
              </div>

              <div class="flex gap-2" @click.stop>
                <button
                  v-if="activeStatus === 'pending'"
                  class="px-4 py-2 bg-green-50 text-green-600 rounded-lg font-medium hover:bg-green-100 transition-colors flex items-center gap-1"
                  @click="approveVideo(video.id)"
                >
                  <CheckCircle class="w-4 h-4" />
                  通过
                </button>
                <button
                  v-if="activeStatus === 'pending'"
                  class="px-4 py-2 bg-red-50 text-red-600 rounded-lg font-medium hover:bg-red-100 transition-colors flex items-center gap-1"
                  @click="rejectVideo(video.id)"
                >
                  <XCircle class="w-4 h-4" />
                  拒绝
                </button>
              </div>
            </div>
          </div>

          <div v-if="loading" class="flex justify-center py-8">
            <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          </div>

          <div v-if="!hasMore && filteredVideos.length > 0" class="text-center py-8 text-gray-500">
            已经到底了
          </div>

          <div v-if="filteredVideos.length === 0 && !loading" class="text-center py-16">
            <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
              <LayoutGrid class="w-10 h-10 text-gray-400" />
            </div>
            <p class="text-gray-500">暂无{{ activeStatus === 'pending' ? '待审核' : activeStatus === 'approved' ? '已通过' : '已拒绝' }}的视频</p>
          </div>
        </div>
        </div>

        <div v-if="activeTab === 'appeals'" class="space-y-4">
          <div class="bg-white rounded-xl shadow-sm p-4">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <FileText class="w-5 h-5 text-primary" />
                <h2 class="text-lg font-semibold text-gray-900">申诉工单管理</h2>
              </div>
              <div class="flex items-center gap-2">
                <div class="bg-yellow-100 text-yellow-700 px-3 py-1 rounded-full text-sm font-medium">
                  待处理 {{ stats.pending }} 件
                </div>
              </div>
            </div>

            <p class="text-sm text-gray-500 mb-4">
              处理作者提交的内容申诉，查看申诉详情并给出复核结论，形成可追踪的内容治理闭环。
            </p>
          </div>

          <div class="bg-white rounded-xl shadow-sm overflow-hidden">
            <div class="flex border-b border-gray-100">
              <button
                class="flex-1 py-4 text-center font-medium transition-colors relative"
                :class="appealStatus === 'all' ? 'text-primary' : 'text-gray-500 hover:text-gray-700'"
                @click="appealStatus = 'all'"
              >
                <FileText class="w-5 h-5 mx-auto mb-1" />
                <span>全部申诉</span>
                <div
                  v-if="appealStatus === 'all'"
                  class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"
                />
              </button>
              <button
                class="flex-1 py-4 text-center font-medium transition-colors relative"
                :class="appealStatus === 'pending' ? 'text-yellow-600' : 'text-gray-500 hover:text-gray-700'"
                @click="appealStatus = 'pending'"
              >
                <Clock class="w-5 h-5 mx-auto mb-1" />
                <span>待处理</span>
                <div
                  v-if="appealStatus === 'pending'"
                  class="absolute bottom-0 left-0 right-0 h-0.5 bg-yellow-500"
                />
              </button>
              <button
                class="flex-1 py-4 text-center font-medium transition-colors relative"
                :class="appealStatus === 'reviewed' ? 'text-green-600' : 'text-gray-500 hover:text-gray-700'"
                @click="appealStatus = 'reviewed'"
              >
                <CheckCircle class="w-5 h-5 mx-auto mb-1" />
                <span>已处理</span>
                <div
                  v-if="appealStatus === 'reviewed'"
                  class="absolute bottom-0 left-0 right-0 h-0.5 bg-green-500"
                />
              </button>
            </div>

            <div v-if="appealsLoading && appeals.length === 0" class="flex justify-center py-12">
              <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
            </div>

            <div v-else-if="appeals.length === 0" class="text-center py-16">
              <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
                <FileText class="w-10 h-10 text-gray-400" />
              </div>
              <p class="text-gray-500">暂无申诉数据</p>
            </div>

            <div v-else class="divide-y divide-gray-100">
              <div
                v-for="appeal in appeals"
                :key="appeal.id"
                class="p-4 hover:bg-gray-50 transition-colors cursor-pointer"
                @click="openAppealDetail(appeal)"
              >
                <div class="flex items-start gap-4">
                  <div class="relative w-20 h-14 bg-black rounded-lg overflow-hidden flex-shrink-0">
                    <img
                      :src="appeal.video?.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=landscape_4_3'"
                      :alt="appeal.video?.title"
                      class="w-full h-full object-cover"
                    />
                  </div>

                  <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between gap-2 mb-1">
                      <h3 class="font-medium text-gray-900 truncate">{{ appeal.video?.title || '未知视频' }}</h3>
                      <span
                        class="text-xs px-2 py-0.5 rounded-full font-medium flex-shrink-0"
                        :class="getAppealStatusClass(appeal.status)"
                      >
                        {{ getAppealStatusLabel(appeal.status) }}
                      </span>
                    </div>

                    <div class="flex items-center gap-3 text-sm text-gray-500 mb-2">
                      <span class="flex items-center gap-1">
                        <User class="w-4 h-4" />
                        {{ appeal.user?.username || '未知作者' }}
                      </span>
                      <span class="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded">
                        {{ getAppealTypeLabel(appeal.appealType) }}
                      </span>
                    </div>

                    <p class="text-sm text-gray-600 line-clamp-2">{{ appeal.content }}</p>

                    <div v-if="appeal.status === 'reviewed'" class="mt-2 pt-2 border-t border-gray-100">
                      <div class="flex items-center gap-2 text-sm">
                        <span class="text-gray-500">复核结果：</span>
                        <span
                          :class="appeal.reviewResult === 'upheld' ? 'text-green-600' : 'text-red-600'"
                          class="font-medium"
                        >
                          {{ getAppealResultLabel(appeal.reviewResult) }}
                        </span>
                      </div>
                    </div>

                    <div class="flex items-center justify-between mt-2">
                      <span class="text-xs text-gray-400">
                        提交时间：{{ formatDate(appeal.createdAt) }}
                      </span>
                      <button
                        class="text-primary text-sm hover:underline"
                        @click.stop="openAppealDetail(appeal)"
                      >
                        查看详情
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="appealsLoading" class="flex justify-center py-6">
              <div class="w-6 h-6 border-3 border-primary border-t-transparent rounded-full animate-spin" />
            </div>

            <div v-if="!appealsHasMore && appeals.length > 0" class="text-center py-6 text-gray-500 text-sm">
              已经到底了
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'tags'" class="space-y-4">
          <div class="bg-white rounded-xl shadow-sm p-4">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <Tag class="w-5 h-5 text-primary" />
                <h2 class="text-lg font-semibold text-gray-900">标签同义词管理</h2>
              </div>
              <div class="flex-1 max-w-xs ml-4">
                <div class="relative">
                  <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    v-model="tagSearchQuery"
                    type="text"
                    placeholder="搜索标签..."
                    class="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
              </div>
            </div>

            <p class="text-sm text-gray-500 mb-4">
              将语义相近的标签归并到统一的主标签下，提升分类推荐的一致性和浏览命中率。
            </p>
          </div>

          <div v-if="tagsLoading" class="flex justify-center py-12">
            <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          </div>

          <div v-else-if="filteredTagList.length === 0" class="bg-white rounded-xl shadow-sm p-12 text-center">
            <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
              <Tag class="w-10 h-10 text-gray-400" />
            </div>
            <p class="text-gray-500">暂无标签数据</p>
          </div>

          <div v-else class="grid gap-4">
            <div
              v-for="tag in filteredTagList"
              :key="tag.id"
              class="bg-white rounded-xl shadow-sm p-5 hover:shadow-md transition-shadow"
            >
              <div class="flex items-start justify-between mb-3">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
                    <Hash class="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <h3 class="font-semibold text-gray-900 text-lg">{{ tag.name }}</h3>
                    <p class="text-xs text-gray-500">主标签</p>
                  </div>
                </div>
                <button
                  class="flex items-center gap-1.5 px-3 py-1.5 text-sm text-primary hover:bg-primary/10 rounded-lg transition-colors"
                  @click="openAddSynonymModal(tag)"
                >
                  <Plus class="w-4 h-4" />
                  添加同义词
                </button>
              </div>

              <div class="border-t border-gray-100 pt-3">
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-xs font-medium text-gray-500">同义词 ({{ tag.synonyms.length }})</span>
                </div>
                <div v-if="tag.synonyms.length === 0" class="text-sm text-gray-400">
                  暂无同义词
                </div>
                <div v-else class="flex flex-wrap gap-2">
                  <div
                    v-for="synonym in tag.synonyms"
                    :key="synonym.id"
                    class="group flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 text-gray-700 rounded-full text-sm"
                  >
                    <span>#{{ synonym.name }}</span>
                    <button
                      class="opacity-0 group-hover:opacity-100 transition-opacity text-gray-400 hover:text-red-500"
                      @click="removeSynonym(synonym.id)"
                    >
                      <X class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="reviewMode && currentReviewVideo"
      class="fixed inset-0 bg-black/95 z-50 flex flex-col"
    >
      <div class="flex items-center justify-between px-6 py-4 border-b border-white/10">
        <div class="flex items-center gap-4">
          <button
            class="w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
            @click="closeReview"
          >
            <ChevronLeft class="w-5 h-5 text-white" />
          </button>
          <div>
            <h2 class="text-white font-semibold text-lg">密集审核模式</h2>
            <p class="text-white/50 text-sm">
              {{ currentReviewIndex + 1 }} / {{ filteredVideos.length }}
              <span class="mx-2">·</span>
              剩余 {{ filteredVideos.length - currentReviewIndex - 1 }} 条待审
            </p>
          </div>
        </div>

        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <span class="text-white/70 text-sm">自动下一条</span>
            <button
              class="w-12 h-6 rounded-full transition-colors relative"
              :class="autoNext ? 'bg-primary' : 'bg-white/20'"
              @click="autoNext = !autoNext"
            >
              <div
                class="absolute top-0.5 w-5 h-5 bg-white rounded-full transition-all"
                :class="autoNext ? 'left-6' : 'left-0.5'"
              />
            </button>
          </div>
          <button
            class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/10 text-white/70 text-sm hover:bg-white/20 transition-colors"
            @click="showShortcuts = !showShortcuts"
          >
            <Keyboard class="w-4 h-4" />
            <span>快捷键</span>
          </button>
        </div>
      </div>

      <div class="flex-1 flex overflow-hidden">
        <div class="flex-1 flex items-center justify-center p-6">
          <div class="relative w-full max-w-4xl aspect-video bg-black rounded-xl overflow-hidden">
            <video
              ref="videoRef"
              :src="currentReviewVideo.videoUrl"
              class="w-full h-full object-contain"
              @timeupdate="handleTimeUpdate"
              @loadedmetadata="handleLoadedMetadata"
              @ended="handleVideoEnded"
            />

            <div
              v-if="!isPlaying"
              class="absolute inset-0 flex items-center justify-center bg-black/30 cursor-pointer"
              @click="togglePlay"
            >
              <div class="w-20 h-20 rounded-full bg-white/90 flex items-center justify-center hover:scale-110 transition-transform">
                <Play class="w-10 h-10 text-primary ml-1" />
              </div>
            </div>

            <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 via-black/50 to-transparent p-4">
              <div class="flex items-center gap-4">
                <button
                  class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                  @click="togglePlay"
                >
                  <Pause v-if="isPlaying" class="w-5 h-5 text-white" />
                  <Play v-else class="w-5 h-5 text-white ml-0.5" />
                </button>

                <div class="flex-1 text-white text-sm">
                  {{ formatDuration(currentTime) }} / {{ formatDuration(duration) }}
                </div>

                <button
                  class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                  @click="toggleMute"
                >
                  <VolumeX v-if="isMuted" class="w-5 h-5 text-white" />
                  <Volume2 v-else class="w-5 h-5 text-white" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="w-96 border-l border-white/10 bg-white/5 flex flex-col">
          <div class="p-6 border-b border-white/10">
            <h3 class="text-white font-semibold text-lg mb-2">{{ currentReviewVideo.title }}</h3>
            <p class="text-white/70 text-sm mb-4">{{ currentReviewVideo.description }}</p>

            <div class="flex flex-wrap gap-2 mb-4">
              <span
                v-for="tag in (currentReviewVideo.tags ?? [])"
                :key="tag"
                class="text-xs px-3 py-1 bg-white/10 text-white/80 rounded-full"
              >
                #{{ tag }}
              </span>
            </div>

            <div class="space-y-3">
              <div class="flex items-center gap-3 text-white/70 text-sm">
                <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
                  <User class="w-4 h-4" />
                </div>
                <div class="flex-1">
                  <div class="text-white font-medium">{{ currentReviewVideo.author?.username || '未知作者' }}</div>
                  <div class="text-white/50 text-xs">作者</div>
                </div>
              </div>
              <div class="flex items-center gap-3 text-white/70 text-sm">
                <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
                  <Clock class="w-4 h-4" />
                </div>
                <div class="flex-1">
                  <div class="text-white">{{ formatDuration(currentReviewVideo.duration) }}</div>
                  <div class="text-white/50 text-xs">视频时长</div>
                </div>
              </div>
              <div class="flex items-center gap-3 text-white/70 text-sm">
                <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
                  <Calendar class="w-4 h-4" />
                </div>
                <div class="flex-1">
                  <div class="text-white">{{ formatDate(currentReviewVideo.createdAt) }}</div>
                  <div class="text-white/50 text-xs">上传时间</div>
                </div>
              </div>
            </div>
          </div>

          <div class="p-6 border-b border-white/10">
            <h4 class="text-white/70 text-sm font-medium mb-3">违规类型标记</h4>
            <div class="flex flex-wrap gap-2">
              <span class="text-xs px-3 py-1.5 bg-white/10 text-white/70 rounded-lg cursor-pointer hover:bg-white/20 transition-colors">
                低俗内容
              </span>
              <span class="text-xs px-3 py-1.5 bg-white/10 text-white/70 rounded-lg cursor-pointer hover:bg-white/20 transition-colors">
                版权问题
              </span>
              <span class="text-xs px-3 py-1.5 bg-white/10 text-white/70 rounded-lg cursor-pointer hover:bg-white/20 transition-colors">
                不良引导
              </span>
              <span class="text-xs px-3 py-1.5 bg-white/10 text-white/70 rounded-lg cursor-pointer hover:bg-white/20 transition-colors">
                虚假信息
              </span>
            </div>
          </div>

          <div class="flex-1 flex flex-col justify-end p-6 space-y-3">
            <button
              class="w-full py-4 bg-green-500 text-white rounded-xl font-semibold hover:bg-green-600 transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="actionInProgress"
              @click="approveVideo(currentReviewVideo.id)"
            >
              <CheckCircle class="w-5 h-5" />
              <span>通过审核</span>
              <span class="text-green-200 text-sm">(A)</span>
            </button>

            <button
              class="w-full py-4 bg-red-500 text-white rounded-xl font-semibold hover:bg-red-600 transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="actionInProgress"
              @click="rejectVideo(currentReviewVideo.id)"
            >
              <XCircle class="w-5 h-5" />
              <span>拒绝审核</span>
              <span class="text-red-200 text-sm">(D)</span>
            </button>

            <div class="flex gap-3">
              <button
                class="flex-1 py-3 bg-white/10 text-white rounded-xl font-medium hover:bg-white/20 transition-colors flex items-center justify-center gap-2 disabled:opacity-30"
                :disabled="!hasPrev"
                @click="prevVideo"
              >
                <SkipBack class="w-4 h-4" />
                <span>上一条</span>
              </button>
              <button
                class="flex-1 py-3 bg-white/10 text-white rounded-xl font-medium hover:bg-white/20 transition-colors flex items-center justify-center gap-2 disabled:opacity-30"
                :disabled="!hasNext"
                @click="nextVideo"
              >
                <span>下一条</span>
                <SkipForward class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <button
        class="absolute left-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-all disabled:opacity-20"
        :disabled="!hasPrev"
        @click="prevVideo"
      >
        <ChevronLeft class="w-6 h-6 text-white" />
      </button>
      <button
        class="absolute right-96 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-all disabled:opacity-20"
        :disabled="!hasNext"
        @click="nextVideo"
      >
        <ChevronRight class="w-6 h-6 text-white" />
      </button>
    </div>

    <div
      v-if="reviewMode && showShortcuts"
      class="fixed inset-0 bg-black/80 z-60 flex items-center justify-center"
      @click="showShortcuts = false"
    >
      <div
        class="bg-gray-900 rounded-2xl p-8 max-w-md w-full mx-4"
        @click.stop
      >
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-white font-bold text-xl">键盘快捷键</h3>
          <button
            class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
            @click="showShortcuts = false"
          >
            &times;
          </button>
        </div>
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-white/70">播放 / 暂停</span>
            <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">空格</kbd>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">上一条视频</span>
            <div class="flex gap-1">
              <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">←</kbd>
              <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">J</kbd>
            </div>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">下一条视频</span>
            <div class="flex gap-1">
              <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">→</kbd>
              <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">K</kbd>
            </div>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">通过审核</span>
            <kbd class="px-3 py-1 bg-green-500 text-white rounded text-sm">A</kbd>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">拒绝审核</span>
            <kbd class="px-3 py-1 bg-red-500 text-white rounded text-sm">D</kbd>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">静音 / 取消静音</span>
            <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">M</kbd>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">退出审核模式</span>
            <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">Esc</kbd>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-white/70">显示快捷键</span>
            <kbd class="px-3 py-1 bg-white/10 text-white rounded text-sm">?</kbd>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="!reviewMode && activeStatus === 'pending' && stats.pending > 0 && activeTab === 'videos'"
      class="fixed bottom-6 right-6 z-40"
    >
      <div class="bg-white rounded-xl shadow-lg p-4 flex items-center gap-3">
        <div class="w-12 h-12 bg-yellow-100 rounded-xl flex items-center justify-center">
          <AlertCircle class="w-6 h-6 text-yellow-600" />
        </div>
        <div>
          <p class="text-2xl font-bold text-gray-900">{{ stats.pending }}</p>
          <p class="text-sm text-gray-500">条待审核</p>
        </div>
        <button
          v-if="filteredVideos.length > 0"
          class="ml-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition-colors flex items-center gap-1"
          @click="openReview(filteredVideos[0])"
        >
          <Zap class="w-4 h-4" />
          开始审核
        </button>
      </div>
    </div>

    <div
      v-if="showAddSynonymModal"
      class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center"
      @click="closeAddSynonymModal"
    >
      <div
        class="bg-white rounded-2xl p-6 max-w-md w-full mx-4"
        @click.stop
      >
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-lg font-bold text-gray-900">添加同义词</h3>
          <button
            class="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-500"
            @click="closeAddSynonymModal"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div v-if="selectedCanonicalTag" class="mb-6">
          <p class="text-sm text-gray-500 mb-2">主标签</p>
          <div class="flex items-center gap-2 px-3 py-2 bg-primary/10 rounded-lg">
            <Hash class="w-4 h-4 text-primary" />
            <span class="font-medium text-primary">{{ selectedCanonicalTag.name }}</span>
          </div>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-2">同义词名称</label>
          <input
            v-model="newSynonymName"
            type="text"
            placeholder="请输入同义词名称，如：夜跑、健身餐"
            class="w-full px-4 py-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            @keyup.enter="addSynonym"
          />
        </div>

        <div class="flex gap-3">
          <button
            class="flex-1 py-2.5 border border-gray-200 text-gray-600 rounded-lg font-medium hover:bg-gray-50 transition-colors"
            @click="closeAddSynonymModal"
          >
            取消
          </button>
          <button
            class="flex-1 py-2.5 bg-primary text-white rounded-lg font-medium hover:bg-orange-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!newSynonymName.trim()"
            @click="addSynonym"
          >
            添加
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="showAppealDetail && selectedAppeal"
      class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center"
      @click="closeAppealDetail"
    >
      <div
        class="bg-white rounded-2xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-hidden flex flex-col"
        @click.stop
      >
        <div class="flex items-center justify-between p-6 border-b border-gray-100">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
              <FileText class="w-5 h-5 text-primary" />
            </div>
            <div>
              <h3 class="text-lg font-bold text-gray-900">申诉详情</h3>
              <p class="text-sm text-gray-500">
                申诉编号：#{{ selectedAppeal.id }}
              </p>
            </div>
          </div>
          <button
            class="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-500"
            @click="closeAppealDetail"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-6">
          <div>
            <div class="flex items-center justify-between mb-3">
              <h4 class="font-medium text-gray-700">视频信息</h4>
              <span
                class="text-xs px-2 py-0.5 rounded-full font-medium"
                :class="getAppealStatusClass(selectedAppeal.status)"
              >
                {{ getAppealStatusLabel(selectedAppeal.status) }}
              </span>
            </div>
            <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-xl">
              <div class="relative w-24 h-16 bg-black rounded-lg overflow-hidden flex-shrink-0">
                <img
                  :src="selectedAppeal.video?.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=landscape_4_3'"
                  :alt="selectedAppeal.video?.title"
                  class="w-full h-full object-cover"
                />
              </div>
              <div class="flex-1 min-w-0">
                <h5 class="font-medium text-gray-900 truncate">{{ selectedAppeal.video?.title || '未知视频' }}</h5>
                <p class="text-sm text-gray-500 mt-1">
                  作者：{{ selectedAppeal.user?.username || '未知作者' }}
                </p>
                <p class="text-xs text-gray-400 mt-1">
                  提交时间：{{ formatDate(selectedAppeal.createdAt) }}
                </p>
              </div>
            </div>
          </div>

          <div>
            <h4 class="font-medium text-gray-700 mb-3">申诉类型</h4>
            <span class="inline-block px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm">
              {{ getAppealTypeLabel(selectedAppeal.appealType) }}
            </span>
          </div>

          <div>
            <h4 class="font-medium text-gray-700 mb-3">申诉内容</h4>
            <div class="p-4 bg-gray-50 rounded-xl">
              <p class="text-gray-800 whitespace-pre-wrap">{{ selectedAppeal.content }}</p>
            </div>
          </div>

          <div v-if="selectedAppeal.status === 'reviewed'">
            <h4 class="font-medium text-gray-700 mb-3">复核结论</h4>
            <div class="p-4 rounded-xl" :class="selectedAppeal.reviewResult === 'upheld' ? 'bg-green-50' : 'bg-red-50'">
              <div class="flex items-center gap-2 mb-2">
                <CheckCircle v-if="selectedAppeal.reviewResult === 'upheld'" class="w-5 h-5 text-green-600" />
                <XCircle v-else class="w-5 h-5 text-red-600" />
                <span class="font-medium" :class="selectedAppeal.reviewResult === 'upheld' ? 'text-green-700' : 'text-red-700'">
                  {{ getAppealResultLabel(selectedAppeal.reviewResult) }}
                </span>
              </div>
              <p class="text-gray-700 text-sm">{{ selectedAppeal.reviewComment }}</p>
              <p class="text-gray-400 text-xs mt-2">
                复核时间：{{ formatDate(selectedAppeal.updatedAt) }}
              </p>
            </div>
          </div>
        </div>

        <div class="p-6 border-t border-gray-100 flex gap-3">
          <button
            class="flex-1 py-3 border border-gray-200 text-gray-600 rounded-xl font-medium hover:bg-gray-50 transition-colors"
            @click="closeAppealDetail"
          >
            关闭
          </button>
          <button
            v-if="selectedAppeal.status === 'pending'"
            class="flex-1 py-3 bg-primary text-white rounded-xl font-medium hover:bg-orange-600 transition-colors flex items-center justify-center gap-2"
            @click="openReviewModal(selectedAppeal)"
          >
            <MessageSquare class="w-4 h-4" />
            <span>处理申诉</span>
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="showReviewModal && selectedAppeal"
      class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center"
      @click="closeReviewModal"
    >
      <div
        class="bg-white rounded-2xl p-6 w-full max-w-md mx-4"
        @click.stop
      >
        <h3 class="text-xl font-bold text-gray-900 mb-6">处理申诉</h3>

        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">复核结论</label>
          <div class="flex gap-3">
            <button
              class="flex-1 py-3 rounded-xl font-medium transition-colors border-2 flex flex-col items-center gap-1"
              :class="reviewResult === 'upheld'
                ? 'border-green-500 bg-green-50 text-green-700'
                : 'border-gray-200 text-gray-600 hover:border-gray-300'"
              @click="reviewResult = 'upheld'"
            >
              <ThumbsUp class="w-5 h-5" />
              <span class="text-sm">申诉成立</span>
            </button>
            <button
              class="flex-1 py-3 rounded-xl font-medium transition-colors border-2 flex flex-col items-center gap-1"
              :class="reviewResult === 'rejected'
                ? 'border-red-500 bg-red-50 text-red-700'
                : 'border-gray-200 text-gray-600 hover:border-gray-300'"
              @click="reviewResult = 'rejected'"
            >
              <ThumbsDown class="w-5 h-5" />
              <span class="text-sm">驳回申诉</span>
            </button>
          </div>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-2">复核说明</label>
          <textarea
            v-model="reviewComment"
            rows="4"
            placeholder="请填写复核说明，这将作为处理依据反馈给作者..."
            class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
          />
        </div>

        <div class="flex gap-3">
          <button
            class="flex-1 py-3 bg-gray-100 text-gray-700 rounded-xl font-medium hover:bg-gray-200 transition-colors"
            @click="closeReviewModal"
          >
            取消
          </button>
          <button
            class="flex-1 py-3 bg-primary text-white rounded-xl font-medium hover:bg-orange-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!reviewComment.trim() || reviewInProgress"
            @click="submitReview"
          >
            {{ reviewInProgress ? '处理中...' : '提交复核' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
