<script setup>
import NoteApp from '../components/NoteApp.vue'
import { clearToken } from '../services/auth'
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  clearToken()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="min-h-screen p-4 md:p-8">
    <header class="max-w-6xl mx-auto mb-12 text-center animate-fade-in">
      <div class="flex items-center justify-center gap-4 mb-4">
        <div class="w-12 h-12 rounded-2xl bg-wood-accent flex items-center justify-center shadow-lg transform rotate-3">
          <span class="text-white text-2xl">🧠</span>
        </div>
        <h1 class="text-4xl font-bold text-wood-500 tracking-tight">
          原木思维空间
          <span class="block text-sm font-normal text-wood-400 mt-1">AINotebook • 记录每一次灵感</span>
        </h1>
      </div>

      <div class="flex items-center justify-center gap-3">
        <router-link
          to="/community"
          class="px-6 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md active:scale-95"
        >
          探索社区
        </router-link>
        <button
          class="px-4 py-2 rounded-xl border border-wood-200 bg-white/60 text-wood-500 hover:bg-white transition-colors"
          @click="logout"
        >
          退出登录
        </button>
      </div>
    </header>

    <main class="max-w-6xl mx-auto">
      <NoteApp />
    </main>

    <footer class="mt-16 text-center text-wood-300 text-sm pb-8">
      <p>© 2026 AINotebook Pro • 极简主义思维管理</p>
    </footer>
  </div>
</template>

<style>
.animate-fade-in {
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

