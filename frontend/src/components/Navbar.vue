<script setup lang="ts">
import { Search, Plus, User, Home, LayoutGrid } from 'lucide-vue-next'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push('/')
  }
}

function goTo(path: string) {
  router.push(path)
}
</script>

<template>
  <nav class="fixed top-0 left-0 right-0 bg-white/95 backdrop-blur-sm shadow-sm z-50">
    <div class="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
      <div class="flex items-center gap-2 cursor-pointer" @click="goTo('/')">
        <div class="w-8 h-8 bg-gradient-to-br from-primary to-orange-400 rounded-lg flex items-center justify-center">
          <Play class="w-5 h-5 text-white fill-white" />
        </div>
        <span class="font-bold text-xl text-gray-900">打卡短视频</span>
      </div>
      
      <div class="hidden md:flex flex-1 max-w-md mx-8">
        <div class="relative w-full">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索视频..."
            class="w-full pl-10 pr-4 py-2 bg-gray-100 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            @keyup.enter="handleSearch"
          />
        </div>
      </div>
      
      <div class="flex items-center gap-4">
        <button 
          class="p-2 hover:bg-gray-100 rounded-full transition-colors"
          @click="goTo('/')"
          title="首页"
        >
          <Home class="w-6 h-6 text-gray-600" />
        </button>
        
        <button 
          class="p-2 hover:bg-gray-100 rounded-full transition-colors"
          @click="goTo('/upload')"
          title="发布"
        >
          <Plus class="w-6 h-6 text-gray-600" />
        </button>
        
        <button 
          class="p-2 hover:bg-gray-100 rounded-full transition-colors"
          @click="goTo('/admin')"
          title="管理"
        >
          <LayoutGrid class="w-6 h-6 text-gray-600" />
        </button>
        
        <button 
          class="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-orange-400 flex items-center justify-center hover:scale-105 transition-transform"
          @click="goTo('/profile')"
          title="个人中心"
        >
          <User class="w-5 h-5 text-white" />
        </button>
      </div>
    </div>
  </nav>
</template>
