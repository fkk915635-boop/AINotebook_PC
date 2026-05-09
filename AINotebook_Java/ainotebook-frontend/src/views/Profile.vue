<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { clearToken } from '../services/auth'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import gsap from 'gsap'
import http from '../services/http'

const router = useRouter()

// 从后端接口获取的用户信息
const user = ref({
  phone: '',
  nickname: '加载中...',
  avatar: '',
  bio: '这个家伙很懒，什么也没有留下'
})

const isEditing = ref(false)
const editForm = ref({ nickname: '', bio: '' })
const isUploadingAvatar = ref(false)
const avatarInputRef = ref(null)
const stats = ref({ totalLikes: 0, followerCount: 0, followingCount: 0 })
const myBlogs = ref([])
const isDeletingBlog = ref(false)

// 获取当前用户信息
async function fetchUserProfile() {
  try {
    const res = await http.get('/auth/me')
    if (res.data) {
      user.value = res.data
    }
  } catch (error) {
    console.error('获取用户信息失败', error)
  }
}

async function fetchUserStats() {
  try {
    const res = await http.get('/user/stats')
    if (res.data) stats.value = res.data
  } catch (e) {
  }
}

async function fetchMyBlogs() {
  try {
    const res = await http.get('/blog/my', { params: { size: 20 } })
    myBlogs.value = res.data || []
  } catch (e) {
    myBlogs.value = []
  }
}

async function deleteMyBlog(id) {
  if (isDeletingBlog.value) return
  const ok = window.confirm('确认删除这篇博客吗？')
  if (!ok) return
  isDeletingBlog.value = true
  try {
    await http.delete(`/blog/${id}`)
    await fetchMyBlogs()
    await fetchUserStats()
  } catch (e) {
    alert(e?.response?.data?.message || '删除失败')
  } finally {
    isDeletingBlog.value = false
  }
}

// Three.js 头像相关
const avatarCanvas = ref(null)
let scene, camera, renderer, avatarMesh
let rafId
let textureLoader

function initThreeAvatar() {
  const container = avatarCanvas.value
  if (!container) return

  const width = container.clientWidth
  const height = container.clientHeight

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100)
  camera.position.z = 3

  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  container.appendChild(renderer.domElement)

  // 环境光与点光源呈现 3D 质感
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.7)
  scene.add(ambientLight)
  const pointLight = new THREE.PointLight(0xffdcb4, 1)
  pointLight.position.set(2, 3, 4)
  scene.add(pointLight)

  // 创建一个带有物理材质的 3D 球体作为头像模型基础
  const geometry = new THREE.SphereGeometry(1, 64, 64)
  
  textureLoader = new THREE.TextureLoader()
  const material = new THREE.MeshStandardMaterial({
    color: 0xc19866,
    roughness: 0.2,
    metalness: 0.5
  })

  avatarMesh = new THREE.Mesh(geometry, material)
  scene.add(avatarMesh)
  applyAvatarTexture(user.value.avatar)

  animate()
}

function animate() {
  rafId = requestAnimationFrame(animate)
  if (avatarMesh && !isEditing.value) {
    // 默认缓慢自转
    avatarMesh.rotation.y += 0.005
    avatarMesh.rotation.x += 0.002
  }
  renderer.render(scene, camera)
}

function onAvatarClick() {
  if (!avatarMesh) return
  // 点击头像时的 3D 交互反馈
  gsap.fromTo(avatarMesh.scale, 
    { x: 0.8, y: 0.8, z: 0.8 }, 
    { x: 1, y: 1, z: 1, duration: 0.6, ease: "elastic.out(1, 0.3)" }
  )
  gsap.to(avatarMesh.rotation, {
    y: avatarMesh.rotation.y + Math.PI * 2,
    duration: 1,
    ease: "power2.out"
  })
  if (avatarInputRef.value) avatarInputRef.value.click()
}

function applyAvatarTexture(url) {
  if (!url || !avatarMesh || !textureLoader) return
  textureLoader.load(
    url,
    (tex) => {
      tex.colorSpace = THREE.SRGBColorSpace
      tex.needsUpdate = true
      avatarMesh.material.map = tex
      avatarMesh.material.color.setHex(0xffffff)
      avatarMesh.material.needsUpdate = true
    },
    undefined,
    () => {
    }
  )
}

