<script setup lang="ts">
import { ref } from 'vue'
import { Upload as UploadIcon, X, Image, Tag, Type, CheckCircle } from 'lucide-vue-next'
import Navbar from '@/components/Navbar.vue'
import { videoApi } from '@/api'

const file = ref<File | null>(null)
const previewUrl = ref('')
const title = ref('')
const description = ref('')
const tags = ref('')
const uploadProgress = ref(0)
const isUploading = ref(false)
const uploadSuccess = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    file.value = files[0]
    previewUrl.value = URL.createObjectURL(files[0])
  }
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    file.value = files[0]
    previewUrl.value = URL.createObjectURL(files[0])
  }
}

function handleDragOver(e: DragEvent) {
  e.preventDefault()
}

function removeFile() {
  file.value = null
  previewUrl.value = ''
}

function handleSubmit() {
  if (!file.value || !title.value.trim()) {
    return
  }
  
  isUploading.value = true
  uploadProgress.value = 0
  
  const formData = new FormData()
  formData.append('file', file.value)
  formData.append('title', title.value)
  formData.append('description', description.value)
  
  const tagList = tags.value.split(',').map(t => t.trim()).filter(t => t)
  tagList.forEach((tag, index) => {
    formData.append(`tags[${index}]`, tag)
  })
  
  videoApi.uploadVideo(formData).then(() => {
    isUploading.value = false
    uploadProgress.value = 100
    uploadSuccess.value = true
    
    setTimeout(() => {
      title.value = ''
      description.value = ''
      tags.value = ''
      file.value = null
      previewUrl.value = ''
      uploadProgress.value = 0
      uploadSuccess.value = false
    }, 2000)
  }).catch(() => {
    isUploading.value = false
  })
}

const commonTags = ['美食', '旅行', '健身', '学习', '音乐', '游戏', '宠物', '日常']

function addTag(tag: string) {
  const currentTags = tags.value.split(',').map(t => t.trim()).filter(t => t)
  if (!currentTags.includes(tag)) {
    if (currentTags.length > 0) {
      tags.value += ','
    }
    tags.value += tag
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />
    
    <div class="pt-20 pb-10">
      <div class="max-w-2xl mx-auto px-4">
        <div class="bg-white rounded-2xl shadow-lg overflow-hidden">
          <div class="bg-gradient-to-r from-primary to-orange-400 px-6 py-4">
            <h1 class="text-xl font-bold text-white">发布视频</h1>
            <p class="text-white/80 text-sm">分享你的精彩时刻</p>
          </div>
          
          <div class="p-6">
            <div 
              v-if="uploadSuccess" 
              class="mb-6 p-4 bg-green-50 border border-green-200 rounded-xl flex items-center gap-3"
            >
              <CheckCircle class="w-8 h-8 text-green-500" />
              <div>
                <p class="font-medium text-green-800">上传成功</p>
                <p class="text-sm text-green-600">视频已提交审核</p>
              </div>
            </div>
            
            <div 
              v-if="uploadProgress > 0 && uploadProgress < 100"
              class="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-xl"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="text-blue-800 text-sm">上传中...</span>
                <span class="text-blue-600 text-sm">{{ uploadProgress }}%</span>
              </div>
              <div class="h-2 bg-blue-200 rounded-full overflow-hidden">
                <div 
                  class="h-full bg-blue-500 transition-all duration-300"
                  :style="{ width: `${uploadProgress}%` }"
                />
              </div>
            </div>
            
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                <Image class="w-4 h-4 inline mr-1" />
                视频文件
              </label>
              
              <div 
                v-if="!previewUrl"
                class="border-2 border-dashed border-gray-300 rounded-xl p-12 text-center hover:border-primary transition-colors cursor-pointer"
                @drop="handleDrop"
                @dragover="handleDragOver"
                @click="fileInputRef?.click()"
              >
                <input
                  ref="fileInputRef"
                  type="file"
                  accept="video/*"
                  class="hidden"
                  @change="handleFileChange"
                />
                <UploadIcon class="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p class="text-gray-600 mb-2">拖拽视频文件到这里，或点击选择文件</p>
                <p class="text-sm text-gray-400">支持 MP4、MOV 等格式，建议视频时长不超过1分钟</p>
              </div>
              
              <div v-else class="relative">
                <div class="aspect-video bg-black rounded-xl overflow-hidden">
                  <video :src="previewUrl" class="w-full h-full object-contain" controls />
                </div>
                <button 
                  class="absolute top-2 right-2 w-8 h-8 bg-black/50 hover:bg-black/70 rounded-full flex items-center justify-center transition-colors"
                  @click="removeFile"
                >
                  <X class="w-5 h-5 text-white" />
                </button>
              </div>
            </div>
            
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                <Type class="w-4 h-4 inline mr-1" />
                标题
              </label>
              <input
                v-model="title"
                type="text"
                placeholder="给你的视频起个标题"
                class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50"
                maxlength="50"
              />
              <p class="text-xs text-gray-400 mt-1 text-right">{{ title.length }}/50</p>
            </div>
            
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                <Type class="w-4 h-4 inline mr-1" />
                描述
              </label>
              <textarea
                v-model="description"
                placeholder="分享更多细节..."
                rows="3"
                class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
                maxlength="200"
              />
              <p class="text-xs text-gray-400 mt-1 text-right">{{ description.length }}/200</p>
            </div>
            
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                <Tag class="w-4 h-4 inline mr-1" />
                标签
              </label>
              <input
                v-model="tags"
                type="text"
                placeholder="输入标签，用逗号分隔"
                class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50"
              />
              
              <div class="flex flex-wrap gap-2 mt-3">
                <button
                  v-for="tag in commonTags"
                  :key="tag"
                  class="px-3 py-1.5 bg-gray-100 hover:bg-orange-50 text-gray-600 hover:text-primary rounded-full text-sm transition-colors"
                  @click="addTag(tag)"
                >
                  #{{ tag }}
                </button>
              </div>
            </div>
            
            <button
              class="w-full py-3 bg-gradient-to-r from-primary to-orange-400 text-white font-medium rounded-xl hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!file || !title.trim() || isUploading"
              @click="handleSubmit"
            >
              {{ isUploading ? '上传中...' : '发布视频' }}
            </button>
            
            <p class="text-xs text-gray-400 text-center mt-4">
              发布即表示同意我们的服务条款和隐私政策
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
