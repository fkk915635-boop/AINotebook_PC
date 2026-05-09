<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Heart, UserPlus, UserCheck, MessageSquare, Send } from 'lucide-vue-next'
import http from '../services/http'

const route = useRoute()
const router = useRouter()

const blog = ref(null)
const isLoading = ref(false)
const isLiking = ref(false)
const isFollowing = ref(false)
const comments = ref([])
const isLoadingComments = ref(false)
const isSendingComment = ref(false)
const commentInput = ref('')

const blogId = computed(() => Number(route.params.id))
const defaultAvatar = (userId) => `https://api.dicebear.com/7.x/bottts/svg?seed=${userId || 'ainotebook'}`
const resolveAvatar = (avatar, userId) => {
  if (!avatar) return defaultAvatar(userId)
  if (avatar.startsWith('http')) return avatar
  if (avatar.startsWith('/api/')) return avatar
  if (avatar.startsWith('/auth/')) return `/api${avatar}`
  if (avatar.startsWith('uploads/')) return `/api/auth/${avatar}`
  return defaultAvatar(userId)
}

async function fetchDetail() {
  isLoading.value = true
  try {
    const res = await http.get(`/blog/${blogId.value}`)
    blog.value = res.data
  } finally {
    isLoading.value = false
  }
}

async function fetchComments() {
  isLoadingComments.value = true
  try {
    const res = await http.get(`/blog/${blogId.value}/comments`, { params: { current: 1, size: 50 } })
    comments.value = res.data || []
  } finally {
    isLoadingComments.value = false
  }
}

async function sendComment() {
  const content = commentInput.value.trim()
  if (!content || isSendingComment.value) return
  isSendingComment.value = true
  try {
    await http.post(`/blog/${blogId.value}/comments`, { content })
    commentInput.value = ''
    await fetchComments()
  } finally {
    isSendingComment.value = false
  }
}

async function toggleLike() {
  if (!blog.value || isLiking.value) return
  isLiking.value = true
  try {
    await http.post(`/blog/like/${blog.value.id}`)
    await fetchDetail()
  } finally {
    isLiking.value = false
  }
}

async function toggleFollow() {
  if (!blog.value || isFollowing.value) return
  if (blog.value.mine) return
  isFollowing.value = true
  try {
    const res = await http.post(`/follow/toggle/${blog.value.userId}`)
    const followed = !!res?.data?.followed
    blog.value.followed = followed
  } finally {
    isFollowing.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchComments()
})
</script>

