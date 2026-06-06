<script setup lang="ts">
import { ref, computed } from 'vue'
import { TrendingUp, Users, Flame, Play, ArrowUp, Award, Clock } from 'lucide-vue-next'
import type { MorningReport, TrendingVideo, HotTag, NewAuthor } from '@/types'

const props = defineProps<{
  report: MorningReport | null
  loading?: boolean
}>()

const emit = defineEmits<{
  tagClick: [tag: HotTag]
  authorClick: [author: NewAuthor]
  videoClick: [video: TrendingVideo]
}>()

const activeTab = ref<'tags' | 'authors' | 'videos'>('tags')

const tabs = [
  { key: 'tags', label: '热门标签', icon: Flame },
  { key: 'authors', label: '新锐作者', icon: Users },
  { key: 'videos', label: '涨势视频', icon: TrendingUp }
] as const

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toString()
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

const reportDate = computed(() => {
  if (!props.report?.reportDate) return ''
  return formatDate(props.report.reportDate)
})
</script>

<template>
  <div class="mb-8">
    <div class="bg-gradient-to-r from-orange-500 via-primary to-red-500 rounded-2xl p-6 text-white shadow-lg">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-xl bg-white/20 backdrop-blur flex items-center justify-center">
            <Clock class="w-6 h-6" />
          </div>
          <div>
            <h2 class="text-xl font-bold">兴趣晨报</h2>
            <p class="text-sm text-white/80">
              {{ reportDate || '今日' }} · 一屏掌握社区动态
            </p>
          </div>
        </div>
        <div class="flex items-center gap-1 px-3 py-1.5 bg-white/20 backdrop-blur rounded-full text-sm">
          <Award class="w-4 h-4" />
          <span>实时更新</span>
        </div>
      </div>

      <div class="flex gap-2 mb-5">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all"
          :class="activeTab === tab.key 
            ? 'bg-white text-primary shadow-md' 
            : 'bg-white/15 hover:bg-white/25 text-white'"
          @click="activeTab = tab.key"
        >
          <component :is="tab.icon" class="w-4 h-4" />
          <span>{{ tab.label }}</span>
          <span 
            v-if="report && ((tab.key === 'tags' && report.hotTags.length > 0) || (tab.key === 'authors' && report.newAuthors.length > 0) || (tab.key === 'videos' && report.trendingVideos.length > 0))"
            class="ml-1 px-1.5 py-0.5 text-xs rounded-full"
            :class="activeTab === tab.key ? 'bg-primary/10 text-primary' : 'bg-white/20 text-white'"
          >
            {{ tab.key === 'tags' ? report?.hotTags.length : tab.key === 'authors' ? report?.newAuthors.length : report?.trendingVideos.length }}
          </span>
        </button>
      </div>

      <div v-if="loading" class="h-48 flex items-center justify-center">
        <div class="w-8 h-8 border-3 border-white/30 border-t-white rounded-full animate-spin" />
      </div>

      <div v-else-if="!report" class="h-48 flex items-center justify-center">
        <p class="text-white/60">暂无数据</p>
      </div>

      <div v-else class="bg-white/10 backdrop-blur rounded-xl p-4 min-h-48">
        <div v-show="activeTab === 'tags'" class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div
            v-for="(tag, index) in report.hotTags"
            :key="tag.id"
            class="group cursor-pointer bg-white/15 hover:bg-white/25 rounded-xl p-3 transition-all hover:scale-105"
            @click="emit('tagClick', tag)"
          >
            <div class="flex items-center gap-2 mb-2">
              <span 
                class="w-6 h-6 rounded-lg flex items-center justify-center text-xs font-bold"
                :class="index < 3 ? 'bg-yellow-400 text-yellow-900' : 'bg-white/20 text-white'"
              >
                {{ index + 1 }}
              </span>
              <span v-if="index < 3" class="flex items-center text-yellow-300">
                <Flame class="w-4 h-4" />
              </span>
              <span v-else class="flex items-center text-white/60">
                <TrendingUp class="w-4 h-4" />
              </span>
            </div>
            <h3 class="font-semibold text-white mb-1">{{ tag.name }}</h3>
            <div class="flex items-center gap-2 text-xs text-white/70">
              <span>{{ formatNumber(tag.videoCount) }} 视频</span>
              <span>·</span>
              <span>{{ formatNumber(tag.viewCount) }} 播放</span>
            </div>
          </div>
        </div>

        <div v-show="activeTab === 'authors'" class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <div
            v-for="author in report.newAuthors"
            :key="author.id"
            class="group cursor-pointer bg-white/15 hover:bg-white/25 rounded-xl p-4 transition-all hover:scale-105"
            @click="emit('authorClick', author)"
          >
            <div class="flex items-center gap-3 mb-3">
              <div class="relative">
                <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center overflow-hidden">
                  <img 
                    v-if="author.avatar" 
                    :src="author.avatar" 
                    :alt="author.username"
                    class="w-full h-full object-cover"
                  />
                  <span v-else class="text-lg font-bold text-white/80">
                    {{ author.username?.charAt(0) || 'U' }}
                  </span>
                </div>
                <div class="absolute -bottom-1 -right-1 w-5 h-5 bg-green-400 rounded-full flex items-center justify-center">
                  <Award class="w-3 h-3 text-green-900" />
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="font-semibold text-white truncate">{{ author.username }}</h3>
                <p class="text-xs text-white/60 truncate">{{ author.bio || '新锐创作者' }}</p>
              </div>
            </div>
            <div class="flex items-center justify-between text-xs">
              <div class="text-white/70">
                <span class="font-semibold text-white">{{ author.videoCount }}</span> 作品
              </div>
              <div class="text-white/70">
                <span class="font-semibold text-white">{{ formatNumber(author.followers) }}</span> 粉丝
              </div>
            </div>
          </div>
        </div>

        <div v-show="activeTab === 'videos'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="(video, index) in report.trendingVideos"
            :key="video.id"
            class="group cursor-pointer bg-white/15 hover:bg-white/25 rounded-xl overflow-hidden transition-all hover:scale-105"
            @click="emit('videoClick', video)"
          >
            <div class="relative aspect-video bg-black/30">
              <img 
                v-if="video.coverUrl"
                :src="video.coverUrl"
                :alt="video.title"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full bg-gradient-to-br from-primary/40 to-purple-500/40 flex items-center justify-center">
                <Play class="w-10 h-10 text-white/60" />
              </div>
              
              <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <div class="w-12 h-12 rounded-full bg-white/90 flex items-center justify-center">
                  <Play class="w-6 h-6 text-primary ml-0.5" />
                </div>
              </div>

              <div class="absolute top-2 left-2 px-2 py-1 bg-red-500 rounded-full text-xs font-bold flex items-center gap-1">
                <ArrowUp class="w-3 h-3" />
                <span>{{ video.growthRate }}%</span>
              </div>

              <div class="absolute top-2 right-2 px-2 py-1 bg-black/60 rounded-lg text-xs font-semibold">
                #{{ index + 1 }}
              </div>
            </div>
            
            <div class="p-3">
              <h3 class="font-medium text-white text-sm line-clamp-2 mb-2 h-10">
                {{ video.title }}
              </h3>
              <div class="flex items-center justify-between text-xs text-white/70">
                <span class="truncate">{{ video.author?.username || '未知作者' }}</span>
                <div class="flex items-center gap-1 flex-shrink-0">
                  <TrendingUp class="w-3 h-3" />
                  <span>{{ formatNumber(video.viewCount) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
