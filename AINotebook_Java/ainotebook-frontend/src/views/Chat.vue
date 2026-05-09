<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Bot, Send, Plus, Trash2, MessageSquare, Loader2, Settings2, ArrowDown, Copy, RotateCcw, X, User } from 'lucide-vue-next'
import http from '../services/http'
import { getToken } from '../services/auth'

const configKey = 'ainotebook_chat_config_v1'

const user = ref({ nickname: '' })
const threads = ref([])
const activeId = ref('')

const messageInput = ref('')
const isSending = ref(false)
const errorText = ref('')

const modelName = ref('deepseek-r1:7b')
const baseUrl = ref('http://localhost:11434')
const showSettings = ref(false)
const toast = ref('')

const activeThread = computed(() => threads.value.find((t) => t.id === activeId.value) || null)

const personaPresets = [
  {
    id: 'xiaozhi',
    name: '小智',
    intro: '解决实际问题：结构化拆解、给步骤与可执行方案。',
    prompt:
      '你是小智，一个擅长解决实际问题的专业助手。你说话简洁、结构化、可执行：先给结论，再给步骤/清单，必要时给示例。你不会胡编乱造，遇到不确定会说明假设。'
  },
  {
    id: 'xiaomiao',
    name: '小淼',
    intro: '分析情感：共情倾听、澄清感受、提供温和建议。',
    prompt:
      '你是小淼，一个情感支持与沟通陪伴型助手。你会先共情与倾听，再用温和的问题帮助用户澄清感受与需求，并给出可尝试的小步骤。你避免武断诊断，优先保护用户感受与边界。'
  },
  {
    id: 'xiaonian',
    name: '小年',
    intro: '熟悉历史：讲背景脉络、对比观点、引用常识性史实。',
    prompt:
      '你是小年，一个擅长历史与人文知识的助手。你回答注重时间线与因果脉络，善于对比不同观点并给出背景解释。对于不确定的细节会说明可能性并建议核对来源。'
  }
]

const selectedPersonaId = ref('xiaomiao')
const customAssistantName = ref('')
const customSystemPrompt = ref('')

const currentAssistantName = computed(() => {
  const t = activeThread.value
  if (t?.assistantName) return t.assistantName
  const preset = personaPresets.find((p) => p.id === selectedPersonaId.value)
  return customAssistantName.value?.trim() || preset?.name || '小淼'
})

const currentSystemPrompt = computed(() => {
  const t = activeThread.value
  if (t?.systemPrompt) return t.systemPrompt
  const preset = personaPresets.find((p) => p.id === selectedPersonaId.value)
  return customSystemPrompt.value?.trim() || preset?.prompt || personaPresets[1].prompt
})

function nowIso() {
  return new Date().toISOString()
}

function loadConfig() {
  try {
    const raw = localStorage.getItem(configKey)
    if (!raw) return
    const c = JSON.parse(raw)
    if (c?.model) modelName.value = c.model
    if (c?.baseUrl) baseUrl.value = c.baseUrl
    if (c?.personaId) selectedPersonaId.value = c.personaId
    if (c?.assistantName) customAssistantName.value = c.assistantName
    if (c?.systemPrompt) customSystemPrompt.value = c.systemPrompt
  } catch {
  }
}

function persistConfig() {
  try {
    localStorage.setItem(
      configKey,
      JSON.stringify({
        model: modelName.value,
        baseUrl: baseUrl.value,
        personaId: selectedPersonaId.value,
        assistantName: customAssistantName.value,
        systemPrompt: customSystemPrompt.value
      })
    )
  } catch {
  }
}

async function fetchSessions() {
  const res = await http.get('/chat/sessions')
  const list = res.data || []
  const map = new Map(threads.value.map((t) => [t.id, t]))
  threads.value = list.map((s) => {
    const existing = map.get(s.id)
    return {
      ...s,
      messages: existing?.messages || []
    }
  })
  if (!activeId.value && threads.value.length) activeId.value = threads.value[0].id
}

async function fetchMessages(sessionId) {
  if (!sessionId) return
  const res = await http.get(`/chat/sessions/${sessionId}/messages`)
  const t = threads.value.find((x) => x.id === sessionId)
  if (t) t.messages = res.data || []
}

async function createSessionOnServer(body) {
  const res = await http.post('/chat/sessions', body)
  return res?.data?.id
}

async function deleteSessionOnServer(id) {
  await http.delete(`/chat/sessions/${id}`)
}

