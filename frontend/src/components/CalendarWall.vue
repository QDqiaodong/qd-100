<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ChevronLeft, ChevronRight, Flame, Award, Play, X } from 'lucide-vue-next'
import { videoApi } from '@/api'
import type { CheckInCalendar, DayInfo, Video } from '@/types'

const props = defineProps<{
  userId: string
}>()

const calendarData = ref<CheckInCalendar | null>(null)
const selectedDateVideos = ref<Video[]>([])
const showDateDetail = ref(false)
const selectedDate = ref('')

const currentDate = new Date()
const currentYear = ref(currentDate.getFullYear())
const currentMonth = ref(currentDate.getMonth() + 1)

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const displayYearMonth = computed(() => {
  return `${currentYear.value}年${currentMonth.value}月`
})

const calendarDays = computed(() => {
  if (!calendarData.value) return []
  
  const firstDay = new Date(currentYear.value, currentMonth.value - 1, 1)
  const firstDayOfWeek = firstDay.getDay()
  
  const days: (DayInfo | null)[] = []
  
  for (let i = 0; i < firstDayOfWeek; i++) {
    days.push(null)
  }
  
  calendarData.value.days.forEach(day => {
    days.push(day)
  })
  
  return days
})

function fetchCalendar() {
  videoApi.getCheckInCalendar(props.userId, {
    year: currentYear.value,
    month: currentMonth.value
  }).then(res => {
    calendarData.value = res.data.data
  })
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

async function handleDayClick(day: DayInfo) {
  if (!day.hasVideo) return
  
  selectedDate.value = day.date
  showDateDetail.value = true
  
  const res = await videoApi.getUserVideosByDate(props.userId, day.date)
  selectedDateVideos.value = res.data.data
}

function closeDateDetail() {
  showDateDetail.value = false
  selectedDateVideos.value = []
  selectedDate.value = ''
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekDay = weekDays[date.getDay()]
  return `${month}月${day}日 周${weekDay}`
}

function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

watch([currentYear, currentMonth], () => {
  fetchCalendar()
})

onMounted(() => {
  fetchCalendar()
})
</script>

<template>
  <div class="bg-white rounded-2xl shadow-lg overflow-hidden">
    <div class="bg-gradient-to-r from-orange-500 to-pink-500 p-6 text-white">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold flex items-center gap-2">
          <Flame class="w-6 h-6" />
          打卡日历墙
        </h2>
        <div class="flex items-center gap-2">
          <button 
            @click="prevMonth"
            class="w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
          >
            <ChevronLeft class="w-5 h-5" />
          </button>
          <span class="font-medium min-w-[120px] text-center">{{ displayYearMonth }}</span>
          <button 
            @click="nextMonth"
            class="w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
          >
            <ChevronRight class="w-5 h-5" />
          </button>
        </div>
      </div>
      
      <div v-if="calendarData" class="grid grid-cols-3 gap-4">
        <div class="bg-white/20 rounded-xl p-3 text-center">
          <p class="text-2xl font-bold">{{ calendarData.checkInDays }}</p>
          <p class="text-sm opacity-80">本月打卡</p>
        </div>
        <div class="bg-white/20 rounded-xl p-3 text-center">
          <div class="flex items-center justify-center gap-1">
            <Flame class="w-5 h-5 text-yellow-300" />
            <p class="text-2xl font-bold">{{ calendarData.currentStreak }}</p>
          </div>
          <p class="text-sm opacity-80">连续打卡</p>
        </div>
        <div class="bg-white/20 rounded-xl p-3 text-center">
          <div class="flex items-center justify-center gap-1">
            <Award class="w-5 h-5 text-yellow-300" />
            <p class="text-2xl font-bold">{{ calendarData.longestStreak }}</p>
          </div>
          <p class="text-sm opacity-80">最长连续</p>
        </div>
      </div>
    </div>
    
    <div class="p-6">
      <div class="grid grid-cols-7 gap-1 mb-2">
        <div 
          v-for="day in weekDays" 
          :key="day"
          class="text-center text-sm font-medium text-gray-500 py-2"
        >
          {{ day }}
        </div>
      </div>
      
      <div class="grid grid-cols-7 gap-1">
        <div 
          v-for="(day, index) in calendarDays" 
          :key="index"
          class="aspect-square"
        >
          <div 
            v-if="day"
            @click="handleDayClick(day)"
            class="w-full h-full rounded-lg flex flex-col items-center justify-center transition-all cursor-pointer relative group"
            :class="[
              day.hasVideo 
                ? day.isMostActive 
                  ? 'bg-gradient-to-br from-orange-400 to-pink-500 text-white shadow-md hover:shadow-lg hover:scale-105' 
                  : 'bg-orange-100 text-orange-600 hover:bg-orange-200 hover:scale-105'
                : 'bg-gray-50 text-gray-400 hover:bg-gray-100',
              day.isStreakBroken ? 'ring-2 ring-red-400 ring-offset-1' : ''
            ]"
          >
            <span class="text-sm font-medium">{{ day.dayOfMonth }}</span>
            <span v-if="day.hasVideo && day.videoCount > 1" class="text-xs opacity-80">
              {{ day.videoCount }}个
            </span>
            <div 
              v-if="day.isMostActive"
              class="absolute -top-1 -right-1 w-4 h-4 bg-yellow-400 rounded-full flex items-center justify-center text-xs"
            >
              🔥
            </div>
            <div 
              v-if="day.isStreakBroken"
              class="absolute -bottom-1 text-xs text-red-500 font-bold"
            >
              ⚡
            </div>
          </div>
        </div>
      </div>
      
      <div class="flex items-center justify-center gap-6 mt-6 pt-4 border-t border-gray-100">
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded bg-orange-100"></div>
          <span class="text-sm text-gray-600">已打卡</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded bg-gradient-to-br from-orange-400 to-pink-500"></div>
          <span class="text-sm text-gray-600">最活跃</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded bg-gray-50"></div>
          <span class="text-sm text-gray-600">未打卡</span>
        </div>
      </div>
    </div>
    
    <div 
      v-if="showDateDetail" 
      class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
      @click.self="closeDateDetail"
    >
      <div class="bg-white rounded-2xl w-full max-w-lg max-h-[80vh] overflow-hidden flex flex-col">
        <div class="bg-gradient-to-r from-orange-500 to-pink-500 p-4 text-white flex items-center justify-between">
          <div class="flex items-center gap-2">
            <Play class="w-5 h-5" />
            <h3 class="font-bold">{{ formatDate(selectedDate) }} 的作品</h3>
          </div>
          <button 
            @click="closeDateDetail"
            class="w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <div class="flex-1 overflow-y-auto p-4">
          <div v-if="selectedDateVideos.length === 0" class="text-center py-12">
            <p class="text-gray-500">当天没有发布视频</p>
          </div>
          
          <div v-else class="space-y-4">
            <div 
              v-for="video in selectedDateVideos" 
              :key="video.id"
              class="flex gap-4 p-3 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors cursor-pointer"
            >
              <div class="relative w-32 h-20 bg-black rounded-lg overflow-hidden flex-shrink-0">
                <img 
                  :src="video.coverUrl || 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=video%20thumbnail%20abstract&image_size=square'" 
                  :alt="video.title"
                  class="w-full h-full object-cover"
                />
                <div class="absolute bottom-1 right-1 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded">
                  {{ formatDuration(video.duration) }}
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <h4 class="font-medium text-gray-900 truncate">{{ video.title }}</h4>
                <p v-if="video.description" class="text-sm text-gray-500 mt-1 line-clamp-2">
                  {{ video.description }}
                </p>
                <div class="flex items-center gap-4 mt-2 text-xs text-gray-400">
                  <span>👁 {{ video.viewCount }}</span>
                  <span>❤️ {{ video.likeCount }}</span>
                  <span>⭐ {{ video.favoriteCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
