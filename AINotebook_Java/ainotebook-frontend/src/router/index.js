import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Auth from '../views/Auth.vue'
import { getToken } from '../services/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: Auth, meta: { public: true } },
    { path: '/', name: 'home', component: Home },
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