async function appendMessageOnServer(sessionId, role, content) {
  await http.post(`/chat/sessions/${sessionId}/messages`, { role, content })
}

async function updateSessionOnServer(sessionId, body) {
  await http.put(`/chat/sessions/${sessionId}`, body)
}

async function clearSessionOnServer(sessionId) {
  await http.post(`/chat/sessions/${sessionId}/clear`)
}

function showToast(text) {
  toast.value = text
  setTimeout(() => {
    if (toast.value === text) toast.value = ''
  }, 2200)
}

async function ensureThread() {
  if (threads.value.length > 0 && activeId.value) return
  if (threads.value.length > 0 && !activeId.value) {
    activeId.value = threads.value[0].id
    await fetchMessages(activeId.value)
    return
  }
  await createThread()
}

async function createThread() {
  const nickname = user.value.nickname || '朋友'
  const preset = personaPresets.find((p) => p.id === selectedPersonaId.value) || personaPresets[1]
  const assistantName = customAssistantName.value?.trim() || preset.name
  const systemPrompt = (customSystemPrompt.value?.trim() || preset.prompt).trim()

  const id = await createSessionOnServer({
    title: '新对话',
    assistantName,
    systemPrompt,
    model: modelName.value,
    baseUrl: baseUrl.value
  })

  await fetchSessions()
  activeId.value = id
  await fetchMessages(id)

  const greeting = `你好，${nickname}。我是${assistantName}，我们开始吧。你可以直接告诉我你想聊什么。`
  const t = threads.value.find((x) => x.id === id)
  if (t) {
    t.messages = [{ role: 'assistant', content: greeting, createdAt: nowIso() }]
  }
  await appendMessageOnServer(id, 'assistant', greeting)
  await fetchSessions()
  await nextTick(scrollToBottom)
}

async function deleteThread(id) {
  const ok = window.confirm('确认删除这段聊天记录吗？')
  if (!ok) return
  await deleteSessionOnServer(id)
  await fetchSessions()
  await ensureThread()
}

function setTitleFromFirstUserMessage(thread) {
  const firstUser = thread.messages.find((m) => m.role === 'user' && m.content?.trim())
  if (!firstUser) return
  const txt = firstUser.content.trim().replace(/\s+/g, ' ')
  thread.title = txt.length > 18 ? `${txt.slice(0, 18)}…` : txt
}

const chatScrollRef = ref(null)
function scrollToBottom() {
  const el = chatScrollRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

async function clearActiveThread() {
  if (!activeThread.value) return
  const ok = window.confirm('确认清空当前对话吗？')
  if (!ok) return
  const nickname = user.value.nickname || '朋友'
  const sessionId = activeThread.value.id
  await clearSessionOnServer(sessionId)
  const greeting = `你好，${nickname}。我是${currentAssistantName.value}，我们开始吧。你可以直接告诉我你想聊什么。`
  await appendMessageOnServer(sessionId, 'assistant', greeting)
  await fetchMessages(sessionId)
  await fetchSessions()
  await nextTick(scrollToBottom)
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    showToast('已复制')
  } catch {
    showToast('复制失败')
  }
}

async function fetchMe() {
  try {
    const res = await http.get('/auth/me')
    if (res.data?.nickname) user.value.nickname = res.data.nickname
  } catch {
  }
}

async function applyPersonaToThread(personaId) {
  if (!activeThread.value) return
  const preset = personaPresets.find((p) => p.id === personaId)
  if (!preset) return
  activeThread.value.assistantName = preset.name
  activeThread.value.systemPrompt = preset.prompt
  selectedPersonaId.value = preset.id
  customAssistantName.value = ''
  customSystemPrompt.value = ''
  persistConfig()
  await updateSessionOnServer(activeThread.value.id, {
    assistantName: preset.name,
    systemPrompt: preset.prompt,
    model: modelName.value,
    baseUrl: baseUrl.value
  })
  await fetchSessions()
}

async function applyCustomToThread() {
  if (!activeThread.value) return
  const name = customAssistantName.value.trim()
  const prompt = customSystemPrompt.value.trim()
  if (!name) {
    alert('请先填写 AI 名字')
    return
  }
  if (!prompt) {
    alert('请先填写 Prompt')
    return
  }
  activeThread.value.assistantName = name
  activeThread.value.systemPrompt = prompt
  persistConfig()
  await updateSessionOnServer(activeThread.value.id, {
    assistantName: name,
    systemPrompt: prompt,
    model: modelName.value,
    baseUrl: baseUrl.value
  })
  await fetchSessions()
}

