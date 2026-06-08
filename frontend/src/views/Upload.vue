<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Upload as UploadIcon, X, Image, Tag, Type, CheckCircle, ClipboardCheck, Save, ImagePlus, HardDrive, Video, Calendar, AlertTriangle } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import PublishCheckup from '@/components/PublishCheckup.vue'
import { videoApi } from '@/api'
import type { PublishCheckResult, PublishCheckItem, CheckItemSeverity, VideoDraft, UserQuota } from '@/types'

const route = useRoute()
const router = useRouter()

const file = ref<File | null>(null)
const previewUrl = ref('')
const title = ref('')
const description = ref('')
const tags = ref('')
const uploadProgress = ref(0)
const isUploading = ref(false)
const uploadSuccess = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const videoDuration = ref(0)
const showCheckup = ref(false)
const checkResult = ref<PublishCheckResult | null>(null)

const draftId = ref<string | null>(null)
const isSaving = ref(false)
const saveSuccess = ref(false)
const isLoadingDraft = ref(false)

const coverFile = ref<File | null>(null)
const coverUrl = ref('')
const coverInputRef = ref<HTMLInputElement | null>(null)

const userQuota = ref<UserQuota | null>(null)
const quotaLoading = ref(false)
const userId = '1'

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return bytes + ' B'
  } else if (bytes < 1024 * 1024) {
    return (bytes / 1024).toFixed(2) + ' KB'
  } else if (bytes < 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  } else {
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  }
}

function getStatusColor(status: string): string {
  switch (status) {
    case 'exceeded':
      return 'text-red-500'
    case 'warning':
      return 'text-orange-500'
    case 'moderate':
      return 'text-yellow-500'
    default:
      return 'text-green-500'
  }
}

function getProgressBgColor(status: string): string {
  switch (status) {
    case 'exceeded':
      return 'bg-red-500'
    case 'warning':
      return 'bg-orange-500'
    case 'moderate':
      return 'bg-yellow-500'
    default:
      return 'bg-green-500'
  }
}

function loadUserQuota() {
  quotaLoading.value = true
  videoApi.getUserQuota(userId).then(res => {
    if (res.data.code === 200) {
      userQuota.value = res.data.data
    }
  }).catch(err => {
    console.error('加载用户配额失败:', err)
  }).finally(() => {
    quotaLoading.value = false
  })
}

const hasQuotaWarning = computed(() => {
  if (!userQuota.value) return false
  return userQuota.value.isVideoCountNearLimit || 
         userQuota.value.isDailyUploadNearLimit || 
         userQuota.value.isStorageNearLimit
})

const isQuotaExceeded = computed(() => {
  if (!userQuota.value) return false
  return userQuota.value.videoCountPercent >= 100 || 
         userQuota.value.dailyUploadPercent >= 100 || 
         userQuota.value.storagePercent >= 100
})

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    file.value = files[0]
    previewUrl.value = URL.createObjectURL(files[0])
    checkVideoDuration(files[0])
  }
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    file.value = files[0]
    previewUrl.value = URL.createObjectURL(files[0])
    checkVideoDuration(files[0])
  }
}

function checkVideoDuration(fileObj: File) {
  const video = document.createElement('video')
  video.preload = 'metadata'
  video.onloadedmetadata = () => {
    videoDuration.value = video.duration
    URL.revokeObjectURL(video.src)
  }
  video.src = URL.createObjectURL(fileObj)
}

function handleDragOver(e: DragEvent) {
  e.preventDefault()
}

function removeFile() {
  file.value = null
  previewUrl.value = ''
  videoDuration.value = 0
}

function handleCoverChange(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    coverFile.value = files[0]
    coverUrl.value = URL.createObjectURL(files[0])
  }
}

function removeCover() {
  coverFile.value = null
  coverUrl.value = ''
}

const tagList = computed(() => {
  return tags.value.split(',').map(t => t.trim()).filter(t => t)
})

function runCheckup(): PublishCheckResult {
  const checks: PublishCheckItem[] = []

  checks.push(checkCover())
  checks.push(checkTitle())
  checks.push(checkTags())
  checks.push(checkDescription())
  checks.push(checkDuration())

  const passedItems = checks.filter(c => c.severity === 'success').length
  const warningItems = checks.filter(c => c.severity === 'warning').length
  const errorItems = checks.filter(c => c.severity === 'error').length

  const scorePerItem = 100 / checks.length
  let overallScore = 0
  checks.forEach(check => {
    if (check.severity === 'success') {
      overallScore += scorePerItem
    } else if (check.severity === 'warning') {
      overallScore += scorePerItem * 0.5
    }
  })
  overallScore = Math.round(overallScore)

  return {
    overallScore,
    totalItems: checks.length,
    passedItems,
    warningItems,
    errorItems,
    checks
  }
}

