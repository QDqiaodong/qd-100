<script setup lang="ts">
import { ref } from 'vue'
import { Play, Clock } from 'lucide-vue-next'
import type { WatchProgress } from '@/types'

defineProps<{
  videos: WatchProgress[]
}>()

const emit = defineEmits<{
  play: [progress: WatchProgress]
}>()

const coverErrors = ref<Set<string>>(new Set())

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function getProgressPercent(progress: WatchProgress): number {
  if (!progress.video.duration || progress.video.duration === 0) return 0
  return Math.min((progress.currentTime / progress.video.duration) * 100, 100)
}

function handleClick(progress: WatchProgress) {
  emit('play', progress)
}

function handleCoverError(videoId: string) {
  coverErrors.value.add(videoId)
}
</script>

<template>
  <div v-if="videos.length > 0" class="mb-8">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <Clock class="w-5 h-5 text-primary" />
        <h2 class="text-lg font-semibold text-gray-800">继续观看</h2>
      </div>
      <span class="text-sm text-gray-500">{{ videos.length }} 个视频</span>
    </div>
    
    <div class="relative">
      <div class="flex gap-4 overflow-x-auto pb-4 scrollbar-hide">
        <div
          v-for="progress in videos"
          :key="progress.id"
          class="flex-shrink-0 w-56 group cursor-pointer"
          @click="handleClick(progress)"
        >
          <div class="relative rounded-xl overflow-hidden bg-gray-200 aspect-video mb-2">
            <img
              v-if="!coverErrors.has(progress.video.id)"
              :src="progress.video.coverUrl || 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=video%20thumbnail%20abstract&image_size=square'"
              :alt="progress.video.title"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              @error="handleCoverError(progress.video.id)"
            />
            <div 
              v-else
              class="w-full h-full bg-gradient-to-br from-primary/20 to-orange-500/20 flex items-center justify-center"
            >
              <Play class="w-12 h-12 text-primary/60" />
            </div>
            
            <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <div class="w-12 h-12 rounded-full bg-white/90 flex items-center justify-center">
                <Play class="w-6 h-6 text-primary ml-0.5" />
              </div>
            </div>
            
            <div class="absolute bottom-0 left-0 right-0 h-1 bg-black/50">
              <div
                class="h-full bg-primary transition-all"
                :style="{ width: `${getProgressPercent(progress)}%` }"
              />
            </div>
            
            <div class="absolute bottom-2 right-2 px-2 py-1 bg-black/70 rounded text-xs text-white">
              {{ formatTime(progress.currentTime) }} / {{ formatTime(progress.video.duration || 0) }}
            </div>
          </div>
          
          <h3 class="text-sm font-medium text-gray-800 line-clamp-2 group-hover:text-primary transition-colors">
            {{ progress.video.title }}
          </h3>
          
          <p class="text-xs text-gray-500 mt-1">
            {{ progress.video.author?.username || '未知作者' }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