async function streamAssistantText(thread, text) {
  if (!thread) return
  const placeholder = { role: 'assistant', content: '', createdAt: nowIso() }
  thread.messages.push(placeholder)
  thread.updatedAt = nowIso()
  await nextTick()
  scrollToBottom()

  const payload = {
    model: modelName.value,
    baseUrl: baseUrl.value,
    userName: user.value.nickname || '',
    assistantName: thread.assistantName || currentAssistantName.value,
    systemPrompt: thread.systemPrompt || currentSystemPrompt.value,
    messages: [{ role: 'user', content: text }]
  }

  let gotAny = false
  try {
    const res = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`
      },
      body: JSON.stringify(payload)
    })
    if (!res.ok || !res.body) throw new Error('stream not available')
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        const lines = part.split('\n')
        const eventLine = lines.find((l) => l.startsWith('event:'))
        const dataLine = lines.find((l) => l.startsWith('data:'))
        const event = eventLine ? eventLine.replace('event:', '').trim() : ''
        const data = dataLine ? dataLine.replace('data:', '').trim() : ''
        if (event === 'delta') {
          gotAny = true
          placeholder.content += data
        } else if (event === 'error') {
          throw new Error(data || 'stream error')
        }
      }
      thread.updatedAt = nowIso()
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    try {
      const res = await http.post('/ai/chat', payload)
      const reply = res?.data?.content || ''
      placeholder.content = reply || placeholder.content
      gotAny = true
    } catch {
    }
  }

  if (!gotAny) placeholder.content = placeholder.content || '（无回复）'
  thread.updatedAt = nowIso()
  await appendMessageOnServer(thread.id, 'assistant', placeholder.content)
  await fetchSessions()
  await nextTick()
  scrollToBottom()
}

async function greetPersona() {
  if (!activeThread.value) return
  const thread = activeThread.value
  const name = thread.assistantName || currentAssistantName.value
  const nickname = user.value.nickname || '朋友'
  await streamAssistantText(thread, `请用你的角色设定向我做一个简短开场白。称呼我为「${nickname}」。第一句必须包含“我是${name}”。并告诉我你最擅长帮我做什么。`)
}

async function send() {
  const content = messageInput.value.trim()
  if (!content) return
  if (!activeThread.value || isSending.value) return

  errorText.value = ''
  isSending.value = true

  const thread = activeThread.value
  const createdAt = nowIso()
  thread.messages.push({ role: 'user', content, createdAt })
  thread.updatedAt = nowIso()
  messageInput.value = ''
  setTitleFromFirstUserMessage(thread)
  await nextTick()
  scrollToBottom()

  try {
    await appendMessageOnServer(thread.id, 'user', content)
    const messages = thread.messages
      .filter((m) => m.role === 'user' || m.role === 'assistant')
      .slice(-10)
      .map((m) => ({ role: m.role, content: m.content }))

    const payload = {
      model: modelName.value,
      baseUrl: baseUrl.value,
      userName: user.value.nickname || '',
      assistantName: thread.assistantName || currentAssistantName.value,
      systemPrompt: thread.systemPrompt || currentSystemPrompt.value,
      messages,
    }

    const placeholder = { role: 'assistant', content: '', createdAt: nowIso() }
    thread.messages.push(placeholder)
    thread.updatedAt = nowIso()
    await nextTick()
    scrollToBottom()

    let gotAny = false
    try {
      const res = await fetch('/api/ai/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getToken()}`
        },
        body: JSON.stringify(payload)
      })
      if (!res.ok || !res.body) {
        throw new Error('stream not available')
      }

      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || ''
        for (const part of parts) {
          const lines = part.split('\n')
          const eventLine = lines.find((l) => l.startsWith('event:'))
          const dataLine = lines.find((l) => l.startsWith('data:'))
          const event = eventLine ? eventLine.replace('event:', '').trim() : ''
          const data = dataLine ? dataLine.replace('data:', '').trim() : ''
          if (event === 'delta') {
            gotAny = true
            placeholder.content += data
          } else if (event === 'error') {
            throw new Error(data || 'stream error')
          }
        }
        thread.updatedAt = nowIso()
        await nextTick()
        scrollToBottom()
      }
    } catch (streamErr) {
      try {
        const res = await http.post('/ai/chat', payload)
        const reply = res?.data?.content || ''
        placeholder.content = reply || '（无回复）'
        gotAny = true
      } catch (e2) {
        throw e2
      }
    }

    if (!gotAny) placeholder.content = '（无回复）'
    thread.updatedAt = nowIso()
    await appendMessageOnServer(thread.id, 'assistant', placeholder.content)
    await fetchSessions()
    await nextTick()
    scrollToBottom()
  } catch (e) {
    errorText.value = e?.response?.data?.message || '发送失败，请确认本地 Ollama 正在运行'
  } finally {
    isSending.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

watch(
  () => activeId.value,
  async () => {
    if (activeId.value) {
      await fetchMessages(activeId.value)
      await fetchSessions()
    }
    await nextTick()
    scrollToBottom()
  }
)

onMounted(async () => {
  loadConfig()
  await fetchMe()
  await fetchSessions()
  await ensureThread()
})
</script>

<template>
  <div class="max-w-6xl mx-auto py-8 px-4">
    <div class="flex items-end justify-between gap-4 mb-8">
      <div>
        <div class="text-3xl md:text-4xl font-bold text-wood-500 tracking-tight">
          让我们开始吧，{{ user.nickname || '朋友' }}。
        </div>
        <div class="mt-2 text-wood-300 text-sm">本地模型对话（{{ currentAssistantName }} · {{ modelName }}）</div>
      </div>
      <div class="flex items-center gap-3">
        <button
          class="px-4 py-2 rounded-xl border border-wood-200 bg-white/60 text-wood-500 hover:bg-white transition-colors inline-flex items-center gap-2"
          @click="showSettings = true"
          type="button"
        >
          <Settings2 :size="18" />
          设置
        </button>
        <button
          class="px-5 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md active:scale-95 inline-flex items-center gap-2"
          @click="createThread"
          type="button"
        >
          <Plus :size="18" />
          新建对话
        </button>
      </div>
    </div>

    <div v-if="showSettings" class="chat-modal-mask">
      <div class="chat-modal" role="dialog" aria-label="聊天设置">
        <div class="chat-modal-head">
          <div class="chat-modal-title">
            <User :size="18" class="text-wood-accent" />
            聊天设置
          </div>
          <button class="chat-modal-close" type="button" @click="showSettings = false" aria-label="关闭">
            <X :size="18" />
          </button>
        </div>

        <div class="chat-modal-body">
          <div class="section-title">模型</div>
          <div class="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
            <div class="md:col-span-6">
              <div class="text-xs text-wood-300 mb-1">Ollama API</div>
              <input v-model="baseUrl" type="text" class="w-full px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent text-wood-500" placeholder="http://localhost:11434" />
            </div>
            <div class="md:col-span-6">
              <div class="text-xs text-wood-300 mb-1">模型名</div>
              <input v-model="modelName" type="text" class="w-full px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent text-wood-500" placeholder="deepseek-r1:7b" />
            </div>
          </div>
          <div class="mt-2 text-xs text-wood-300">仅支持本机地址（localhost / 127.0.0.1）。</div>

          <div class="section-title mt-6">人设</div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <button
              v-for="p in personaPresets"
              :key="p.id"
              type="button"
              class="persona-card"
              :class="activeThread?.personaId === p.id ? 'active' : ''"
              :data-tip="p.intro"
              @click="applyPersonaToThread(p.id); showSettings = false; greetPersona()"
            >
              <div class="persona-name">{{ p.name }}</div>
              <div class="persona-desc">{{ p.intro }}</div>
            </button>
          </div>

          <div class="section-title mt-6">自定义</div>
          <div class="grid grid-cols-1 md:grid-cols-12 gap-4 items-start">
            <div class="md:col-span-4">
              <div class="text-xs text-wood-300 mb-1">AI 名字</div>
              <input v-model="customAssistantName" type="text" class="w-full px-4 py-3 rounded-xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent text-wood-500" placeholder="例如：小岚" />
            </div>
            <div class="md:col-span-8">
              <div class="text-xs text-wood-300 mb-1">Prompt（人设）</div>
              <textarea v-model="customSystemPrompt" class="w-full min-h-[120px] px-4 py-3 rounded-2xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent resize-none text-wood-500" placeholder="写下你的角色设定、口吻、输出风格..."></textarea>
            </div>
          </div>

          <div class="mt-4 flex items-center justify-end gap-3">
            <button class="px-5 py-2 rounded-xl border border-wood-200 text-wood-500 hover:bg-wood-50" type="button" @click="loadConfig(); showToast('已还原')">还原</button>
            <button class="px-5 py-2 rounded-xl border border-wood-200 text-wood-500 hover:bg-wood-50" type="button" @click="persistConfig(); showToast('已保存')">保存</button>
            <button
              class="px-6 py-2 rounded-xl bg-wood-accent text-white font-bold hover:bg-[#8D7352] transition-all shadow-md"
              type="button"
              @click="applyCustomToThread(); showSettings = false; greetPersona()"
            >
              应用并开场
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 chat-layout">
      <aside class="lg:col-span-3 bg-white/70 rounded-[2rem] border border-wood-100 overflow-hidden chat-pane">
        <div class="px-6 py-4 border-b border-wood-100 flex items-center justify-between">
          <div class="flex items-center gap-2 text-wood-500 font-bold">
            <MessageSquare :size="18" class="text-wood-accent" />
            历史记录
          </div>
        </div>

        <div class="p-3 chat-pane-scroll">
          <button
            v-for="t in threads"
            :key="t.id"
            class="w-full text-left px-4 py-3 rounded-2xl transition-colors border mb-2"
            :class="t.id === activeId ? 'bg-wood-50 border-wood-100' : 'bg-white border-transparent hover:bg-wood-50/60'"
            @click="activeId = t.id"
            type="button"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="font-bold text-wood-500 truncate">{{ t.title }}</div>
                <div class="text-xs text-wood-300 mt-1 truncate">{{ new Date(t.updatedAt || t.createdAt).toLocaleString() }}</div>
              </div>
              <button
                class="shrink-0 p-2 rounded-xl hover:bg-white text-wood-300 hover:text-red-500 transition-colors"
                @click.stop="deleteThread(t.id)"
                type="button"
                aria-label="删除对话"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </button>
        </div>
      </aside>

      <section class="lg:col-span-9 bg-white rounded-[2rem] border border-wood-100 overflow-hidden shadow-sm chat-pane">
        <div class="px-6 py-4 border-b border-wood-100 flex items-center justify-between gap-4">
          <div class="flex items-center gap-2 text-wood-500 font-bold">
            <Bot :size="18" class="text-wood-accent" />
            实时聊天 · {{ currentAssistantName }}
          </div>
          <div class="flex items-center gap-2">
            <button
              class="px-3 py-2 rounded-xl border border-wood-100 bg-wood-50/40 text-wood-500 hover:bg-wood-50 transition-colors inline-flex items-center gap-2"
              type="button"
              @click="clearActiveThread"
            >
              <RotateCcw :size="16" />
              清空
            </button>
            <button
              class="px-3 py-2 rounded-xl border border-wood-100 bg-wood-50/40 text-wood-500 hover:bg-wood-50 transition-colors inline-flex items-center gap-2"
              type="button"
              @click="scrollToBottom"
            >
              <ArrowDown :size="16" />
              底部
            </button>
          </div>
        </div>

        <div ref="chatScrollRef" class="p-6 space-y-4 chat-pane-scroll">
          <div v-if="!activeThread" class="text-wood-300 text-sm">请选择或新建一个对话。</div>

          <div
            v-else
            v-for="(m, idx) in activeThread.messages"
            :key="`${idx}_${m.createdAt}`"
            class="flex"
            :class="m.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[85%] rounded-2xl px-4 py-3 border whitespace-pre-wrap leading-relaxed"
              :class="m.role === 'user' ? 'bg-wood-accent text-white border-transparent' : 'bg-wood-50/60 text-wood-500 border-wood-100'"
            >
              <div class="text-sm">{{ m.content }}</div>
              <div class="mt-2 flex items-center justify-between gap-3 text-[11px] opacity-70">
                <div>{{ new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }}</div>
                <button
                  v-if="m.role !== 'user'"
                  class="inline-flex items-center gap-1 hover:opacity-100"
                  type="button"
                  @click="copyText(m.content)"
                >
                  <Copy :size="14" />
                  复制
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 border-t border-wood-100">
          <div v-if="errorText" class="mb-3 px-4 py-3 rounded-2xl bg-red-50 border border-red-100 text-red-600 text-sm">
            {{ errorText }}
          </div>

          <div class="mb-3 flex flex-wrap gap-2">
            <button class="px-3 py-1.5 rounded-full bg-wood-50 border border-wood-100 text-wood-500 text-sm hover:bg-white transition-colors" type="button" @click="messageInput = '我最近情绪很低落，能帮我梳理一下吗？'">情绪低落</button>
            <button class="px-3 py-1.5 rounded-full bg-wood-50 border border-wood-100 text-wood-500 text-sm hover:bg-white transition-colors" type="button" @click="messageInput = '我有些焦虑，脑子停不下来，怎么办？'">焦虑失控</button>
            <button class="px-3 py-1.5 rounded-full bg-wood-50 border border-wood-100 text-wood-500 text-sm hover:bg-white transition-colors" type="button" @click="messageInput = '我和朋友/伴侣的关系有点僵，如何沟通？'">关系沟通</button>
            <button class="px-3 py-1.5 rounded-full bg-wood-50 border border-wood-100 text-wood-500 text-sm hover:bg-white transition-colors" type="button" @click="messageInput = '我总是自责，觉得自己不够好，怎么调整？'">自我否定</button>
          </div>

          <div class="flex items-end gap-3">
            <textarea
              v-model="messageInput"
              class="flex-1 min-h-[52px] max-h-[160px] px-4 py-3 rounded-2xl border border-wood-100 bg-wood-50/40 focus:outline-none focus:border-wood-accent resize-none text-wood-500"
              placeholder="有什么我能帮你的吗？（Enter 发送，Shift+Enter 换行）"
              @keydown="onKeydown"
            ></textarea>
            <button
              class="px-5 py-3 rounded-2xl bg-wood-500 text-white font-bold hover:bg-[#344955] transition-all shadow-md disabled:opacity-60 inline-flex items-center gap-2"
              :disabled="isSending || !messageInput.trim()"
              @click="send"
              type="button"
            >
              <Loader2 v-if="isSending" :size="18" class="animate-spin" />
              <Send v-else :size="18" />
              发送
            </button>
          </div>
        </div>
      </section>
    </div>

    <div v-if="toast" class="fixed bottom-6 right-6 z-50 px-4 py-3 rounded-2xl bg-wood-500 text-white shadow-lg">
      {{ toast }}
    </div>
  </div>
</template>

<style scoped>
.chat-layout {
  align-items: stretch;
}

.chat-pane {
  height: 72vh;
  display: flex;
  flex-direction: column;
}

.chat-pane-scroll {
  flex: 1;
  overflow: auto;
}

.chat-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(15, 25, 34, 0.45);
  backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  padding: 16px;
}

