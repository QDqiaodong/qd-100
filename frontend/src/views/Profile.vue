<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Camera, Edit3, Trash2, Heart } from 'lucide-vue-next'
import Navbar from '@/components/Navbar.vue'
import { userApi, videoApi } from '@/api'
import type { User as UserType, Video } from '@/types'

const user = ref<UserType | null>(null)
const videos = ref<Video[]>([])
const favorites = ref<Video[]>([])
const activeTab = ref<'videos' | 'favorites'>('videos')
const editingVideo = ref<Video | null>(null)
const editTitle = ref('')
const editDescription = ref('')

function fetchUser() {
  userApi.getCurrentUser().then(res => {
    user.value = res.data.data
  })
}

function fetchVideos() {
  if (user.value) {
    userApi.getUserVideos(user.value.id).then(res => {
      videos.value = res.data.data
    })
  }
}

function fetchFavorites() {
  if (user.value) {
    userApi.getUserFavorites(user.value.id).then(res => {
      favorites.value = res.data.data
    })
  }
}

function handleEdit(video: Video) {
  editingVideo.value = video
  editTitle.value = video.title
  editDescription.value = video.description || ''
}

function saveEdit() {
  if (editingVideo.value) {
    videoApi.updateVideo(editingVideo.value.id, {
      title: editTitle.value,
      description: editDescription.value
    }).then(() => {
      const index = videos.value.findIndex(v => v.id === editingVideo.value?.id)
      if (index !== -1) {
        videos.value[index].title = editTitle.value
        videos.value[index].description = editDescription.value
      }
      editingVideo.value = null
    })
  }
}

function cancelEdit() {
  editingVideo.value = null
}

function deleteVideo(id: string) {
  if (confirm('确定要删除这个视频吗？')) {
    videoApi.deleteVideo(id).then(() => {
      videos.value = videos.value.filter(v => v.id !== id)
    })
  }
}

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

