<script setup>
import { ref, onMounted } from 'vue'
import { Heart, MessageSquare, User, Clock, Plus, Trophy, UserPlus, UserCheck } from 'lucide-vue-next'
import http from '../services/http'
import gsap from 'gsap'
import { useRouter } from 'vue-router'

const blogs = ref([])
const isLoading = ref(false)
const likeRank = ref([])
const isPublishing = ref(false)
const isOptimizing = ref(false)
const showPublish = ref(false)
const showOptimizePreview = ref(false)
const publishForm = ref({ title: '', content: '', apiKey: '' })
const optimized = ref({ title: '', content: '' })
const router = useRouter()

const fetchFeed = async () => {
  isLoading.value = true
  try {
    const response = await http.get('/blog/feed')
    blogs.value = response.data
  } catch (error) {
    console.error('获取社区内容失败', error)
  } finally {
    isLoading.value = false
  }
}

const fetchLikeRank = async () => {
  try {
    const res = await http.get('/blog/like-rank', { params: { size: 3 } })
    likeRank.value = res.data || []
  } catch (e) {
    likeRank.value = []
  }
}

const defaultAvatar = (userId) => `https://api.dicebear.com/7.x/bottts/svg?seed=${userId || 'ainotebook'}`

const resolveAvatar = (avatar, userId) => {
  if (!avatar) return defaultAvatar(userId)
  if (avatar.startsWith('http')) return avatar
  if (avatar.startsWith('/api/')) return avatar
  if (avatar.startsWith('/auth/')) return `/api${avatar}`
  if (avatar.startsWith('uploads/')) return `/api/auth/${avatar}`
  return defaultAvatar(userId)
}

const onAvatarError = (e, userId) => {
  e.target.src = defaultAvatar(userId)
}

const likeBlog = async (id, event) => {
  // GSAP 3D 缩放点赞动画（轻量替代复杂 Three.js 物理引擎）
  const btn = event.currentTarget
  gsap.fromTo(btn, 
    { scale: 0.8, rotationX: 45, rotationY: 20 }, 
    { scale: 1.2, rotationX: 0, rotationY: 0, duration: 0.4, ease: "elastic.out(1, 0.3)", yoyo: true, repeat: 1 }
  )

  try {
    await http.post(`/blog/like/${id}`)
    await fetchFeed()
    await fetchLikeRank()
  } catch (error) {
    console.error('点赞失败', error)
  }
}

const toggleFollow = async (userId) => {
  try {
    const blog = blogs.value.find((b) => b.userId === userId)
    if (blog && blog.mine) return
    const res = await http.post(`/follow/toggle/${userId}`)
    const followed = !!res?.data?.followed
    const idx = blogs.value.findIndex((b) => b.userId === userId)
    if (idx >= 0) {
      blogs.value = blogs.value.map((b) => (b.userId === userId ? { ...b, followed } : b))
    }
  } catch (e) {
    alert(e?.response?.data?.message || '关注失败')
  }
}

const openPublish = () => {
  publishForm.value = { title: '', content: '', apiKey: '' }
  showPublish.value = true
}

const optimize = async () => {
  if (isOptimizing.value) return
  const title = publishForm.value.title.trim()
  const content = publishForm.value.content.trim()
  if (!title || title.length < 2) {
    alert('标题至少 2 个字')
    return
  }
  if (!content || content.length < 5) {
    alert('内容至少 5 个字')
    return
  }
  isOptimizing.value = true
  try {
    const res = await http.post(`/blog/optimize?apiKey=${encodeURIComponent(publishForm.value.apiKey || '')}`, {
      title,
      content
    })
    optimized.value = {
      title: res?.data?.title ?? title,
      content: res?.data?.content ?? content
    }
    showOptimizePreview.value = true
  } catch (e) {
    alert(e?.response?.data?.message || '内容优化失败，请稍后再试')
  } finally {
    isOptimizing.value = false
  }
}

const applyOptimized = () => {
  publishForm.value.title = optimized.value.title || publishForm.value.title
  publishForm.value.content = optimized.value.content || publishForm.value.content
  showOptimizePreview.value = false
}

const publish = async () => {
  if (isPublishing.value) return
  const title = publishForm.value.title.trim()
  const content = publishForm.value.content.trim()
  if (!title || title.length < 2) {
    alert('标题至少 2 个字')
    return
  }
  if (!content || content.length < 5) {
    alert('内容至少 5 个字')
    return
  }
  isPublishing.value = true
  try {
    await http.post(`/blog/publish?apiKey=${encodeURIComponent(publishForm.value.apiKey || '')}`, {
      title,
      content
    })
    showPublish.value = false
    await fetchFeed()
    await fetchLikeRank()
  } catch (e) {
    alert(e?.response?.data?.message || '发布失败，请稍后再试')
  } finally {
    isPublishing.value = false
  }
}

const formatDateTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchFeed()
  fetchLikeRank()
})
</script>

<template>
  <div class="max-w-4xl mx-auto py-8 px-4">
    <div class="flex items-center justify-between mb-8">
      <h2 class="text-3xl font-bold text-wood-500 flex items-center gap-3">
        <span class="w-2 h-8 bg-wood-accent rounded-full"></span>
        笔记广场
      </h2>
      <button
        class="px-5 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md active:scale-95 inline-flex items-center gap-2"
        @click="openPublish"
      >
        <Plus :size="18" />
        发布博客
      </button>
    </div>

    <div v-if="likeRank.length" class="bg-white/70 rounded-[2rem] border border-wood-100 p-6 mb-8">
      <div class="flex items-center gap-2 text-wood-500 font-bold mb-4">
        <Trophy :size="18" class="text-wood-accent" />
        点赞排行榜
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        <button
          v-for="(item, idx) in likeRank.slice(0, 3)"
          :key="item.id"
          class="flex items-center gap-3 p-3 rounded-2xl hover:bg-white transition-colors text-left"
          @click="router.push(`/blog/${item.id}`)"
        >
          <div class="w-8 text-sm font-bold text-wood-300">#{{ idx + 1 }}</div>
          <img :src="resolveAvatar(item.authorAvatar, item.userId)" alt="" class="w-9 h-9 rounded-full object-cover bg-wood-50 border border-wood-100" @error="(e) => onAvatarError(e, item.userId)">
          <div class="flex-1 min-w-0">
            <div class="text-wood-500 font-bold truncate">{{ item.title }}</div>
            <div class="text-wood-300 text-xs truncate">{{ item.authorNickname || `用户 #${item.userId}` }}</div>
          </div>
          <div class="text-red-500 font-bold text-sm inline-flex items-center gap-1">
            <Heart :size="16" fill="currentColor" />
            {{ item.liked || 0 }}
          </div>
        </button>
      </div>
    </div>

    <div v-if="isLoading" class="flex flex-col items-center py-20">
      <div class="w-12 h-12 border-4 border-wood-200 border-t-wood-accent rounded-full animate-spin mb-4"></div>
      <p class="text-wood-300">正在探索思维森林...</p>
    </div>

    <div v-else-if="blogs.length === 0" class="text-center py-20 bg-white/50 rounded-[2rem] border-2 border-dashed border-wood-200">
      <div class="text-6xl mb-6">🌱</div>
      <p class="text-wood-300 text-lg">社区还在萌芽中，快去分享你的第一条灵感吧！</p>
    </div>

    <div v-else class="grid grid-cols-1 gap-8">
      <div 
        v-for="blog in blogs" 
        :key="blog.id"
        class="bg-white rounded-[2rem] p-8 shadow-sm hover:shadow-xl transition-all duration-300 group border border-transparent hover:border-wood-100 hover:rotate-1 hover:scale-[1.01]"
        style="transform-style: preserve-3d; perspective: 1000px;"
        @click="router.push(`/blog/${blog.id}`)"
      >
        <div class="flex items-start justify-between mb-6">
          <div class="flex items-center gap-4">
            <!-- 3D 头像占位效果 -->
            <img
              :src="resolveAvatar(blog.authorAvatar, blog.userId)"
              alt=""
              class="w-12 h-12 rounded-full object-cover bg-wood-50 border border-wood-100 shadow-inner transition-transform duration-500 group-hover:rotate-180 group-hover:scale-110"
              @error="(e) => onAvatarError(e, blog.userId)"
            />
            <div>
              <h3 class="font-bold text-wood-500 text-lg">{{ blog.authorNickname || `用户 #${blog.userId}` }}</h3>
              <p class="text-wood-300 text-sm flex items-center gap-1">
                <Clock :size="14" />
                {{ formatDateTime(blog.createdAt) }}
              </p>
            </div>
          </div>
          <!-- 关注按钮 3D 样式 -->
          <button
            class="px-4 py-1.5 rounded-full transition-all duration-300 font-medium inline-flex items-center gap-2"
            :class="blog.mine ? 'bg-wood-50 text-wood-300 cursor-not-allowed' : (blog.followed ? 'bg-wood-accent text-white hover:bg-[#8D7352]' : 'text-wood-accent border border-wood-accent hover:bg-wood-accent hover:text-white hover:-translate-y-1 hover:shadow-lg')"
            @click.stop="toggleFollow(blog.userId)"
            :disabled="blog.mine"
          >
            <UserCheck v-if="blog.mine || blog.followed" :size="16" />
            <UserPlus v-else :size="16" />
            {{ blog.mine ? '我自己' : (blog.followed ? '已关注' : '+ 关注') }}
          </button>
        </div>

        <div class="mb-6">
          <h4 class="text-xl font-bold text-wood-500 mb-3 group-hover:text-wood-accent transition-colors">
            {{ blog.title }}
          </h4>
          <div class="bg-wood-50/50 rounded-2xl p-6 border-l-4 border-wood-accent mb-4 transform transition-transform group-hover:translate-z-4">
            <p class="text-wood-400 text-sm italic mb-2">AI 摘要：</p>
            <p class="text-wood-500 leading-relaxed">{{ blog.summary }}</p>
          </div>
          <p class="text-wood-400 line-clamp-3 leading-relaxed">
            {{ blog.content }}
          </p>
        </div>

        <div class="flex items-center gap-6 pt-6 border-t border-wood-50">
          <button 
            @click="likeBlog(blog.id, $event)"
            class="flex items-center gap-2 px-4 py-2 rounded-xl transition-all"
            :class="blog.likedByMe ? 'text-red-500 bg-red-50 shadow-inner' : 'text-wood-300 hover:bg-wood-50 hover:shadow-md hover:-translate-y-1'"
            style="transform-style: preserve-3d;"
            @click.stop
          >
            <Heart :size="20" :fill="blog.likedByMe ? 'currentColor' : 'none'" />
            <span class="font-bold">{{ blog.liked || 0 }}</span>
          </button>
          
          <button class="flex items-center gap-2 px-4 py-2 text-wood-300 hover:bg-wood-50 rounded-xl transition-all hover:shadow-md hover:-translate-y-1" @click.stop="router.push(`/blog/${blog.id}#comments`)">
            <MessageSquare :size="20" />
            <span class="font-bold">评论</span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="showPublish" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-wood-500/50 backdrop-blur-sm">
      <div class="bg-white w-full max-w-2xl rounded-[2rem] shadow-2xl border border-wood-100 overflow-hidden">
        <div class="p-8">
          <div class="flex items-center justify-between mb-6">
            <div class="text-2xl font-bold text-wood-500">发布博客</div>
            <button class="px-3 py-2 rounded-xl hover:bg-wood-50 text-wood-400" @click="showPublish = false">关闭</button>
          </div>
          <div class="space-y-4">
            <input
              v-model="publishForm.title"
              type="text"
              placeholder="标题（至少2个字）"
              class="w-full px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent"
              maxlength="100"
            />
            <textarea
              v-model="publishForm.content"
              placeholder="写下你想分享的内容..."
              class="w-full min-h-[180px] px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent resize-none"
            ></textarea>
            <input
              v-model="publishForm.apiKey"
              type="password"
              placeholder="API Key（留空使用模拟摘要）"
              class="w-full px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent"
            />
          </div>
          <div class="mt-6 flex items-center justify-end gap-3">
            <button class="px-5 py-2 rounded-xl border border-wood-200 text-wood-500 hover:bg-wood-50" @click="showPublish = false">取消</button>
            <button
              class="px-5 py-2 rounded-xl border border-wood-200 text-wood-500 hover:bg-wood-50 disabled:opacity-60"
              :disabled="isOptimizing"
              @click="optimize"
            >
              {{ isOptimizing ? '优化中...' : '内容优化' }}
            </button>
            <button
              class="px-6 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md disabled:opacity-60"
              :disabled="isPublishing"
              @click="publish"
            >
              {{ isPublishing ? '发布中...' : '发布' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showOptimizePreview" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-wood-500/50 backdrop-blur-sm">
      <div class="bg-white w-full max-w-2xl rounded-[2rem] shadow-2xl border border-wood-100 overflow-hidden">
        <div class="p-8">
          <div class="flex items-center justify-between mb-6">
            <div class="text-2xl font-bold text-wood-500">内容优化预览</div>
            <button class="px-3 py-2 rounded-xl hover:bg-wood-50 text-wood-400" @click="showOptimizePreview = false">关闭</button>
          </div>
          <div class="space-y-4">
            <input
              v-model="optimized.title"
              type="text"
              class="w-full px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent"
              maxlength="100"
            />
            <textarea
              v-model="optimized.content"
              class="w-full min-h-[220px] px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent resize-none"
            ></textarea>
          </div>
          <div class="mt-6 flex items-center justify-end gap-3">
            <button class="px-5 py-2 rounded-xl border border-wood-200 text-wood-500 hover:bg-wood-50" @click="showOptimizePreview = false">取消</button>
            <button
              class="px-6 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md"
              @click="applyOptimized"
            >
              确定并替换
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;  
  overflow: hidden;
}
.translate-z-4 {
  transform: translateZ(10px);
}
</style>