.chat-modal {
  width: min(900px, 100%);
  border-radius: 24px;
  border: 1px solid rgba(193, 152, 102, 0.22);
  background: rgba(255, 255, 255, 0.92);
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
}

.chat-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.chat-modal-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  color: #5b4633;
}

.chat-modal-close {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  place-items: center;
  color: rgba(91, 70, 51, 0.75);
}

.chat-modal-close:hover {
  background: #fff;
}

.chat-modal-body {
  padding: 16px;
}

.section-title {
  font-size: 12px;
  font-weight: 800;
  color: rgba(91, 70, 51, 0.75);
  letter-spacing: 0.3px;
  margin-bottom: 10px;
}

.persona-card {
  position: relative;
  text-align: left;
  padding: 14px 14px;
  border-radius: 18px;
  border: 1px solid rgba(193, 152, 102, 0.18);
  background: rgba(255, 255, 255, 0.75);
  transition: transform 120ms ease, background 200ms ease, border-color 200ms ease;
}

.persona-card:hover {
  transform: translateY(-1px);
  border-color: rgba(193, 152, 102, 0.35);
  background: rgba(255, 255, 255, 0.95);
}

.persona-card.active {
  border-color: rgba(193, 152, 102, 0.55);
  box-shadow: 0 10px 30px rgba(193, 152, 102, 0.12);
}

.persona-name {
  font-weight: 900;
  color: #5b4633;
}

.persona-desc {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(91, 70, 51, 0.65);
  line-height: 1.35;
}

.persona-card::after {
  content: attr(data-tip);
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: calc(100% + 10px);
  transform: translateY(6px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 140ms ease, transform 140ms ease;
  background: rgba(15, 25, 34, 0.92);
  color: rgba(248, 243, 237, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.35;
}

.persona-card:hover::after {
  opacity: 1;
  transform: translateY(0);
}
</style>
