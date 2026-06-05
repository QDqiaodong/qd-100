<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CheckCircle, XCircle, Eye, Play, LayoutGrid, Users, BarChart } from 'lucide-vue-next'
import Navbar from '@/components/Navbar.vue'
import { adminApi } from '@/api'
import type { Video, PageResponse } from '@/types'

const videos = ref<Video[]>([])
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const selectedVideo = ref<Video | null>(null)
const activeStatus = ref<'pending' | 'approved' | 'rejected'>('pending')
const stats = ref({
  total: 128,
  pending: 24,
  approved: 89,
  rejected: 15
})

function fetchVideos() {
  if (loading.value || !hasMore.value) return
  
  loading.value = true
  adminApi.getPendingVideos({
    page: page.value,
    size: 10,
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
  adminApi.updateVideoStatus(id, 'approved').then(() => {
    videos.value = videos.value.filter(v => v.id !== id)
    stats.value.pending--
    stats.value.approved++
  })
}

function rejectVideo(id: string) {
  adminApi.updateVideoStatus(id, 'rejected').then(() => {
    videos.value = videos.value.filter(v => v.id !== id)
    stats.value.pending--
    stats.value.rejected++
  })
}

function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchVideos()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />
    
    <div class="pt-20 pb-10">
      <div class="max-w-6xl mx-auto px-4">
        <div class="flex items-center gap-3 mb-6">
          <LayoutGrid class="w-8 h-8 text-primary" />
          <h1 class="text-2xl font-bold text-gray-900">管理后台</h1>
        </div>
        
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
              @click="activeStatus = 'pending'; videos = []; page = 0; hasMore = true; fetchVideos()"
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
              @click="activeStatus = 'approved'; videos = []; page = 0; hasMore = true; fetchVideos()"
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
              @click="activeStatus = 'rejected'; videos = []; page = 0; hasMore = true; fetchVideos()"
            >
              <XCircle class="w-5 h-5 mx-auto mb-1" />
              <span>已拒绝</span>
              <div 
                v-if="activeStatus === 'rejected'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-red-600"
              />
            </button>
          </div>
          
          <div class="divide-y divide-gray-100">
            <div 
              v-for="video in videos" 
              :key="video.id"
              class="flex items-center gap-4 p-4 hover:bg-gray-50 transition-colors"
            >
              <div class="relative w-32 h-18 bg-black rounded-lg overflow-hidden flex-shrink-0">
                <img 
                  :src="video.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=landscape_4_3'" 
                  :alt="video.title"
                  class="w-full h-full object-cover"
                />
                <button 
                  class="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 hover:opacity-100 transition-opacity"
                  @click="selectedVideo = video"
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
                    {{ video.author.username }}
                  </span>
                  <span>{{ formatDate(video.createdAt) }}</span>
                </div>
              </div>
              
              <div class="flex gap-2">
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
          
          <div v-if="!hasMore && videos.length > 0" class="text-center py-8 text-gray-500">
            已经到底了
          </div>
          
          <div v-if="videos.length === 0 && !loading" class="text-center py-16">
            <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
              <LayoutGrid class="w-10 h-10 text-gray-400" />
            </div>
            <p class="text-gray-500">暂无{{ activeStatus === 'pending' ? '待审核' : activeStatus === 'approved' ? '已通过' : '已拒绝' }}的视频</p>
          </div>
        </div>
        
        <div 
          v-if="selectedVideo" 
          class="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
          @click="selectedVideo = null"
        >
          <div 
            class="bg-black rounded-xl overflow-hidden max-w-4xl w-full mx-4 aspect-video"
            @click.stop
          >
            <video 
              :src="selectedVideo.videoUrl" 
              class="w-full h-full object-contain" 
              controls
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