function checkCover(): PublishCheckItem {
  const hasVideo = !!file.value
  let severity: CheckItemSeverity = 'success'
  let description = '视频文件已准备好'
  let suggestion = ''

  if (!hasVideo) {
    severity = 'error'
    description = '尚未上传视频文件'
    suggestion = '请上传视频文件后再发布'
  } else if (videoDuration.value > 0 && videoDuration.value < 3) {
    severity = 'warning'
    description = '视频时长较短，封面可能不够有代表性'
    suggestion = '建议选择时长超过3秒的视频，或手动设置更吸引人的封面'
  }

  return {
    id: 'cover',
    title: '封面检查',
    description,
    severity,
    suggestion,
    icon: 'cover'
  }
}

function checkTitle(): PublishCheckItem {
  const titleLen = title.value.trim().length
  let severity: CheckItemSeverity = 'success'
  let description = '标题长度合适'
  let suggestion = ''

  if (titleLen === 0) {
    severity = 'error'
    description = '标题不能为空'
    suggestion = '请填写视频标题，让观众了解视频内容'
  } else if (titleLen < 5) {
    severity = 'warning'
    description = `标题过短（${titleLen}字），可能无法准确传达内容`
    suggestion = '建议标题长度在5-30字之间，更有利于推荐和搜索'
  } else if (titleLen > 40) {
    severity = 'warning'
    description = `标题过长（${titleLen}字），展示可能不完整`
    suggestion = '建议精简标题，突出重点内容'
  }

  return {
    id: 'title',
    title: '标题检查',
    description,
    severity,
    suggestion,
    icon: 'title'
  }
}

function checkTags(): PublishCheckItem {
  const tagsArr = tagList.value
  const tagCount = tagsArr.length
  let severity: CheckItemSeverity = 'success'
  let desc = `已添加 ${tagCount} 个标签`
  let suggestion = ''

  if (tagCount === 0) {
    severity = 'warning'
    desc = '尚未添加任何标签'
    suggestion = '建议添加2-5个相关标签，有助于视频被更多人发现'
  } else if (tagCount === 1) {
    severity = 'warning'
    desc = '标签数量较少，覆盖范围有限'
    suggestion = '建议添加2-5个相关标签，覆盖更多搜索场景'
  } else if (tagCount > 8) {
    severity = 'warning'
    desc = `标签过多（${tagCount}个），可能过于分散`
    suggestion = '建议精选3-5个最相关的标签，避免标签过于分散'
  }

  if (tagCount > 0) {
    const shortTags = tagsArr.filter(t => t.length < 2)
    if (shortTags.length > 0) {
      severity = 'warning'
      desc += '，部分标签过短'
      suggestion = '建议每个标签至少2个字，更有实际意义'
    }
  }

  return {
    id: 'tags',
    title: '标签检查',
    description: desc,
    severity,
    suggestion,
    icon: 'tags'
  }
}

function checkDescription(): PublishCheckItem {
  const descLen = description.value.trim().length
  let severity: CheckItemSeverity = 'success'
  let desc = '描述内容正常'
  let suggestion = ''

  if (descLen === 0) {
    severity = 'info'
    desc = '尚未填写视频描述'
    suggestion = '可以添加详细描述，帮助观众更好地了解视频内容'
  } else if (descLen > 0 && descLen < 10) {
    severity = 'info'
    desc = '描述内容较短'
    suggestion = '可以补充更多细节，提升视频吸引力'
  }

  if (descLen > 0 && title.value.trim().length > 0) {
    const similarity = calculateSimilarity(title.value.trim(), description.value.trim())
    if (similarity > 0.7) {
      severity = 'warning'
      desc = '描述与标题内容重复度较高'
      suggestion = '建议在描述中补充更多标题之外的信息，避免内容重复'
    }
  }

  return {
    id: 'description',
    title: '文案检查',
    description: desc,
    severity,
    suggestion,
    icon: 'description'
  }
}

