import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import VideoPlayer from '@/views/VideoPlayer.vue'
import Upload from '@/views/Upload.vue'
import Profile from '@/views/Profile.vue'
import Admin from '@/views/Admin.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/video/:id',
    name: 'VideoPlayer',
    component: VideoPlayer
  },
  {
    path: '/upload',
    name: 'Upload',
    component: Upload
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile
  },
  {
    path: '/admin',
    name: 'Admin',
    component: Admin
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