<template>
  <div class="max-w-4xl mx-auto py-8 px-4">
    <div class="flex items-center justify-between mb-8">
      <button
        class="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-wood-200 bg-white/60 text-wood-500 hover:bg-white transition-colors"
        @click="router.back()"
      >
        <ArrowLeft :size="18" />
        返回
      </button>
    </div>

    <div v-if="isLoading" class="flex flex-col items-center py-20">
      <div class="w-12 h-12 border-4 border-wood-200 border-t-wood-accent rounded-full animate-spin mb-4"></div>
      <p class="text-wood-300">加载中...</p>
    </div>

    <div v-else-if="!blog" class="text-center py-20 bg-white/50 rounded-[2rem] border-2 border-dashed border-wood-200">
      <div class="text-6xl mb-6">📝</div>
      <p class="text-wood-300 text-lg">博客不存在或已被删除</p>
    </div>

    <div v-else class="bg-white rounded-[2rem] p-8 shadow-sm border border-wood-100">
      <div class="flex items-start justify-between gap-6 mb-8">
        <div class="flex items-center gap-4">
          <img
            :src="resolveAvatar(blog.authorAvatar, blog.userId)"
            alt=""
            class="w-12 h-12 rounded-full object-cover bg-wood-50 border border-wood-100"
          />
          <div>
            <div class="text-lg font-bold text-wood-500">{{ blog.authorNickname || `用户 #${blog.userId}` }}</div>
            <div class="text-sm text-wood-300">{{ blog.createdAt }}</div>
          </div>
        </div>

        <button
          class="px-4 py-1.5 rounded-full transition-all duration-300 font-medium inline-flex items-center gap-2"
          :class="blog.mine ? 'bg-wood-50 text-wood-300 cursor-not-allowed' : (blog.followed ? 'bg-wood-accent text-white hover:bg-[#8D7352]' : 'text-wood-accent border border-wood-accent hover:bg-wood-accent hover:text-white hover:-translate-y-1 hover:shadow-lg')"
          @click="toggleFollow"
          :disabled="blog.mine"
        >
          <UserCheck v-if="blog.mine || blog.followed" :size="16" />
          <UserPlus v-else :size="16" />
          {{ blog.mine ? '我自己' : (blog.followed ? '已关注' : '+ 关注') }}
        </button>
      </div>

      <h1 class="text-3xl font-bold text-wood-500 mb-4">{{ blog.title }}</h1>

      <div v-if="blog.summary" class="bg-wood-50/50 rounded-2xl p-6 border-l-4 border-wood-accent mb-6">
        <p class="text-wood-400 text-sm italic mb-2">AI 摘要：</p>
        <p class="text-wood-500 leading-relaxed whitespace-pre-wrap">{{ blog.summary }}</p>
      </div>

      <article class="text-wood-500 leading-loose whitespace-pre-wrap text-lg mb-8">{{ blog.content }}</article>

      <div class="flex items-center gap-4 pt-6 border-t border-wood-50">
        <button
          class="flex items-center gap-2 px-4 py-2 rounded-xl transition-all"
          :class="blog.likedByMe ? 'text-red-500 bg-red-50 shadow-inner' : 'text-wood-300 hover:bg-wood-50 hover:shadow-md hover:-translate-y-1'"
          @click="toggleLike"
        >
          <Heart :size="20" :fill="blog.likedByMe ? 'currentColor' : 'none'" />
          <span class="font-bold">{{ blog.liked || 0 }}</span>
        </button>
      </div>

      <div id="comments" class="mt-8 pt-8 border-t border-wood-50">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2 text-wood-500 font-bold">
            <MessageSquare :size="18" class="text-wood-accent" />
            评论
          </div>
          <div class="text-wood-300 text-sm">{{ comments.length }} 条</div>
        </div>

        <div class="bg-wood-50/40 border border-wood-100 rounded-2xl p-4">
          <textarea
            v-model="commentInput"
            class="w-full min-h-[88px] px-4 py-3 rounded-2xl border border-wood-100 bg-white/70 focus:outline-none focus:border-wood-accent resize-none text-wood-500"
            placeholder="写下你的评论..."
            maxlength="300"
          ></textarea>
          <div class="mt-3 flex items-center justify-between">
            <div class="text-xs text-wood-300">{{ (commentInput || '').length }}/300</div>
            <button
              class="px-5 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md disabled:opacity-60 inline-flex items-center gap-2"
              :disabled="isSendingComment || !commentInput.trim()"
              type="button"
              @click="sendComment"
            >
              <Send :size="16" />
              发表评论
            </button>
          </div>
        </div>

        <div v-if="isLoadingComments" class="flex items-center gap-3 text-wood-300 text-sm mt-5">
          <div class="w-5 h-5 border-2 border-wood-200 border-t-wood-accent rounded-full animate-spin"></div>
          加载评论中...
        </div>

        <div v-else-if="comments.length === 0" class="mt-6 text-wood-300 text-sm bg-white/60 border border-wood-100 rounded-2xl p-6 text-center">
          还没有评论，来做第一个发言的人吧。
        </div>

        <div v-else class="mt-6 space-y-3">
          <div v-for="c in comments" :key="c.id" class="bg-white/70 border border-wood-100 rounded-2xl p-4">
            <div class="flex items-center gap-3">
              <img :src="resolveAvatar(c.authorAvatar, c.userId)" alt="" class="w-9 h-9 rounded-full object-cover bg-wood-50 border border-wood-100" />
              <div class="min-w-0 flex-1">
                <div class="text-wood-500 font-bold truncate">{{ c.authorNickname || `用户 #${c.userId}` }}</div>
                <div class="text-wood-300 text-xs">{{ c.createdAt }}</div>
              </div>
              <div v-if="c.mine" class="text-xs px-2 py-1 rounded-full bg-wood-50 border border-wood-100 text-wood-400">我</div>
            </div>
            <div class="mt-3 text-wood-500 whitespace-pre-wrap leading-relaxed">{{ c.content }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