function calculateSimilarity(str1: string, str2: string): number {
  const set1 = new Set(str1.split(''))
  const set2 = new Set(str2.split(''))
  const intersection = new Set([...set1].filter(x => set2.has(x)))
  const union = new Set([...set1, ...set2])
  return intersection.size / union.size
}

function checkDuration(): PublishCheckItem {
  const duration = videoDuration.value
  let severity: CheckItemSeverity = 'success'
  let description = '视频时长合适'
  let suggestion = ''

  if (duration === 0) {
    severity = 'info'
    description = '视频时长检测中...'
    suggestion = '视频加载完成后可检测时长'
  } else if (duration < 5) {
    severity = 'warning'
    description = `视频过短（${duration.toFixed(1)}秒）`
    suggestion = '建议视频时长在15秒以上，内容更完整'
  } else if (duration > 180) {
    severity = 'warning'
    description = `视频过长（${(duration / 60).toFixed(1)}分钟）`
    suggestion = '短视频建议控制在3分钟以内，完播率更高'
  } else if (duration > 60) {
    severity = 'info'
    description = `视频时长 ${(duration / 60).toFixed(1)} 分钟`
    suggestion = '可以考虑将精彩内容前置，提升观众留存'
  }

  return {
    id: 'duration',
    title: '时长检查',
    description,
    severity,
    suggestion,
    icon: 'duration'
  }
}

function handleCheckup() {
  if (!file.value || !title.value.trim()) {
    return
  }
  checkResult.value = runCheckup()
  showCheckup.value = true
}

function handleBackToEdit() {
  showCheckup.value = false
}

function handleSaveDraft() {
  if (!title.value.trim() && !file.value && !description.value.trim() && !tags.value.trim() && !coverFile.value) {
    return
  }

  isSaving.value = true
  saveSuccess.value = false

  videoApi.saveDraft({
    draftId: draftId.value || undefined,
    title: title.value,
    description: description.value,
    tags: tagList.value,
    duration: videoDuration.value,
    file: file.value || undefined,
    coverFile: coverFile.value || undefined
  }).then((response) => {
    if (response.data.code === 200 && response.data.data) {
      draftId.value = response.data.data.id
      saveSuccess.value = true
      setTimeout(() => {
        saveSuccess.value = false
      }, 2000)
    }
  }).catch(() => {
    console.error('保存草稿失败')
  }).finally(() => {
    isSaving.value = false
  })
}

function loadDraft(id: string) {
  isLoadingDraft.value = true
  videoApi.getDraft(id).then((response) => {
    if (response.data.code === 200 && response.data.data) {
      const draft: VideoDraft = response.data.data
      draftId.value = draft.id
      title.value = draft.title || ''
      description.value = draft.description || ''
      tags.value = draft.tags?.join(',') || ''
      videoDuration.value = draft.duration || 0
      
      if (draft.videoUrl) {
        previewUrl.value = draft.videoUrl
      }
      if (draft.coverUrl) {
        coverUrl.value = draft.coverUrl
      }
    }
  }).catch(() => {
    console.error('加载草稿失败')
  }).finally(() => {
    isLoadingDraft.value = false
  })
}

function resetForm() {
  title.value = ''
  description.value = ''
  tags.value = ''
  file.value = null
  previewUrl.value = ''
  uploadProgress.value = 0
  uploadSuccess.value = false
  videoDuration.value = 0
  checkResult.value = null
  draftId.value = null
  saveSuccess.value = false
  coverFile.value = null
  coverUrl.value = ''
}

function handlePublishFromDraft() {
  if (!title.value.trim()) {
    return
  }

  isUploading.value = true
  uploadProgress.value = 0

  const doPublish = (id: string) => {
    videoApi.publishDraft(id).then((response) => {
      if (response.data.code === 200) {
        isUploading.value = false
        uploadProgress.value = 100
        uploadSuccess.value = true
        showCheckup.value = false
        loadUserQuota()

        setTimeout(() => {
          resetForm()
          router.push('/')
        }, 2000)
      }
    }).catch((err) => {
      isUploading.value = false
      const errorMsg = err?.response?.data?.message || '发布失败，请稍后重试'
      alert(errorMsg)
    })
  }

  if (draftId.value) {
    videoApi.saveDraft({
      draftId: draftId.value,
      title: title.value,
      description: description.value,
      tags: tagList.value,
      duration: videoDuration.value,
      file: file.value || undefined,
      coverFile: coverFile.value || undefined
    }).then((response) => {
      if (response.data.code === 200 && response.data.data) {
        doPublish(response.data.data.id)
      } else {
        isUploading.value = false
      }
    }).catch(() => {
      isUploading.value = false
    })
  } else {
    if (!file.value) {
      isUploading.value = false
      return
    }
    
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('title', title.value)
    formData.append('description', description.value)
    
    tagList.value.forEach((tag, index) => {
      formData.append(`tags[${index}]`, tag)
    })
    
    videoApi.uploadVideo(formData).then(() => {
      isUploading.value = false
      uploadProgress.value = 100
      uploadSuccess.value = true
      showCheckup.value = false
      
      setTimeout(() => {
        resetForm()
        router.push('/')
      }, 2000)
    }).catch(() => {
      isUploading.value = false
    })
  }
}