onMounted(() => {
  fetchUser()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />
    
    <div class="pt-20 pb-10">
      <div v-if="user" class="max-w-4xl mx-auto px-4">
        <div class="bg-white rounded-2xl shadow-lg overflow-hidden mb-6">
          <div class="bg-gradient-to-r from-primary to-orange-400 h-32 relative">
            <div class="absolute -bottom-16 left-8">
              <div class="relative">
                <img 
                  :src="user.avatar || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=avatar%20portrait%20professional&image_size=square'" 
                  :alt="user.username"
                  class="w-32 h-32 rounded-full border-4 border-white object-cover"
                />
                <button class="absolute bottom-2 right-2 w-10 h-10 bg-white rounded-full shadow-md flex items-center justify-center hover:scale-110 transition-transform">
                  <Camera class="w-5 h-5 text-gray-600" />
                </button>
              </div>
            </div>
          </div>
          
          <div class="pt-20 px-6 pb-6">
            <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
              <div>
                <h1 class="text-2xl font-bold text-gray-900">{{ user.username }}</h1>
                <p v-if="user.bio" class="text-gray-500 mt-1">{{ user.bio }}</p>
              </div>
              
              <button class="px-6 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-full font-medium transition-colors flex items-center gap-2">
                <Edit3 class="w-4 h-4" />
                编辑资料
              </button>
            </div>
            
            <div class="flex gap-8 mt-6">
              <div class="text-center">
                <p class="text-xl font-bold text-gray-900">{{ user.videoCount }}</p>
                <p class="text-sm text-gray-500">作品</p>
              </div>
              <div class="text-center">
                <p class="text-xl font-bold text-gray-900">{{ formatNumber(user.followers) }}</p>
                <p class="text-sm text-gray-500">粉丝</p>
              </div>
              <div class="text-center">
                <p class="text-xl font-bold text-gray-900">{{ formatNumber(user.following) }}</p>
                <p class="text-sm text-gray-500">关注</p>
              </div>
            </div>
          </div>
        </div>
        
        <div class="bg-white rounded-2xl shadow-lg overflow-hidden">
          <div class="flex border-b border-gray-100">
            <button
              class="flex-1 py-4 text-center font-medium transition-colors relative"
              :class="activeTab === 'videos' ? 'text-primary' : 'text-gray-500 hover:text-gray-700'"
              @click="activeTab = 'videos'; fetchVideos()"
            >
              <User class="w-5 h-5 mx-auto mb-1" />
              <span>我的作品</span>
              <div 
                v-if="activeTab === 'videos'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"
              />
            </button>
            <button
              class="flex-1 py-4 text-center font-medium transition-colors relative"
              :class="activeTab === 'favorites' ? 'text-primary' : 'text-gray-500 hover:text-gray-700'"
              @click="activeTab = 'favorites'; fetchFavorites()"
            >
              <Heart class="w-5 h-5 mx-auto mb-1" />
              <span>我的收藏</span>
              <div 
                v-if="activeTab === 'favorites'"
                class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"
              />
            </button>
          </div>
          
          <div v-if="activeTab === 'videos'" class="p-4">
            <div v-if="videos.length === 0" class="text-center py-16">
              <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
                <User class="w-8 h-8 text-gray-400" />
              </div>
              <p class="text-gray-500">还没有发布过视频</p>
              <button class="mt-4 px-6 py-2 bg-primary text-white rounded-full font-medium hover:bg-orange-600 transition-colors">
                发布第一个视频
              </button>
            </div>
            
            <div v-else class="grid grid-cols-3 gap-2">
              <div 
                v-for="video in videos" 
                :key="video.id"
                class="relative aspect-square bg-black rounded-lg overflow-hidden group cursor-pointer"
              >
                <img 
                  :src="video.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=square'" 
                  :alt="video.title"
                  class="w-full h-full object-cover group-hover:scale-105 transition-transform"
                />
                <div class="absolute inset-0 bg-black/0 group-hover:bg-black/30 flex items-center justify-center transition-colors opacity-0 group-hover:opacity-100">
                  <button 
                    class="w-10 h-10 rounded-full bg-white/90 flex items-center justify-center mr-2 hover:scale-110 transition-transform"
                    @click.stop="handleEdit(video)"
                  >
                    <Edit3 class="w-5 h-5 text-gray-600" />
                  </button>
                  <button 
                    class="w-10 h-10 rounded-full bg-red-500/90 flex items-center justify-center hover:scale-110 transition-transform"
                    @click.stop="deleteVideo(video.id)"
                  >
                    <Trash2 class="w-5 h-5 text-white" />
                  </button>
                </div>
                <div class="absolute bottom-1 right-1 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded">
                  {{ formatDuration(video.duration) }}
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="p-4">
            <div v-if="favorites.length === 0" class="text-center py-16">
              <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
                <Heart class="w-8 h-8 text-gray-400" />
              </div>
              <p class="text-gray-500">还没有收藏视频</p>
            </div>
            
            <div v-else class="grid grid-cols-3 gap-2">
              <div 
                v-for="video in favorites" 
                :key="video.id"
                class="relative aspect-square bg-black rounded-lg overflow-hidden group cursor-pointer"
              >
                <img 
                  :src="video.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=square'" 
                  :alt="video.title"
                  class="w-full h-full object-cover group-hover:scale-105 transition-transform"
                />
                <div class="absolute bottom-1 right-1 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded">
                  {{ formatDuration(video.duration) }}
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div 
          v-if="editingVideo" 
          class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
          @click="cancelEdit"
        >
          <div 
            class="bg-white rounded-2xl p-6 w-full max-w-md mx-4"
            @click.stop
          >
            <h2 class="text-xl font-bold text-gray-900 mb-4">编辑视频</h2>
            
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">标题</label>
              <input
                v-model="editTitle"
                type="text"
                class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50"
              />
            </div>
            
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">描述</label>
              <textarea
                v-model="editDescription"
                rows="3"
                class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
              />
            </div>
            
            <div class="flex gap-3">
              <button
                class="flex-1 py-3 bg-gray-100 text-gray-700 rounded-xl font-medium hover:bg-gray-200 transition-colors"
                @click="cancelEdit"
              >
                取消
              </button>
              <button
                class="flex-1 py-3 bg-primary text-white rounded-xl font-medium hover:bg-orange-600 transition-colors"
                @click="saveEdit"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
