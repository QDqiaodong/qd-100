<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Heart, Bookmark, MessageCircle, Share2, Volume2, VolumeX, Maximize, Pause, Play } from 'lucide-vue-next'
import type { Video, Comment } from '@/types'
import { videoApi } from '@/api'

const props = defineProps<{
  video: Video
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
  }
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
})

onMounted(() => {
  videoApi.getComments(props.video.id).then(res => {
    comments.value = res.data.data
  })
})

onUnmounted(() => {
  if (controlsTimeout.value) {
    clearTimeout(controlsTimeout.value)
  }
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
        @timeupdate="handleTimeUpdate"
        @loadedmetadata="handleLoadedMetadata"
        @play="isPlaying = true"
        @pause="isPlaying = false"
        @ended="emit('next')"
      />
      
      <div 
        v-if="!isPlaying && currentTime === 0" 
        class="absolute inset-0 flex items-center justify-center"
      >
        <div class="w-20 h-20 rounded-full bg-white/90 flex items-center justify-center hover:scale-110 transition-transform">
          <Play class="w-10 h-10 text-primary ml-1" />
        </div>
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
              class="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
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
