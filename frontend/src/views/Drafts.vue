<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { FileText, Trash2, Edit, Clock, Film, Plus } from 'lucide-vue-next'
import Navbar from '@/components/Navbar.vue'
import { videoApi } from '@/api'
import type { VideoDraft, PageResponse } from '@/types'

const router = useRouter()

const drafts = ref<VideoDraft[]>([])
const isLoading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

function loadDrafts() {
  isLoading.value = true
  videoApi.getDrafts({ page: page.value, size: size.value }).then((response) => {
    if (response.data.code === 200 && response.data.data) {
      const pageData: PageResponse<VideoDraft> = response.data.data
      drafts.value = pageData.content
      total.value = pageData.totalElements
    }
  }).catch(() => {
    console.error('加载草稿列表失败')
  }).finally(() => {
    isLoading.value = false
  })
}

function handleEditDraft(draft: VideoDraft) {
  router.push(`/upload?draft=${draft.id}`)
}

function handleDeleteDraft(draft: VideoDraft) {
  if (!confirm('确定要删除这个草稿吗？')) {
    return
  }

  videoApi.deleteDraft(draft.id).then((response) => {
    if (response.data.code === 200) {
      loadDrafts()
    }
  }).catch(() => {
    console.error('删除草稿失败')
  })
}

function handleCreateNew() {
  router.push('/upload')
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}

function getFileStatusText(status: string) {
  if (status === 'uploaded') return '视频已上传'
  return '未上传视频'
}

function getFileStatusClass(status: string) {
  if (status === 'uploaded') return 'bg-green-100 text-green-700'
  return 'bg-gray-100 text-gray-500'
}

onMounted(() => {
  loadDrafts()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />
    
    <div class="pt-20 pb-10">
      <div class="max-w-4xl mx-auto px-4">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h1 class="text-2xl font-bold text-gray-800">草稿箱</h1>
            <p class="text-gray-500 text-sm mt-1">共 {{ total }} 个草稿</p>
          </div>
          <button
            class="px-4 py-2 bg-gradient-to-r from-primary to-orange-400 text-white font-medium rounded-xl hover:opacity-90 transition-opacity flex items-center gap-2"
            @click="handleCreateNew"
          >
            <Plus class="w-5 h-5" />
            新建草稿
          </button>
        </div>

        <div v-if="isLoading" class="bg-white rounded-2xl shadow-lg p-12 text-center">
          <div class="w-10 h-10 border-4 border-gray-200 border-t-primary rounded-full animate-spin mx-auto mb-4" />
          <p class="text-gray-500">加载中...</p>
        </div>

        <div v-else-if="drafts.length === 0" class="bg-white rounded-2xl shadow-lg p-12 text-center">
          <FileText class="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p class="text-gray-500 mb-2">暂无草稿</p>
          <p class="text-gray-400 text-sm mb-6">点击上方按钮创建你的第一个草稿</p>
          <button
            class="px-6 py-2 bg-gradient-to-r from-primary to-orange-400 text-white font-medium rounded-xl hover:opacity-90 transition-opacity"
            @click="handleCreateNew"
          >
            开始创作
          </button>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="draft in drafts"
            :key="draft.id"
            class="bg-white rounded-2xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow"
          >
            <div class="flex">
              <div class="w-48 h-28 flex-shrink-0 bg-gray-900 relative">
                <img
                  v-if="draft.coverUrl"
                  :src="draft.coverUrl"
                  class="w-full h-full object-cover"
                  alt="封面"
                />
                <video
                  v-else-if="draft.videoUrl"
                  :src="draft.videoUrl"
                  class="w-full h-full object-cover"
                  muted
                />
                <div v-else class="w-full h-full flex flex-col items-center justify-center text-gray-500">
                  <Film class="w-10 h-10 mb-2 text-gray-400" />
                  <span class="text-xs">无视频</span>
                </div>
                <div class="absolute bottom-2 right-2 px-2 py-0.5 bg-black/60 text-white text-xs rounded">
                  {{ getFileStatusText(draft.fileStatus) }}
                </div>
              </div>
              
              <div class="flex-1 p-4 flex flex-col justify-between">
                <div>
                  <div class="flex items-start justify-between">
                    <h3 class="font-medium text-gray-800 text-lg line-clamp-1">
                      {{ draft.title || '未命名草稿' }}
                    </h3>
                    <span :class="['px-2 py-0.5 rounded-full text-xs', getFileStatusClass(draft.fileStatus)]">
                      {{ getFileStatusText(draft.fileStatus) }}
                    </span>
                  </div>
                  <p v-if="draft.description" class="text-gray-500 text-sm mt-1 line-clamp-2">
                    {{ draft.description }}
                  </p>
                  <div v-if="draft.tags && draft.tags.length > 0" class="flex flex-wrap gap-1.5 mt-2">
                    <span
                      v-for="tag in draft.tags.slice(0, 3)"
                      :key="tag"
                      class="px-2 py-0.5 bg-orange-50 text-primary text-xs rounded-full"
                    >
                      #{{ tag }}
                    </span>
                    <span v-if="draft.tags.length > 3" class="px-2 py-0.5 text-gray-400 text-xs">
                      +{{ draft.tags.length - 3 }}
                    </span>
                  </div>
                </div>
                
                <div class="flex items-center justify-between mt-3">
                  <div class="flex items-center gap-1 text-gray-400 text-xs">
                    <Clock class="w-3.5 h-3.5" />
                    <span>{{ formatDate(draft.updatedAt) }}编辑</span>
                  </div>
                  
                  <div class="flex items-center gap-2">
                    <button
                      class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                      title="删除草稿"
                      @click="handleDeleteDraft(draft)"
                    >
                      <Trash2 class="w-4 h-4" />
                    </button>
                    <button
                      class="px-3 py-1.5 bg-gradient-to-r from-primary to-orange-400 text-white text-sm font-medium rounded-lg hover:opacity-90 transition-opacity flex items-center gap-1"
                      @click="handleEditDraft(draft)"
                    >
                      <Edit class="w-4 h-4" />
                      继续编辑
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
