import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Auth from '../views/AuthAnimated.vue'
import Community from '../views/Community.vue'
import Profile from '../views/Profile.vue'
import BlogDetail from '../views/BlogDetail.vue'
import Chat from '../views/Chat.vue'
import { getToken } from '../services/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: Auth, meta: { public: true } },
    { path: '/', name: 'home', component: Home },
    { path: '/community', name: 'community', component: Community },
    { path: '/chat', name: 'chat', component: Chat },
    { path: '/blog/:id', name: 'blogDetail', component: BlogDetail },
    { path: '/profile', name: 'profile', component: Profile },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
})

router.beforeEach((to) => {
  const token = getToken()
  if (to.name === 'login' && token) return { name: 'home' }
  if (to.meta.public) return true
  if (!token) return { name: 'login', query: { redirect: to.fullPath } }
  return true
})

export default router
