<script setup lang="ts">
import { Heart, Bookmark, Eye, Play } from 'lucide-vue-next'
import type { Video } from '@/types'

defineProps<{
  video: Video
}>()

const emit = defineEmits<{
  play: [video: Video]
}>()

function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toString()
}
</script>

<template>
  <div 
    class="bg-white rounded-xl overflow-hidden shadow-md hover:shadow-lg transition-shadow duration-300 cursor-pointer group"
    @click="emit('play', video)"
  >
    <div class="relative aspect-video bg-gray-900 overflow-hidden">
      <img 
        :src="video.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=beautiful%20video%20thumbnail%20abstract%20colorful&image_size=landscape_16_9'" 
        :alt="video.title"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        loading="lazy"
      />
      <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-black/30">
        <div class="w-16 h-16 rounded-full bg-white/90 flex items-center justify-center">
          <Play class="w-8 h-8 text-primary ml-1" />
        </div>
      </div>
      <div class="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
        {{ formatDuration(video.duration) }}
      </div>
    </div>
    
    <div class="p-4">
      <h3 class="font-semibold text-gray-900 text-sm mb-2 ellipsis-text">
        {{ video.title }}
      </h3>
      
      <div class="flex flex-wrap gap-1 mb-3">
        <span 
          v-for="tag in video.tags.slice(0, 3)" 
          :key="tag"
          class="text-xs px-2 py-0.5 bg-orange-50 text-primary rounded-full"
        >
          #{{ tag }}
        </span>
      </div>
      
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2">
            <Heart class="w-4 h-4 text-gray-400" />
            <span class="text-xs text-gray-500">{{ formatNumber(video.likeCount) }}</span>
          </div>
          <div class="flex items-center gap-2">
            <Bookmark class="w-4 h-4 text-gray-400" />
            <span class="text-xs text-gray-500">{{ formatNumber(video.favoriteCount) }}</span>
          </div>
          <div class="flex items-center gap-2">
            <Eye class="w-4 h-4 text-gray-400" />
            <span class="text-xs text-gray-500">{{ formatNumber(video.viewCount) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
