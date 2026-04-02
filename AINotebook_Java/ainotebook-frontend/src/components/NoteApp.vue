<script setup>
import { ref, onMounted } from 'vue'
import { Save, Brain, Trash2, Clock, CheckCircle, AlertTriangle, X } from 'lucide-vue-next'
import http from '../services/http'

const notes = ref([])
const inputNote = ref('')
const aiResult = ref('')
const isAnalyzing = ref(false)
const showAiModal = ref(false)
const apiKey = ref('')
const message = ref({ text: '', type: '' })

function parseDate(v) {
  if (!v) return null
  if (typeof v === 'string') {
    const s = v.includes('T') ? v : v.replace(' ', 'T')
    const d = new Date(s)
    return Number.isNaN(d.getTime()) ? null : d
  }
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

function formatDateTime(v) {
  const d = parseDate(v)
  if (!d) return ''
  const date = d.toLocaleDateString()
  const time = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  return `${date} ${time}`
}

const fetchNotes = async () => {
  try {
    const response = await http.get('/note/list')
    notes.value = response.data
  } catch (error) {
    showMsg('获取笔记失败', 'error')
  }
}

const saveNote = async () => {
  if (!inputNote.value.trim()) {
    showMsg('请输入内容后再保存', 'warning')
    return
  }
  try {
    await http.post('/note/save', { content: inputNote.value })
    inputNote.value = ''
    fetchNotes()
    showMsg('保存成功', 'success')
  } catch (error) {
    showMsg('保存失败', 'error')
  }
}

const deleteNote = async (id) => {
  try {
    await http.delete(`/note/${id}`)
    fetchNotes()
    showMsg('删除成功', 'success')
  } catch (error) {
    showMsg('删除失败', 'error')
  }
}

const analyzeNotes = async () => {
  if (notes.value.length === 0) {
    showMsg('请先记录至少一条想法', 'warning')
    return
  }
  isAnalyzing.value = true
  try {
    const response = await http.post(`/note/analyze?apiKey=${encodeURIComponent(apiKey.value || '')}`)
    aiResult.value = response.data
    showAiModal.value = true
  } catch (error) {
    showMsg('AI 分析失败', 'error')
  } finally {
    isAnalyzing.value = false
  }
}

const showMsg = (text, type) => {
  message.value = { text, type }
  setTimeout(() => {
    message.value = { text: '', type: '' }
  }, 3000)
}

onMounted(() => {
  fetchNotes()
})
</script>

<template>
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
    <!-- Left: Input Area -->
    <section class="lg:col-span-5 flex flex-col gap-6">
      <div class="bg-white rounded-3xl p-8 shadow-xl wood-card border-none relative overflow-hidden group">
        <div class="absolute top-0 right-0 w-32 h-32 bg-wood-accent/5 rounded-full -mr-16 -mt-16 group-hover:scale-110 transition-transform duration-500"></div>
        <div class="relative">
          <label class="block text-wood-400 font-medium mb-4 flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-wood-accent"></span>
            记录当下的思考
          </label>
          <textarea 
            v-model="inputNote"
            placeholder="今天有什么新的灵感或困惑？"
            class="w-full h-64 p-6 bg-wood-50 rounded-2xl wood-input text-lg resize-none placeholder:text-wood-300"
          ></textarea>
          
          <div class="mt-8 flex flex-wrap gap-4">
            <button 
              @click="saveNote"
              class="flex-1 py-4 px-8 rounded-2xl bg-wood-accent text-white font-bold flex items-center justify-center gap-2 hover:bg-[#8D7352] transform hover:-translate-y-1 transition-all shadow-lg active:scale-95"
            >
              <Save :size="20" />
              保存灵感
            </button>
            <button 
              @click="analyzeNotes"
              :disabled="isAnalyzing"
              class="flex-1 py-4 px-8 rounded-2xl bg-wood-500 text-white font-bold flex items-center justify-center gap-2 hover:bg-[#344955] transform hover:-translate-y-1 transition-all shadow-lg disabled:opacity-50 disabled:cursor-not-allowed active:scale-95"
            >
              <Brain v-if="!isAnalyzing" :size="20" />
              <div v-else class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
              {{ isAnalyzing ? '分析中...' : 'AI 深度洞察' }}
            </button>
          </div>
          
          <div class="mt-6">
            <input 
              v-model="apiKey"
              type="password"
              placeholder="API Key (留空使用模拟模式)"
              class="w-full p-4 bg-wood-50 rounded-xl wood-input text-sm border-dashed"
            />
          </div>
        </div>
      </div>
      
      <!-- Feedback Message -->
      <transition name="fade">
        <div v-if="message.text" 
          class="p-4 rounded-2xl flex items-center gap-3 shadow-sm border"
          :class="{
            'bg-green-50 text-green-700 border-green-100': message.type === 'success',
            'bg-amber-50 text-amber-700 border-amber-100': message.type === 'warning',
            'bg-red-50 text-red-700 border-red-100': message.type === 'error'
          }"
        >
          <CheckCircle v-if="message.type === 'success'" :size="18" />
          <AlertTriangle v-else :size="18" />
          <span class="font-medium">{{ message.text }}</span>
        </div>
      </transition>
    </section>

    <!-- Right: Notes List -->
    <section class="lg:col-span-7 flex flex-col gap-6">
      <div class="flex items-center justify-between px-4">
        <h2 class="text-xl font-bold text-wood-500 flex items-center gap-3">
          <Clock :size="24" class="text-wood-accent" />
          时光记录
          <span class="text-sm font-normal text-wood-300 ml-2">已记录 {{ notes.length }} 条灵感</span>
        </h2>
      </div>

      <div class="flex flex-col gap-4 overflow-y-auto max-h-[calc(100vh-250px)] pr-2 custom-scrollbar">
        <div v-if="notes.length === 0" class="text-center py-20 bg-white/50 rounded-3xl border-2 border-dashed border-wood-200">
          <div class="text-6xl mb-6">📝</div>
          <p class="text-wood-300">还没有任何记录，开始你的思考之旅吧</p>
        </div>
        
        <transition-group name="list">
          <div 
            v-for="(note, index) in notes" 
            :key="note.id"
            class="bg-white p-6 rounded-3xl shadow-sm wood-card border-none hover:shadow-md transition-shadow relative group animate-in slide-in-from-right duration-300"
            :style="{ animationDelay: `${index * 50}ms` }"
          >
            <div class="flex items-start justify-between mb-3">
              <span class="text-xs font-bold text-wood-300 bg-wood-50 px-3 py-1 rounded-full uppercase tracking-wider">
                {{ formatDateTime(note.createdAt) }}
              </span>
              <button 
                @click="deleteNote(note.id)"
                class="opacity-0 group-hover:opacity-100 p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-full transition-all"
              >
                <Trash2 :size="18" />
              </button>
            </div>
            <p class="text-wood-500 leading-relaxed text-lg whitespace-pre-wrap">{{ note.content }}</p>
          </div>
        </transition-group>
      </div>
    </section>

    <!-- AI Result Modal -->
    <transition name="modal">
      <div v-if="showAiModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-wood-500/60 backdrop-blur-sm">
        <div class="bg-wood-100 w-full max-w-2xl rounded-[2.5rem] shadow-2xl overflow-hidden relative border border-wood-200">
          <div class="absolute top-6 right-6 z-10">
            <button @click="showAiModal = false" class="p-3 bg-white/50 hover:bg-white rounded-2xl transition-colors shadow-sm">
              <X :size="20" class="text-wood-500" />
            </button>
          </div>
          
          <div class="p-10">
            <div class="flex items-center gap-4 mb-8">
              <div class="w-16 h-16 rounded-2xl bg-wood-500 flex items-center justify-center shadow-lg">
                <Brain :size="32" class="text-wood-100" />
              </div>
              <div>
                <h3 class="text-2xl font-bold text-wood-500">思维洞察报告</h3>
                <p class="text-wood-300">由 AI 驱动的深度思维分析</p>
              </div>
            </div>
            
            <div class="bg-white rounded-3xl p-8 wood-card border-none shadow-inner max-h-[60vh] overflow-y-auto">
              <div class="prose prose-stone max-w-none text-wood-500 whitespace-pre-wrap leading-loose">
                {{ aiResult }}
              </div>
            </div>
            
            <div class="mt-10 flex justify-end">
              <button 
                @click="showAiModal = false"
                class="px-10 py-4 bg-wood-accent text-white font-bold rounded-2xl shadow-lg hover:bg-[#8D7352] transform hover:-translate-y-1 transition-all active:scale-95"
              >
                收起报告
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #EAE0D5;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #C6AC8F;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.list-enter-active, .list-leave-active {
  transition: all 0.5s ease;
}
.list-enter-from, .list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.modal-enter-active, .modal-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}
</style>
