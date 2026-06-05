<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft } from 'lucide-vue-next'
import VideoPlayerModal from '@/components/VideoPlayer.vue'
import { videoApi } from '@/api'
import type { Video } from '@/types'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const video = ref<Video | null>(null)
const loading = ref(true)

function goBack() {
  router.back()
}

onMounted(() => {
  const id = route.params.id as string
  videoApi.getVideo(id).then(res => {
    video.value = res.data.data
    loading.value = false
  })
})
</script>

<template>
  <div class="min-h-screen bg-black">
    <button 
      class="fixed top-4 left-4 z-50 w-10 h-10 bg-black/50 hover:bg-black/70 rounded-full flex items-center justify-center transition-colors"
      @click="goBack"
    >
      <ArrowLeft class="w-6 h-6 text-white" />
    </button>
    
    <div v-if="loading" class="flex items-center justify-center h-screen">
      <div class="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin" />
    </div>
    
    <VideoPlayerModal 
      v-if="video"
      :video="video"
      @close="goBack"
    />
  </div>
</template>
