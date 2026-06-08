<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Heart, Bookmark, MessageCircle, Share2, Volume2, VolumeX, Maximize, Pause, Play, Flag } from 'lucide-vue-next'
import type { Video, Comment, VideoMilestone, WatchProgress } from '@/types'
import { videoApi } from '@/api'

const props = defineProps<{
  video: Video
  startTime?: number
  autoResume?: boolean
}>()

const emit = defineEmits<{
  close: []
  next: []
  prev: []
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const isMuted = ref(false)
const isFullscreen = ref(false)
const showControls = ref(true)
const currentTime = ref(0)
const duration = ref(0)
const liked = ref(false)
const favorited = ref(false)
const likeCount = ref(props.video.likeCount)
const favoriteCount = ref(props.video.favoriteCount)
const comments = ref<Comment[]>([])
const commentInput = ref('')
const controlsTimeout = ref<number | null>(null)
const progressSaveInterval = ref<number | null>(null)
const userId = '1'
const videoLoading = ref(true)
const videoError = ref(false)
const milestones = ref<VideoMilestone[]>([])
const hoveredMilestone = ref<VideoMilestone | null>(null)
const watchProgress = ref<WatchProgress | null>(null)
const showResumeTip = ref(false)
const resumeTime = ref(0)

function togglePlay() {
  if (videoRef.value) {
    if (isPlaying.value) {
      videoRef.value.pause()
    } else {
      videoRef.value.play()
    }
    isPlaying.value = !isPlaying.value
  }
}

function toggleMute() {
  if (videoRef.value) {
    videoRef.value.muted = !videoRef.value.muted
    isMuted.value = videoRef.value.muted
  }
}

function toggleFullscreen() {
  const container = videoRef.value?.parentElement
  if (container) {
    if (!document.fullscreenElement) {
      container.requestFullscreen()
      isFullscreen.value = true
    } else {
      document.exitFullscreen()
      isFullscreen.value = false
    }
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
    videoLoading.value = false
    videoError.value = false
    
    if (props.startTime !== undefined && props.startTime > 0 && props.startTime < duration.value) {
      videoRef.value.currentTime = props.startTime
      currentTime.value = props.startTime
    } else if (props.autoResume && watchProgress.value && watchProgress.value.currentTime > 0 && !watchProgress.value.isCompleted) {
      videoRef.value.currentTime = watchProgress.value.currentTime
      currentTime.value = watchProgress.value.currentTime
    }
  }
}

function handleVideoLoadStart() {
  videoLoading.value = true
  videoError.value = false
}

function handleVideoError() {
  videoLoading.value = false
  videoError.value = true
  console.error('Video failed to load:', props.video.videoUrl)
}

function handleProgressClick(e: MouseEvent) {
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  if (videoRef.value) {
    videoRef.value.currentTime = percent * duration.value
  }
}

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function jumpToMilestone(timestamp: number) {
  if (videoRef.value && duration.value > 0 && timestamp >= 0 && timestamp <= duration.value) {
    videoRef.value.currentTime = timestamp
    currentTime.value = timestamp
  }
}

function handleMilestoneClick(e: MouseEvent, milestone: VideoMilestone) {
  e.stopPropagation()
  jumpToMilestone(milestone.timestampSeconds)
}

function handleMilestoneMouseEnter(e: MouseEvent, milestone: VideoMilestone) {
  e.stopPropagation()
  hoveredMilestone.value = milestone
}

function handleMilestoneMouseLeave(e: MouseEvent) {
  e.stopPropagation()
  hoveredMilestone.value = null
}

function loadMilestones() {
  videoApi.getVideoMilestones(props.video.id).then(res => {
    milestones.value = res.data.data
  }).catch(err => {
    console.error('Failed to load milestones:', err)
  })
}

function loadWatchProgress() {
  if (props.startTime !== undefined) return
  
  videoApi.getWatchProgress(props.video.id, userId).then(res => {
    const progress = res.data.data
    if (progress && progress.currentTime > 0 && !progress.isCompleted && progress.currentTime < (duration.value || props.video.duration || 0) - 2) {
      watchProgress.value = progress
      resumeTime.value = progress.currentTime
      showResumeTip.value = true
    }
  }).catch(err => {
    console.error('Failed to load watch progress:', err)
  })
}

function resumeFromLastPosition() {
  if (videoRef.value && resumeTime.value > 0) {
    videoRef.value.currentTime = resumeTime.value
    currentTime.value = resumeTime.value
    showResumeTip.value = false
    if (!isPlaying.value) {
      togglePlay()
    }
  }
}

function dismissResumeTip() {
  showResumeTip.value = false
}

function handleLike() {
  videoApi.likeVideo(props.video.id).then(res => {
    liked.value = res.data.data.liked
    likeCount.value = res.data.data.likeCount
  })
}

function handleFavorite() {
  videoApi.favoriteVideo(props.video.id).then(res => {
    favorited.value = res.data.data.favorited
    favoriteCount.value = res.data.data.favoriteCount
  })
}

function addComment() {
  if (commentInput.value.trim()) {
    videoApi.addComment(props.video.id, commentInput.value).then(res => {
      comments.value.unshift(res.data.data)
      commentInput.value = ''
    })
  }
}

function saveWatchProgress() {
  if (videoRef.value && currentTime.value > 0) {
    videoApi.updateWatchProgress(props.video.id, userId, Math.floor(currentTime.value)).catch(err => {
      console.error('Failed to save watch progress:', err)
    })
  }
}

function startProgressSaving() {
  if (progressSaveInterval.value) {
    clearInterval(progressSaveInterval.value)
  }
  progressSaveInterval.value = window.setInterval(() => {
    saveWatchProgress()
  }, 5000)
}

function stopProgressSaving() {
  if (progressSaveInterval.value) {
    clearInterval(progressSaveInterval.value)
    progressSaveInterval.value = null
  }
}

function resetControlsTimeout() {
  showControls.value = true
  if (controlsTimeout.value) {
    clearTimeout(controlsTimeout.value)
  }
  controlsTimeout.value = window.setTimeout(() => {
    if (isPlaying.value) {
      showControls.value = false
    }
  }, 3000)
}

watch(() => props.video, (newVideo) => {
  likeCount.value = newVideo.likeCount
  favoriteCount.value = newVideo.favoriteCount
  watchProgress.value = null
  showResumeTip.value = false
  resumeTime.value = 0
  loadMilestones()
  loadWatchProgress()
})

watch(() => props.startTime, (newTime) => {
  if (newTime !== undefined && newTime > 0 && videoRef.value && duration.value > 0 && newTime < duration.value) {
    videoRef.value.currentTime = newTime
    currentTime.value = newTime
  }
})

onMounted(() => {
  videoApi.getComments(props.video.id).then(res => {
    comments.value = res.data.data
  })
  loadMilestones()
  loadWatchProgress()
})

onUnmounted(() => {
  if (controlsTimeout.value) {
    clearTimeout(controlsTimeout.value)
  }
  stopProgressSaving()
  saveWatchProgress()
})
</script>

<template>
  <div 
    class="fixed inset-0 bg-black z-50 flex items-center justify-center"
    @click="resetControlsTimeout"
    @mousemove="resetControlsTimeout"
  >
    <button 
      class="absolute top-4 left-4 z-10 w-10 h-10 bg-black/50 hover:bg-black/70 rounded-full flex items-center justify-center transition-colors"
      @click.stop="emit('close')"
    >
      <span class="text-white text-2xl leading-none">&times;</span>
    </button>
    
    <div 
      class="relative w-full max-w-5xl aspect-video bg-black"
      @click.stop="togglePlay"
    >
      <video
        ref="videoRef"
        :src="video.videoUrl"
        class="w-full h-full object-contain"
        preload="metadata"
        @loadstart="handleVideoLoadStart"
        @timeupdate="handleTimeUpdate"
        @loadedmetadata="handleLoadedMetadata"
        @play="isPlaying = true; startProgressSaving()"
        @pause="isPlaying = false; stopProgressSaving(); saveWatchProgress()"
        @ended="emit('next'); saveWatchProgress()"
        @error="handleVideoError"
      />
      
      <div 
        v-if="videoLoading" 
        class="absolute inset-0 flex items-center justify-center bg-black/50"
      >
        <div class="w-12 h-12 border-4 border-white/30 border-t-white rounded-full animate-spin" />
      </div>
      
      <div 
        v-if="videoError" 
        class="absolute inset-0 flex flex-col items-center justify-center bg-black/70"
      >
        <svg class="w-16 h-16 text-white/50 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
        <p class="text-white/70 text-sm">视频加载失败</p>
      </div>
      
      <div 
        v-if="!isPlaying && !videoLoading && !videoError" 
        class="absolute inset-0 flex items-center justify-center"
      >
        <div class="w-20 h-20 rounded-full bg-white/90 flex items-center justify-center hover:scale-110 transition-transform">
          <Play class="w-10 h-10 text-primary ml-1" />
        </div>
      </div>

      <div 
        v-if="showResumeTip && !videoLoading && !videoError"
        class="absolute bottom-24 left-1/2 -translate-x-1/2 bg-black/80 backdrop-blur-sm rounded-xl px-4 py-3 flex items-center gap-3 z-20"
      >
        <div class="text-white/90 text-sm">
          上次看到 <span class="text-primary font-medium">{{ formatTime(resumeTime) }}</span>
        </div>
        <button 
          class="px-3 py-1 bg-primary text-white text-sm rounded-lg hover:bg-orange-600 transition-colors"
          @click.stop="resumeFromLastPosition"
        >
          继续播放
        </button>
        <button 
          class="text-white/50 hover:text-white/80 transition-colors"
          @click.stop="dismissResumeTip"
        >
          <span class="text-xl leading-none">&times;</span>
        </button>
      </div>
      
      <div 
        v-if="showControls || !isPlaying"
        class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 via-black/50 to-transparent p-4"
      >
        <div class="flex items-center gap-4 mb-3">
          <button 
            class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
            @click.stop="togglePlay"
          >
            <Pause v-if="isPlaying" class="w-5 h-5 text-white" />
            <Play v-else class="w-5 h-5 text-white ml-0.5" />
          </button>
          
          <div 
            class="flex-1 h-1 bg-white/30 rounded-full cursor-pointer relative group"
            @click.stop="handleProgressClick"
          >
            <div 
              class="h-full bg-primary rounded-full"
              :style="{ width: `${(currentTime / duration) * 100}%` }"
            />
            
            <div 
              v-for="milestone in milestones" 
              :key="milestone.id"
              class="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-yellow-400 rounded-full cursor-pointer z-10 hover:scale-150 transition-transform shadow-md"
              :style="{ left: `${(milestone.timestampSeconds / duration) * 100}%`, transform: 'translate(-50%, -50%)' }"
              :title="milestone.title"
              @click.stop="handleMilestoneClick($event, milestone)"
              @mouseenter.stop="handleMilestoneMouseEnter($event, milestone)"
              @mouseleave.stop="handleMilestoneMouseLeave($event)"
            />
            
            <div 
              v-if="hoveredMilestone"
              class="absolute -top-10 left-0 -translate-x-1/2 bg-black/90 text-white text-xs px-2 py-1 rounded whitespace-nowrap pointer-events-none z-20"
              :style="{ left: `${(hoveredMilestone.timestampSeconds / duration) * 100}%` }"
            >
              <div class="font-medium">{{ hoveredMilestone.title }}</div>
              <div class="text-white/70">{{ formatTime(hoveredMilestone.timestampSeconds) }}</div>
              <div class="absolute -bottom-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-black/90 rotate-45" />
            </div>
            
            <div 
              class="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity z-20"
              :style="{ left: `${(currentTime / duration) * 100}%`, transform: 'translate(-50%, -50%)' }"
            />
          </div>
          
          <span class="text-white text-sm">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
          
          <button 
            class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
            @click.stop="toggleMute"
          >
            <VolumeX v-if="isMuted" class="w-5 h-5 text-white" />
            <Volume2 v-else class="w-5 h-5 text-white" />
          </button>
          
          <button 
            class="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
            @click.stop="toggleFullscreen"
          >
            <Maximize class="w-5 h-5 text-white" />
          </button>
        </div>
      </div>
    </div>
    
    <div class="absolute right-4 top-1/2 -translate-y-1/2 flex flex-col gap-6">
      <div class="flex flex-col items-center">
        <img 
          :src="video.author.avatar || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=avatar%20portrait%20friendly%20face&image_size=square'" 
          :alt="video.author.username"
          class="w-14 h-14 rounded-full object-cover border-2 border-primary"
        />
        <span class="text-white text-xs mt-2">{{ video.author.username }}</span>
      </div>
      
      <button 
        class="flex flex-col items-center gap-1 transition-transform hover:scale-110"
        @click.stop="handleLike"
      >
        <div 
          class="w-12 h-12 rounded-full flex items-center justify-center transition-colors"
          :class="liked ? 'bg-red-500' : 'bg-white/10'"
        >
          <Heart 
            class="w-6 h-6" 
            :class="liked ? 'text-white fill-white' : 'text-white'"
          />
        </div>
        <span class="text-white text-sm">{{ likeCount }}</span>
      </button>
      
      <button 
        class="flex flex-col items-center gap-1 transition-transform hover:scale-110"
        @click.stop="handleFavorite"
      >
        <div 
          class="w-12 h-12 rounded-full flex items-center justify-center transition-colors"
          :class="favorited ? 'bg-yellow-500' : 'bg-white/10'"
        >
          <Bookmark 
            class="w-6 h-6" 
            :class="favorited ? 'text-white fill-white' : 'text-white'"
          />
        </div>
        <span class="text-white text-sm">{{ favoriteCount }}</span>
      </button>
      
      <button class="flex flex-col items-center gap-1 transition-transform hover:scale-110">
        <div class="w-12 h-12 rounded-full bg-white/10 flex items-center justify-center">
          <MessageCircle class="w-6 h-6 text-white" />
        </div>
        <span class="text-white text-sm">{{ comments.length }}</span>
      </button>
      
      <button class="flex flex-col items-center gap-1 transition-transform hover:scale-110">
        <div class="w-12 h-12 rounded-full bg-white/10 flex items-center justify-center">
          <Share2 class="w-6 h-6 text-white" />
        </div>
        <span class="text-white text-sm">分享</span>
      </button>
    </div>
    
    <div class="absolute left-4 bottom-4 max-w-md">
      <h2 class="text-white font-semibold text-lg mb-2">{{ video.title }}</h2>
      <p class="text-white/70 text-sm mb-4">{{ video.description }}</p>
      
      <div class="flex flex-wrap gap-2 mb-4">
        <span 
          v-for="tag in video.tags" 
          :key="tag"
          class="text-xs px-3 py-1 bg-white/20 text-white rounded-full"
        >
          #{{ tag }}
        </span>
      </div>
      
      <div v-if="milestones.length > 0" class="mb-4">
        <div class="flex items-center gap-2 mb-2">
          <Flag class="w-4 h-4 text-yellow-400" />
          <span class="text-white text-sm font-medium">关键时刻</span>
        </div>
        <div class="space-y-1 max-h-32 overflow-y-auto">
          <div 
            v-for="milestone in milestones" 
            :key="milestone.id"
            class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer hover:bg-white/10 transition-colors"
            :class="{ 'bg-white/10': currentTime >= milestone.timestampSeconds && (milestones.indexOf(milestone) === milestones.length - 1 || currentTime < milestones[milestones.indexOf(milestone) + 1].timestampSeconds) }"
            @click.stop="jumpToMilestone(milestone.timestampSeconds)"
          >
            <div class="w-2 h-2 rounded-full bg-yellow-400 flex-shrink-0" />
            <span class="text-white/90 text-sm flex-1 truncate">{{ milestone.title }}</span>
            <span class="text-white/50 text-xs">{{ formatTime(milestone.timestampSeconds) }}</span>
          </div>
        </div>
      </div>
      
      <div class="bg-white/10 rounded-lg p-3">
        <div class="flex gap-2 mb-2">
          <input
            v-model="commentInput"
            type="text"
            placeholder="发表评论..."
            class="flex-1 bg-white/20 text-white placeholder-white/50 rounded-full px-4 py-2 text-sm focus:outline-none"
            @keyup.enter="addComment"
          />
          <button 
            class="px-4 py-2 bg-primary text-white rounded-full text-sm font-medium hover:bg-orange-600 transition-colors"
            @click="addComment"
          >
            发送
          </button>
        </div>
        
        <div class="max-h-40 overflow-y-auto space-y-3">
          <div 
            v-for="comment in comments" 
            :key="comment.id"
            class="flex gap-2"
          >
            <img 
              :src="comment.author.avatar || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=avatar%20portrait&image_size=square'" 
              :alt="comment.author.username"
              class="w-8 h-8 rounded-full object-cover flex-shrink-0"
            />
            <div>
              <div class="flex items-center gap-2">
                <span class="text-white text-sm font-medium">{{ comment.author.username }}</span>
                <span class="text-white/50 text-xs">{{ new Date(comment.createdAt).toLocaleString() }}</span>
              </div>
              <p class="text-white/80 text-sm">{{ comment.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
