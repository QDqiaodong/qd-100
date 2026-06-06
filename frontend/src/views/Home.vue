<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import Navbar from '@/components/Navbar.vue'
import VideoCard from '@/components/VideoCard.vue'
import VideoPlayerModal from '@/components/VideoPlayer.vue'
import ContinueWatchStrip from '@/components/ContinueWatchStrip.vue'
import MorningReport from '@/components/MorningReport.vue'
import { videoApi } from '@/api'
import type { Video, WatchProgress, MorningReport as MorningReportType, HotTag, NewAuthor, TrendingVideo } from '@/types'

const videos = ref<Video[]>([])
const continueWatchingVideos = ref<WatchProgress[]>([])
const morningReport = ref<MorningReportType | null>(null)
const morningReportLoading = ref(false)
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const selectedVideo = ref<Video | null>(null)
const selectedStartTime = ref<number | undefined>(undefined)
const tags = ref(['全部', '美食', '旅行', '健身', '学习', '音乐', '游戏', '宠物'])
const activeTag = ref('全部')
const sortOptions = ref([
  { value: 'hot', label: '热门' },
  { value: 'new', label: '最新' },
  { value: 'follow', label: '关注' }
])
const activeSort = ref('hot')
const userId = '1'

const observer = ref<IntersectionObserver | null>(null)
const lastVideoRef = ref<HTMLElement | null>(null)

function fetchContinueWatching() {
  videoApi.getContinueWatchingVideos(userId).then(res => {
    continueWatchingVideos.value = res.data.data || []
  }).catch(err => {
    console.error('Failed to fetch continue watching videos:', err)
  })
}

function fetchMorningReport() {
  morningReportLoading.value = true
  videoApi.getMorningReport().then(res => {
    morningReport.value = res.data.data
  }).catch(err => {
    console.error('Failed to fetch morning report:', err)
  }).finally(() => {
    morningReportLoading.value = false
  })
}

function handleTagClick(tag: HotTag) {
  activeTag.value = tag.name
  videos.value = []
  page.value = 0
  hasMore.value = true
  fetchVideos()
}

function handleAuthorClick(author: NewAuthor) {
  console.log('Author clicked:', author.username)
}

function handleTrendingVideoClick(video: TrendingVideo) {
  videoApi.getVideo(video.id).then(res => {
    selectedVideo.value = res.data.data
    selectedStartTime.value = undefined
  }).catch(() => {
    console.error('Failed to fetch video details')
  })
}

function handleContinueWatch(progress: WatchProgress) {
  selectedVideo.value = progress.video
  selectedStartTime.value = progress.currentTime
}

function fetchVideos() {
  if (loading.value || !hasMore.value) return
  
  loading.value = true
  const params = {
    page: page.value,
    size: 12,
    tag: activeTag.value === '全部' ? undefined : activeTag.value,
    sort: activeSort.value
  }
  
  videoApi.getVideos(params).then(res => {
    const newVideos = res.data.data.content
    if (newVideos.length === 0) {
      hasMore.value = false
    } else {
      videos.value = [...videos.value, ...newVideos]
      page.value++
    }
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleVideoClick(video: Video) {
  selectedVideo.value = video
  selectedStartTime.value = undefined
}

function closePlayer() {
  selectedVideo.value = null
  selectedStartTime.value = undefined
  fetchContinueWatching()
}

function initObserver() {
  observer.value = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && !loading.value && hasMore.value) {
        fetchVideos()
      }
    },
    { rootMargin: '200px' }
  )
}

onMounted(() => {
  fetchMorningReport()
  fetchContinueWatching()
  fetchVideos()
  initObserver()
})

onUnmounted(() => {
  if (observer.value) {
    observer.value.disconnect()
  }
})
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />
    
    <div class="pt-20 pb-10">
      <div class="max-w-7xl mx-auto px-4">
        <MorningReport 
          :report="morningReport"
          :loading="morningReportLoading"
          @tag-click="handleTagClick"
          @author-click="handleAuthorClick"
          @video-click="handleTrendingVideoClick"
        />

        <ContinueWatchStrip 
          :videos="continueWatchingVideos"
          @play="handleContinueWatch"
        />
        
        <div class="sticky top-16 bg-gray-100/95 backdrop-blur-sm py-4 z-40">
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div class="flex gap-2 overflow-x-auto pb-2 md:pb-0 scrollbar-hide">
              <button
                v-for="tag in tags"
                :key="tag"
                class="px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all"
                :class="activeTag === tag 
                  ? 'bg-primary text-white' 
                  : 'bg-white text-gray-600 hover:bg-gray-100'"
                @click="activeTag = tag; videos = []; page = 0; hasMore = true; fetchVideos()"
              >
                {{ tag }}
              </button>
            </div>
            
            <div class="flex gap-2">
              <button
                v-for="option in sortOptions"
                :key="option.value"
                class="px-3 py-1.5 rounded-lg text-sm transition-all"
                :class="activeSort === option.value 
                  ? 'bg-gray-900 text-white' 
                  : 'bg-white text-gray-600 hover:bg-gray-100'"
                @click="activeSort = option.value; videos = []; page = 0; hasMore = true; fetchVideos()"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </div>
        
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mt-6">
          <VideoCard 
            v-for="(video, index) in videos" 
            :key="video.id"
            :video="video"
            :ref="(el) => { if (index === videos.length - 1) lastVideoRef = el as HTMLElement }"
            @play="handleVideoClick"
          />
        </div>
        
        <div v-if="loading" class="flex justify-center py-8">
          <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
        
        <div v-if="!hasMore && videos.length > 0" class="text-center py-8 text-gray-500">
          已经到底了
        </div>
        
        <div v-if="videos.length === 0 && !loading" class="text-center py-16">
          <div class="w-20 h-20 mx-auto mb-4 bg-gray-200 rounded-full flex items-center justify-center">
            <svg class="w-10 h-10 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <p class="text-gray-500">暂无视频内容</p>
        </div>
      </div>
    </div>
    
    <VideoPlayerModal 
      v-if="selectedVideo"
      :video="selectedVideo"
      :start-time="selectedStartTime"
      @close="closePlayer"
    />
  </div>
</template>