onMounted(() => {
  const draftParam = route.query.draft as string
  if (draftParam) {
    loadDraft(draftParam)
  }
  loadUserQuota()
})

const commonTags = ['美食', '旅行', '健身', '学习', '音乐', '游戏', '宠物', '日常']

function addTag(tag: string) {
  const currentTags = tagList.value
  if (!currentTags.includes(tag)) {
    if (currentTags.length > 0) {
      tags.value += ','
    }
    tags.value += tag
  }
}

const canCheckup = computed(() => {
  if (isQuotaExceeded.value) return false
  return !!file.value && !!title.value.trim()
})
</script>

<template>
  <div class="min-h-screen bg-gray-100">
    <Navbar />
    
    <div class="pt-20 pb-10">
      <div class="max-w-2xl mx-auto px-4">
        <div v-if="showCheckup && checkResult">
          <PublishCheckup
            :check-result="checkResult"
            :video-preview="previewUrl"
            :title="title"
            @back="handleBackToEdit"
            @publish="handlePublishFromDraft"
          />
        </div>

        <div v-else class="bg-white rounded-2xl shadow-lg overflow-hidden">
          <div class="bg-gradient-to-r from-primary to-orange-400 px-6 py-4">
            <h1 class="text-xl font-bold text-white">发布视频</h1>
            <p class="text-white/80 text-sm">分享你的精彩时刻</p>
          </div>
          
          <div class="p-6">
            <div 
              v-if="quotaLoading" 
              class="mb-6 p-4 bg-gray-50 border border-gray-200 rounded-xl flex items-center gap-3"
            >
              <div class="w-5 h-5 border-2 border-gray-300 border-t-primary rounded-full animate-spin" />
              <p class="text-sm text-gray-600">正在加载配额信息...</p>
            </div>
            
            <div v-else-if="userQuota" class="mb-6">
              <div 
                v-if="isQuotaExceeded" 
                class="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3"
              >
                <AlertTriangle class="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                <div>
                  <p class="font-medium text-red-800">配额已用尽</p>
                  <p class="text-sm text-red-600">您的上传配额已达上限，请升级套餐或删除部分内容后再尝试</p>
                </div>
              </div>
              
              <div 
                v-else-if="hasQuotaWarning" 
                class="mb-4 p-4 bg-orange-50 border border-orange-200 rounded-xl flex items-start gap-3"
              >
                <AlertTriangle class="w-5 h-5 text-orange-500 flex-shrink-0 mt-0.5" />
                <div>
                  <p class="font-medium text-orange-800">配额即将用尽</p>
                  <p class="text-sm text-orange-600">请注意控制上传频率，避免影响正常使用</p>
                </div>
              </div>
              
              <div class="grid grid-cols-3 gap-3">
                <div class="p-3 bg-gray-50 rounded-xl">
                  <div class="flex items-center gap-2 mb-2">
                    <Video class="w-4 h-4 text-gray-500" />
                    <span class="text-xs text-gray-500">视频数量</span>
                  </div>
                  <div class="flex items-baseline gap-1 mb-2">
                    <span :class="['text-lg font-bold', getStatusColor(userQuota.videoCountStatus)]">
                      {{ userQuota.totalVideoCount }}
                    </span>
                    <span class="text-xs text-gray-400">/ {{ userQuota.maxVideoCount }}</span>
                  </div>
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div 
                      :class="['h-full rounded-full transition-all', getProgressBgColor(userQuota.videoCountStatus)]"
                      :style="{ width: Math.min(userQuota.videoCountPercent, 100) + '%' }"
                    />
                  </div>
                </div>
                
                <div class="p-3 bg-gray-50 rounded-xl">
                  <div class="flex items-center gap-2 mb-2">
                    <Calendar class="w-4 h-4 text-gray-500" />
                    <span class="text-xs text-gray-500">今日上传</span>
                  </div>
                  <div class="flex items-baseline gap-1 mb-2">
                    <span :class="['text-lg font-bold', getStatusColor(userQuota.dailyUploadStatus)]">
                      {{ userQuota.todayUploadCount }}
                    </span>
                    <span class="text-xs text-gray-400">/ {{ userQuota.dailyUploadLimit }}</span>
                  </div>
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div 
                      :class="['h-full rounded-full transition-all', getProgressBgColor(userQuota.dailyUploadStatus)]"
                      :style="{ width: Math.min(userQuota.dailyUploadPercent, 100) + '%' }"
                    />
                  </div>
                </div>
                
                <div class="p-3 bg-gray-50 rounded-xl">
                  <div class="flex items-center gap-2 mb-2">
                    <HardDrive class="w-4 h-4 text-gray-500" />
                    <span class="text-xs text-gray-500">存储空间</span>
                  </div>
                  <div class="flex items-baseline gap-1 mb-2">
                    <span :class="['text-lg font-bold', getStatusColor(userQuota.storageStatus)]">
                      {{ formatFileSize(userQuota.usedStorageBytes) }}
                    </span>
                  </div>
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div 
                      :class="['h-full rounded-full transition-all', getProgressBgColor(userQuota.storageStatus)]"
                      :style="{ width: Math.min(userQuota.storagePercent, 100) + '%' }"
                    />
                  </div>
                  <p class="text-xs text-gray-400 mt-1">共 {{ formatFileSize(userQuota.maxStorageBytes) }}</p>
                </div>
              </div>
            </div>
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
                <ImagePlus class="w-4 h-4 inline mr-1" />
                封面图片
              </label>
              
              <div 
                v-if="!coverUrl"
                class="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center hover:border-primary transition-colors cursor-pointer"
                @click="coverInputRef?.click()"
              >
                <input
                  ref="coverInputRef"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="handleCoverChange"
                />
                <ImagePlus class="w-10 h-10 text-gray-400 mx-auto mb-3" />
                <p class="text-gray-600 text-sm">点击上传封面图片</p>
                <p class="text-xs text-gray-400 mt-1">支持 JPG、PNG 等格式，建议尺寸 1:1</p>
              </div>
              
              <div v-else class="relative">
                <div class="aspect-video bg-gray-100 rounded-xl overflow-hidden">
                  <img :src="coverUrl" class="w-full h-full object-cover" alt="封面预览" />
                </div>
                <button 
                  class="absolute top-2 right-2 w-8 h-8 bg-black/50 hover:bg-black/70 rounded-full flex items-center justify-center transition-colors"
                  @click="removeCover"
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
            
            <div 
              v-if="saveSuccess" 
              class="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-xl flex items-center gap-2"
            >
              <CheckCircle class="w-5 h-5 text-blue-500" />
              <p class="text-sm text-blue-700">草稿已保存</p>
            </div>

            <div 
              v-if="isLoadingDraft" 
              class="mb-4 p-3 bg-gray-50 border border-gray-200 rounded-xl flex items-center gap-2"
            >
              <div class="w-5 h-5 border-2 border-gray-300 border-t-primary rounded-full animate-spin" />
              <p class="text-sm text-gray-600">正在加载草稿...</p>
            </div>

            <div class="flex gap-3">
              <button
                class="flex-1 py-3 border-2 border-gray-200 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                :disabled="isSaving || (!title.trim() && !file && !description.trim() && !tags.trim() && !coverFile)"
                @click="handleSaveDraft"
              >
                <Save class="w-5 h-5" />
                {{ isSaving ? '保存中...' : '保存草稿' }}
              </button>
              <button
                class="flex-1 py-3 bg-gradient-to-r from-primary to-orange-400 text-white font-medium rounded-xl hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                :disabled="!canCheckup || isUploading"
                @click="handleCheckup"
              >
                <ClipboardCheck class="w-5 h-5" />
                发布前体检
              </button>
            </div>
            
            <p class="text-xs text-gray-400 text-center mt-4">
              发布即表示同意我们的服务条款和隐私政策
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