async function onAvatarFileChange(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  if (isUploadingAvatar.value) return
  isUploadingAvatar.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await http.post('/auth/avatar', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const avatar = res?.data?.avatar
    if (avatar) {
      user.value.avatar = avatar
      applyAvatarTexture(avatar)
    }
  } catch (error) {
    alert(error?.response?.data?.message || error?.message || '头像上传失败，请稍后再试')
  } finally {
    isUploadingAvatar.value = false
    if (avatarInputRef.value) avatarInputRef.value.value = ''
  }
}

function startEdit() {
  isEditing.value = true
  editForm.value.nickname = user.value.nickname
  editForm.value.bio = user.value.bio || '这个家伙很懒，什么也没有留下'
}

async function saveProfile() {
  if (editForm.value.nickname.trim().length < 2 || editForm.value.nickname.trim().length > 10) {
    alert('昵称长度需在2-10个字符之间')
    return
  }
  try {
    await http.put('/auth/profile', { nickname: editForm.value.nickname, bio: editForm.value.bio })
    user.value.nickname = editForm.value.nickname
    user.value.bio = editForm.value.bio
    isEditing.value = false
  } catch (error) {
    alert('保存失败，请稍后再试')
    console.error('更新用户信息失败', error)
  }
}

function logout() {
  clearToken()
  router.push('/login')
}

onMounted(() => {
  ;(async () => {
    await fetchUserProfile()
    await fetchUserStats()
    await fetchMyBlogs()
    initThreeAvatar()
  })()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  if (renderer) renderer.dispose()
})
</script>

