<script setup lang="ts">
import { computed } from 'vue'
import { 
  Image as ImageIcon, 
  Type, 
  Tag, 
  FileText, 
  Clock, 
  CheckCircle, 
  AlertTriangle, 
  XCircle, 
  Info, 
  ArrowLeft,
  ShieldCheck
} from 'lucide-vue-next'
import type { PublishCheckResult, CheckItemSeverity } from '@/types'

const props = defineProps<{
  checkResult: PublishCheckResult
  videoPreview?: string
  title: string
}>()

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'publish'): void
}>()

const scoreColor = computed(() => {
  const score = props.checkResult.overallScore
  if (score >= 90) return 'text-green-500'
  if (score >= 70) return 'text-yellow-500'
  if (score >= 60) return 'text-orange-500'
  return 'text-red-500'
})

const scoreRingColor = computed(() => {
  const score = props.checkResult.overallScore
  if (score >= 90) return '#22c55e'
  if (score >= 70) return '#eab308'
  if (score >= 60) return '#f97316'
  return '#ef4444'
})

const scoreText = computed(() => {
  const score = props.checkResult.overallScore
  if (score >= 90) return '优秀'
  if (score >= 70) return '良好'
  if (score >= 60) return '及格'
  return '待改进'
})

function getSeverityIcon(severity: CheckItemSeverity) {
  switch (severity) {
    case 'success':
      return CheckCircle
    case 'warning':
      return AlertTriangle
    case 'error':
      return XCircle
    case 'info':
    default:
      return Info
  }
}

function getSeverityColor(severity: CheckItemSeverity) {
  switch (severity) {
    case 'success':
      return 'text-green-500 bg-green-50 border-green-200'
    case 'warning':
      return 'text-yellow-600 bg-yellow-50 border-yellow-200'
    case 'error':
      return 'text-red-500 bg-red-50 border-red-200'
    case 'info':
    default:
      return 'text-blue-500 bg-blue-50 border-blue-200'
  }
}

function getItemIcon(iconName: string) {
  switch (iconName) {
    case 'cover':
      return ImageIcon
    case 'title':
      return Type
    case 'tags':
      return Tag
    case 'description':
      return FileText
    case 'duration':
      return Clock
    default:
      return Info
  }
}

const canPublish = computed(() => {
  return props.checkResult.errorItems === 0
})
</script>

<template>
  <div class="bg-white rounded-2xl shadow-lg overflow-hidden">
    <div class="bg-gradient-to-r from-primary to-orange-400 px-6 py-5">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <button 
            class="w-10 h-10 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
            @click="emit('back')"
          >
            <ArrowLeft class="w-5 h-5 text-white" />
          </button>
          <div>
            <h1 class="text-xl font-bold text-white">发布前体检</h1>
            <p class="text-white/80 text-sm">完成自检，发布更放心</p>
          </div>
        </div>
        <ShieldCheck class="w-8 h-8 text-white/80" />
      </div>
    </div>

    <div class="p-6">
      <div class="flex items-center gap-6 mb-8">
        <div class="relative w-28 h-28 flex-shrink-0">
          <svg class="w-28 h-28 transform -rotate-90" viewBox="0 0 100 100">
            <circle
              cx="50"
              cy="50"
              r="42"
              stroke="#e5e7eb"
              stroke-width="8"
              fill="none"
            />
            <circle
              cx="50"
              cy="50"
              r="42"
              :stroke="scoreRingColor"
              stroke-width="8"
              fill="none"
              stroke-linecap="round"
              :stroke-dasharray="`${checkResult.overallScore * 2.64} 264`"
              class="transition-all duration-700 ease-out"
            />
          </svg>
          <div class="absolute inset-0 flex flex-col items-center justify-center">
            <span :class="['text-3xl font-bold', scoreColor]">{{ checkResult.overallScore }}</span>
            <span class="text-xs text-gray-500">{{ scoreText }}</span>
          </div>
        </div>

        <div class="flex-1">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">{{ title }}</h2>
          <div class="grid grid-cols-3 gap-3">
            <div class="text-center p-2 bg-green-50 rounded-lg">
              <p class="text-xl font-bold text-green-600">{{ checkResult.passedItems }}</p>
              <p class="text-xs text-green-600">通过</p>
            </div>
            <div class="text-center p-2 bg-yellow-50 rounded-lg">
              <p class="text-xl font-bold text-yellow-600">{{ checkResult.warningItems }}</p>
              <p class="text-xs text-yellow-600">警告</p>
            </div>
            <div class="text-center p-2 bg-red-50 rounded-lg">
              <p class="text-xl font-bold text-red-600">{{ checkResult.errorItems }}</p>
              <p class="text-xs text-red-600">问题</p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="videoPreview" class="mb-6">
        <div class="aspect-video bg-gray-900 rounded-xl overflow-hidden">
          <video :src="videoPreview" class="w-full h-full object-contain" muted />
        </div>
      </div>

      <div class="space-y-3 mb-6">
        <div
          v-for="item in checkResult.checks"
          :key="item.id"
          :class="['p-4 rounded-xl border-2 transition-all', getSeverityColor(item.severity)]"
        >
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0 mt-0.5">
              <component :is="getItemIcon(item.icon)" class="w-5 h-5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h3 class="font-medium">{{ item.title }}</h3>
                <component 
                  :is="getSeverityIcon(item.severity)" 
                  class="w-4 h-4 flex-shrink-0" 
                />
              </div>
              <p class="text-sm opacity-80 mb-2">{{ item.description }}</p>
              <p v-if="item.suggestion" class="text-sm font-medium">
                <span class="opacity-70">建议：</span>{{ item.suggestion }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-3">
        <button
          class="flex-1 py-3 border-2 border-gray-200 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-colors"
          @click="emit('back')"
        >
          返回修改
        </button>
        <button
          :class="[
            'flex-1 py-3 font-medium rounded-xl transition-all',
            canPublish 
              ? 'bg-gradient-to-r from-primary to-orange-400 text-white hover:opacity-90' 
              : 'bg-gray-200 text-gray-400 cursor-not-allowed'
          ]"
          :disabled="!canPublish"
          @click="canPublish && emit('publish')"
        >
          {{ canPublish ? '确认发布' : '请先修复问题' }}
        </button>
      </div>

      <p class="text-xs text-gray-400 text-center mt-4">
        体检结果仅供参考，最终以平台审核为准
      </p>
    </div>
  </div>
</template>