<template>
  <div class="max-w-3xl mx-auto py-12 px-4">
    <div class="bg-white rounded-[2rem] shadow-xl p-8 md:p-12 relative overflow-hidden border border-wood-100">
      
      <!-- 页面装饰背景 -->
      <div class="absolute top-0 right-0 w-64 h-64 bg-wood-50 rounded-full blur-3xl opacity-50 -translate-y-1/2 translate-x-1/3"></div>
      
      <h2 class="text-3xl font-bold text-wood-500 mb-10 flex items-center gap-3 relative z-10">
        <span class="w-2 h-8 bg-wood-accent rounded-full"></span>
        个人中心
      </h2>

      <div class="flex flex-col md:flex-row items-center md:items-start gap-12 relative z-10">
        <!-- 3D 头像区域 -->
        <div class="relative group cursor-pointer md:sticky md:top-24 self-start" @click="onAvatarClick">
          <!-- 光晕背景 -->
          <div class="absolute inset-0 bg-wood-accent/20 rounded-full blur-xl group-hover:bg-wood-accent/40 transition-all duration-500 transform group-hover:scale-110"></div>
          
          <!-- Three.js 渲染容器 -->
          <div ref="avatarCanvas" class="w-40 h-40 relative z-10 rounded-full overflow-hidden border-4 border-white shadow-lg"></div>
          
          <div class="absolute bottom-2 right-2 bg-white p-2 rounded-full shadow-md text-wood-400 group-hover:text-wood-accent transition-colors z-20">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
          </div>
          <input ref="avatarInputRef" type="file" accept="image/*" class="hidden" @change="onAvatarFileChange">
        </div>

        <!-- 信息与修改区域 -->
        <div class="flex-1 w-full">
          <div class="grid grid-cols-3 gap-3 mb-8">
            <div class="bg-wood-50/50 rounded-2xl p-4 border border-wood-100 text-center">
              <div class="text-xs text-wood-300 mb-1">累计获赞</div>
              <div class="text-2xl font-bold text-wood-500">{{ stats.totalLikes || 0 }}</div>
            </div>
            <div class="bg-wood-50/50 rounded-2xl p-4 border border-wood-100 text-center">
              <div class="text-xs text-wood-300 mb-1">粉丝</div>
              <div class="text-2xl font-bold text-wood-500">{{ stats.followerCount || 0 }}</div>
            </div>
            <div class="bg-wood-50/50 rounded-2xl p-4 border border-wood-100 text-center">
              <div class="text-xs text-wood-300 mb-1">关注</div>
              <div class="text-2xl font-bold text-wood-500">{{ stats.followingCount || 0 }}</div>
            </div>
          </div>
          <div class="mb-6">
            <p class="text-sm text-wood-300 mb-1 uppercase tracking-wider">手机号 / 账号</p>
            <p class="text-xl font-mono text-wood-500">{{ user.phone }}</p>
          </div>

          <div class="mb-8">
            <p class="text-sm text-wood-300 mb-1 uppercase tracking-wider">昵称</p>
            
            <div v-if="!isEditing" class="flex items-center gap-4 group">
              <!-- 使用 preserve-3d 增加立体感文字效果 -->
              <h3 class="text-2xl font-bold text-wood-500 transition-transform duration-300 group-hover:scale-105 origin-left" style="transform-style: preserve-3d; text-shadow: 1px 1px 0 rgba(0,0,0,0.1);">
                {{ user.nickname }}
              </h3>
              <button @click="startEdit" class="text-wood-300 hover:text-wood-accent transition-colors p-2 bg-wood-50 rounded-xl hover:shadow-md">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
              </button>
            </div>
            
            <div v-else class="flex items-center gap-3">
              <input 
                v-model="editForm.nickname" 
                type="text" 
                class="flex-1 px-4 py-2 border-2 border-wood-100 rounded-xl focus:outline-none focus:border-wood-accent bg-white shadow-inner transition-colors"
                placeholder="2-10个字符"
                maxlength="10"
              >
              <button @click="saveProfile" class="px-6 py-2 bg-wood-accent text-white font-bold rounded-xl shadow-md hover:bg-[#8D7352] hover:-translate-y-0.5 transition-all">保存</button>
              <button @click="isEditing = false" class="px-4 py-2 text-wood-400 hover:bg-wood-50 rounded-xl transition-colors">取消</button>
            </div>
          </div>

          <div class="mb-8">
            <p class="text-sm text-wood-300 mb-2 uppercase tracking-wider">个人简介</p>
            <div v-if="!isEditing" class="bg-wood-50/40 border border-wood-100 rounded-2xl px-4 py-3 text-wood-500 whitespace-pre-wrap">
              {{ user.bio || '这个家伙很懒，什么也没有留下' }}
            </div>
            <div v-else>
              <textarea
                v-model="editForm.bio"
                class="w-full min-h-[96px] px-4 py-3 rounded-2xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent resize-none text-wood-500"
                maxlength="200"
                placeholder="这个家伙很懒，什么也没有留下"
              ></textarea>
              <div class="mt-1 text-right text-xs text-wood-300">{{ (editForm.bio || '').length }}/200</div>
            </div>
          </div>

          <div class="pt-8 border-t border-wood-100/50">
            <!-- 3D 立体风格退出按钮 -->
            <button 
              @click="logout"
              class="flex items-center justify-center gap-2 w-full py-3 text-red-500 font-bold bg-red-50 rounded-xl transition-all duration-300 hover:bg-red-500 hover:text-white hover:shadow-lg hover:-translate-y-1 active:translate-y-0 active:shadow-md"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
              退出登录
            </button>
          </div>

          <div class="mt-8">
            <div class="text-sm font-bold text-wood-500 mb-3">我发布的博客</div>
            <div v-if="myBlogs.length === 0" class="text-wood-300 text-sm bg-wood-50/40 border border-wood-100 rounded-xl px-4 py-3">还没有发布博客，去笔记广场发第一篇吧。</div>
            <div v-else class="space-y-2 max-h-60 overflow-auto pr-1">
              <router-link
                v-for="item in myBlogs"
                :key="item.id"
                :to="`/blog/${item.id}`"
                class="block bg-wood-50/40 border border-wood-100 rounded-xl px-4 py-3 hover:bg-white transition-colors"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0 flex-1">
                    <div class="text-wood-500 font-semibold truncate">{{ item.title }}</div>
                    <div class="text-wood-300 text-xs mt-1 inline-flex items-center gap-3">
                      <span>{{ item.createdAt }}</span>
                      <span>❤ {{ item.liked || 0 }}</span>
                    </div>
                  </div>
                  <button
                    class="shrink-0 px-3 py-1 text-xs rounded-lg border border-red-200 text-red-500 hover:bg-red-50 transition-colors"
                    type="button"
                    @click.prevent.stop="deleteMyBlog(item.id)"
                    :disabled="isDeletingBlog"
                  >
                    删除
                  </button>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
