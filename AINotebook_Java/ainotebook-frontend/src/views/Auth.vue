<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../services/http'
import { setToken } from '../services/auth'

const router = useRouter()
const route = useRoute()

const mode = ref('login')
const form = ref({ username: '', password: '', confirm: '' })
const msg = ref({ text: '', type: '' })

const pull = ref(0)
const active = ref(false)
const loading = ref(false)
const drag = ref({ on: false, startY: 0, base: 0, toggled: false, lastPull: 0, moved: false })
const handleRef = ref(null)

const spring = ref(0)
let springRaf = 0
let springV = 0

const canvasRef = ref(null)
const size = ref({ w: 0, h: 0, dpr: 1 })
let raf = 0
let t0 = 0
const reducedMotion = ref(false)

const canSubmit = computed(() => {
  if (!form.value.username.trim() || !form.value.password) return false
  if (mode.value === 'register') return form.value.password === form.value.confirm && form.value.password.length >= 6
  return true
})

const passwordMismatch = computed(() => mode.value === 'register' && form.value.confirm && form.value.password !== form.value.confirm)

const idBase = `auth-${Math.random().toString(36).slice(2, 9)}`
const usernameId = `${idBase}-username`
const passwordId = `${idBase}-password`
const confirmId = `${idBase}-confirm`
const confirmHelpId = `${idBase}-confirm-help`
const toastId = `${idBase}-toast`

const CHAIN_BASE = 38
const CHAIN_INSIDE_MAX = 108

const insideLen = computed(() => Math.min(CHAIN_INSIDE_MAX, CHAIN_BASE + pull.value))
const outsideLen = computed(() => Math.max(0, CHAIN_BASE + pull.value - CHAIN_INSIDE_MAX))

function showMsg(text, type = 'info') {
  msg.value = { text, type }
  window.clearTimeout(showMsg._t)
  showMsg._t = window.setTimeout(() => (msg.value = { text: '', type: '' }), 2600)
}

function clamp(n, a, b) {
  return Math.max(a, Math.min(b, n))
}

function setActive(v) {
  if (active.value === v) return
  active.value = v
  if (v) {
    loading.value = true
    window.setTimeout(() => (loading.value = false), 650)
  } else {
    loading.value = false
  }
}

function toggleLamp() {
  setActive(!active.value)
  springV = 0
  runSpring()
}

function runSpring() {
  if (drag.value.on) return
  const target = active.value ? 120 : 0
  const k = 0.15
  const d = 0.75
  
  const f = (target - spring.value) * k
  springV = (springV + f) * d
  spring.value += springV
  
  pull.value = spring.value
  
  if (Math.abs(springV) > 0.1 || Math.abs(target - spring.value) > 0.1) {
    springRaf = requestAnimationFrame(runSpring)
  } else {
    spring.value = target
    pull.value = target
  }
}

function onPointerDown(e) {
  drag.value.on = true
  drag.value.startY = e.clientY
  drag.value.base = pull.value
  drag.value.toggled = false
  drag.value.lastPull = pull.value
  drag.value.moved = false
  cancelAnimationFrame(springRaf)
  try { e.target.setPointerCapture(e.pointerId) } catch {}
}

function onPointerMove(e) {
  if (!drag.value.on) return
  const dy = e.clientY - drag.value.startY
  if (Math.abs(dy) > 4) drag.value.moved = true
  const tension = Math.max(0.3, 1 - (pull.value / 250))
  const nextPull = clamp(drag.value.base + dy * tension, 0, 180)
  const goingDown = nextPull > drag.value.lastPull
  drag.value.lastPull = nextPull
  pull.value = nextPull
  spring.value = pull.value
  
  if (goingDown && pull.value > 112 && !drag.value.toggled) {
    setActive(!active.value)
    drag.value.toggled = true
    if (navigator.vibrate) navigator.vibrate(15)
  }
}

function onPointerUp() {
  drag.value.on = false
  springV = 0
  runSpring()
}

function onHandleClick() {
  if (drag.value.moved) return
  toggleLamp()
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    if (mode.value === 'register') {
      await http.post('/auth/register', { username: form.value.username.trim(), password: form.value.password })
      mode.value = 'login'
      form.value.confirm = ''
      showMsg('注册成功，请登录', 'success')
      loading.value = false
      return
    }

    const res = await http.post('/auth/login', { username: form.value.username.trim(), password: form.value.password })
    const token = res?.data?.token || res?.data
    if (!token) throw new Error('登录失败')
    setToken(token)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    showMsg(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    loading.value = false
  }
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  form.value.password = ''
  form.value.confirm = ''
}

function resize() {
  const el = canvasRef.value
  if (!el) return
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  const rect = el.getBoundingClientRect()
  size.value = { w: rect.width, h: rect.height, dpr }
  el.width = Math.floor(rect.width * dpr)
  el.height = Math.floor(rect.height * dpr)
}

function draw(ts) {
  const el = canvasRef.value
  if (!el) return
  const ctx = el.getContext('2d')
  if (!ctx) return
  if (!t0) t0 = ts
  const t = (ts - t0) / 1000
  const { w, h, dpr } = size.value
  if (!w || !h) return
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, w, h)

  const g = ctx.createRadialGradient(w * 0.55, h * 0.35, 10, w * 0.55, h * 0.35, Math.max(w, h))
  g.addColorStop(0, '#0b1b2a')
  g.addColorStop(0.5, '#07121d')
  g.addColorStop(1, '#050b12')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, w, h)

  const swirl = (x, y, k) => {
    const cx = w * 0.62
    const cy = h * 0.38
    const dx = x - cx
    const dy = y - cy
    const r = Math.sqrt(dx * dx + dy * dy) + 1e-6
    const a = Math.atan2(dy, dx) + k * (1 / r) * 220
    return [cx + Math.cos(a) * r, cy + Math.sin(a) * r]
  }

  const n = Math.floor((w * h) / 12000)
  for (let i = 0; i < n; i++) {
    const seed = (i * 99991) % 104729
    const rx = (seed % 997) / 997
    const ry = ((seed * 7) % 991) / 991
    const px = rx * w
    const py = ry * h
    const [sx, sy] = swirl(px, py, Math.sin(t * 0.55) * 0.8)
    const tw = (Math.sin(t * 0.8 + i) + 1) * 0.5
    const r = 0.6 + tw * 1.4
    const a = 0.14 + tw * 0.26
    const hue = 205 + (i % 7) * 5
    ctx.fillStyle = `hsla(${hue}, 85%, ${70 + tw * 10}%, ${a})`
    ctx.beginPath()
    ctx.arc(sx, sy, r, 0, Math.PI * 2)
    ctx.fill()
  }

  const glowX = w * 0.28
  const glowY = h * 0.38
  const power = active.value ? 1 : 0
  const cone = ctx.createRadialGradient(glowX, glowY, 20, glowX, glowY, Math.max(w, h) * 0.7)
  cone.addColorStop(0, `rgba(166,138,100,${0.22 * power})`)
  cone.addColorStop(0.35, `rgba(166,138,100,${0.10 * power})`)
  cone.addColorStop(1, 'rgba(166,138,100,0)')
  ctx.fillStyle = cone
  ctx.fillRect(0, 0, w, h)

  if (!reducedMotion.value) raf = requestAnimationFrame(draw)
}

function onKeydown(e) {
  if (e.key === 'Escape' && active.value) toggleLamp()
}

onMounted(() => {
  reducedMotion.value = !!window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('keydown', onKeydown)
  raf = requestAnimationFrame(draw)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  window.removeEventListener('keydown', onKeydown)
  cancelAnimationFrame(raf)
})

watch(active, async (v) => {
  await nextTick()
  if (v) {
    const el = document.getElementById(usernameId)
    if (el && typeof el.focus === 'function') el.focus()
  } else {
    const h = handleRef.value
    if (h && typeof h.focus === 'function') h.focus()
  }
})
</script>

<template>
  <div class="auth-root">
    <canvas ref="canvasRef" class="auth-bg" aria-hidden="true"></canvas>

    <div class="auth-stage">
      <div class="lamp-wrap" :class="{ on: active }">
        <div class="lamp-floor-glow"></div>

        <div class="lamp">
          <div class="lamp-shade">
            <div class="shade-pull">
              <div class="pull-hole"></div>
              <div class="pull-inside" :style="{ height: `${insideLen}px` }">
                <div class="pull-chain-dots"></div>
              </div>
            </div>
            <div class="lamp-shade-inner">
              <div class="lamp-face">
                <span class="eye"></span>
                <span class="eye"></span>
                <span class="mouth"></span>
              </div>
            </div>
          </div>
          <div class="pull-outside">
            <div class="pull-outside-line" :style="{ height: `${outsideLen}px` }">
              <div class="pull-chain-dots"></div>
            </div>
            <button
              class="pull-handle"
              ref="handleRef"
              type="button"
              aria-label="切换台灯"
              @pointerdown="onPointerDown"
              @pointermove="onPointerMove"
              @pointerup="onPointerUp"
              @pointercancel="onPointerUp"
              @click="onHandleClick"
              @keydown.enter.prevent="toggleLamp"
              @keydown.space.prevent="toggleLamp"
            ></button>
          </div>
          <div class="lamp-pole"></div>
          <div class="lamp-base">
            <div class="lamp-base-top"></div>
          </div>
        </div>
      </div>

      <transition name="panel">
        <div v-if="active" class="panel" role="dialog" aria-label="登录或注册">
          <div class="panel-glow"></div>
          <div class="panel-inner">
            <button class="panel-close" type="button" aria-label="关闭登录面板" @click="toggleLamp"></button>
            <div class="panel-title">
              <div class="badge">AINotebook</div>
              <h2 class="title">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
              <p class="subtitle">{{ mode === 'login' ? '点亮小灯，开始记录' : '先注册，再把灵感存进原木空间' }}</p>
            </div>

            <form class="form" @submit.prevent="submit" :aria-describedby="msg.text ? toastId : undefined">
              <label class="field">
                <span class="label">账号</span>
                <input
                  :id="usernameId"
                  v-model="form.username"
                  class="input"
                  name="username"
                  autocomplete="username"
                  placeholder="请输入账号"
                  required
                />
              </label>

              <label class="field">
                <span class="label">密码</span>
                <input
                  :id="passwordId"
                  v-model="form.password"
                  class="input"
                  name="password"
                  type="password"
                  :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
                  placeholder="请输入密码"
                  minlength="6"
                  required
                />
              </label>

              <label v-if="mode === 'register'" class="field">
                <span class="label">确认密码</span>
                <input
                  :id="confirmId"
                  v-model="form.confirm"
                  class="input"
                  name="confirmPassword"
                  type="password"
                  autocomplete="new-password"
                  placeholder="再次输入密码"
                  minlength="6"
                  required
                  :aria-invalid="passwordMismatch ? 'true' : 'false'"
                  :aria-describedby="confirmHelpId"
                />
                <span :id="confirmHelpId" class="help" :class="{ on: passwordMismatch }">
                  {{ passwordMismatch ? '两次输入的密码不一致' : '' }}
                </span>
              </label>

              <button class="primary" type="submit" :disabled="!canSubmit || loading">
                <span v-if="!loading">{{ mode === 'login' ? '登录' : '注册' }}</span>
                <span v-else class="dots"><i></i><i></i><i></i></span>
              </button>

              <button class="ghost" type="button" @click="toggleMode">
                {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
              </button>
            </form>

            <transition name="toast">
              <div v-if="msg.text" :id="toastId" class="toast" :class="msg.type" role="status" aria-live="polite">{{ msg.text }}</div>
            </transition>
          </div>
        </div>
      </transition>

      <transition name="hint">
        <div v-if="!active" class="hint">
          <div class="hint-pill">拉下小绳点亮台灯</div>
          <div class="hint-sub">打开登录面板，松开可关闭</div>
          <button class="sr-only" type="button" @click="toggleLamp">切换台灯</button>
        </div>
      </transition>
    </div>

    <transition name="mask">
      <div v-if="active && loading" class="loading-mask">
        <div class="loading-card">
          <div class="loading-ring"></div>
          <div class="loading-text">点亮中...</div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.auth-root {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #f3efe9;
}

.auth-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.auth-stage {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  justify-items: center;
  gap: 56px;
  padding: clamp(18px, 4vw, 56px);
  max-width: 1400px;
  margin: 0 auto;
}

@media (max-width: 860px) {
  .auth-stage {
    grid-template-columns: 1fr;
    gap: 60px;
    justify-items: center;
    padding-top: 60px;
  }
}

.lamp-wrap {
  position: relative;
  width: 320px;
  height: 420px;
  display: grid;
  align-content: center;
  justify-items: center;
}

.lamp-floor-glow {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 400px;
  height: 100px;
  background: radial-gradient(ellipse at center, rgba(166,138,100,0.15) 0%, rgba(0,0,0,0) 70%);
  opacity: 0;
  transition: opacity 600ms ease;
  pointer-events: none;
}
.lamp-wrap.on .lamp-floor-glow {
  opacity: 1;
}

.lamp {
  position: relative;
  width: 220px;
  height: 340px;
  filter: drop-shadow(0 30px 40px rgba(0,0,0,0.4));
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
}

.lamp-shade {
  width: 190px;
  height: 150px;
  clip-path: polygon(20% 0%, 80% 0%, 100% 100%, 0% 100%);
  background: linear-gradient(135deg, #d4c5b3 0%, #b8a38a 100%);
  position: relative;
  overflow: hidden;
  z-index: 3;
  transition: all 400ms ease;
  box-shadow: inset 0 -10px 20px rgba(0,0,0,0.2);
}

.lamp-shade::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(45deg, rgba(0,0,0,0.03) 0px, rgba(0,0,0,0.03) 2px, transparent 2px, transparent 4px),
                    repeating-linear-gradient(-45deg, rgba(0,0,0,0.03) 0px, rgba(0,0,0,0.03) 2px, transparent 2px, transparent 4px);
  mix-blend-mode: multiply;
  pointer-events: none;
  z-index: 1;
}

.lamp-shade::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.10), rgba(255,255,255,0.02));
  pointer-events: none;
  z-index: 2;
}

.lamp-wrap.on .lamp-shade {
  background: linear-gradient(135deg, #ffeebb 0%, #e6ca98 100%);
  filter: drop-shadow(0 0 40px rgba(255, 218, 145, 0.6));
}

.lamp-shade-inner {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 12px;
  background: rgba(0,0,0,0.1);
  z-index: 3;
}

.lamp-face {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  gap: 8px;
  opacity: 0.8;
  z-index: 4;
}

.eye {
  width: 12px;
  height: 12px;
  background: #3a2e24;
  border-radius: 999px;
  display: inline-block;
  margin: 0 16px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.3);
}

.mouth {
  width: 24px;
  height: 12px;
  border-bottom: 4px solid #3a2e24;
  border-radius: 0 0 20px 20px;
  transform: translateY(-4px);
}

.lamp-pole {
  width: 22px;
  height: 170px;
  background: linear-gradient(90deg, #6b4c3a 0%, #8b6b52 50%, #5c3e2b 100%);
  position: relative;
  z-index: 1;
  box-shadow: inset 2px 0 6px rgba(0,0,0,0.3), inset -2px 0 6px rgba(255,255,255,0.1);
}

.lamp-pole::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(0deg, transparent 0px, transparent 10px, rgba(0,0,0,0.05) 10px, rgba(0,0,0,0.05) 12px);
}

.lamp-base {
  width: 140px;
  height: 20px;
  background: #7a5843;
  border-radius: 10px 10px 4px 4px;
  position: relative;
  z-index: 2;
  box-shadow: 0 10px 20px rgba(0,0,0,0.5), inset 0 2px 4px rgba(255,255,255,0.2);
}

.lamp-base-top {
  position: absolute;
  top: -8px;
  left: 10px;
  right: 10px;
  height: 16px;
  background: #8b6b52;
  border-radius: 50%;
  box-shadow: inset 0 2px 4px rgba(255,255,255,0.1);
}

.shade-pull {
  position: absolute;
  right: 34px;
  top: 14px;
  width: 28px;
  height: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
  z-index: 0;
  opacity: 0.72;
}

.pull-hole {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: rgba(0,0,0,0.20);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.35);
}

.pull-inside {
  width: 8px;
  margin-top: 6px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255,255,255,0.12), rgba(255,255,255,0.03));
  position: relative;
  overflow: hidden;
}

.pull-outside {
  position: absolute;
  right: 30px;
  top: 150px;
  width: 72px;
  height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: auto;
  z-index: 8;
}

.pull-outside-line {
  width: 8px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255,255,255,0.16), rgba(255,255,255,0.04));
  position: relative;
  overflow: hidden;
  pointer-events: none;
}

.pull-chain-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at center, rgba(240, 226, 204, 0.95) 38%, rgba(0,0,0,0) 45%);
  background-size: 8px 10px;
  background-repeat: repeat-y;
  filter: drop-shadow(0 1px 1px rgba(0,0,0,0.55));
}

.pull-handle {
  width: 18px;
  height: 48px;
  border-radius: 7px;
  background: linear-gradient(90deg, #4a3f35 0%, #7a6b5d 50%, #3a2e24 100%);
  box-shadow: 0 4px 8px rgba(0,0,0,0.4), inset 0 1px 2px rgba(255,255,255,0.3);
  display: grid;
  place-items: center;
  pointer-events: auto;
  touch-action: none;
  user-select: none;
  cursor: grab;
  transition: filter 160ms ease;
  margin-top: -4px;
}

.pull-handle:focus-visible {
  outline: 2px solid rgba(255, 225, 170, 0.95);
  outline-offset: 4px;
}

.pull-handle:active {
  cursor: grabbing;
}

.pull-handle:hover {
  filter: brightness(1.2);
}

.panel {
  position: relative;
  width: 100%;
  max-width: 420px;
  border-radius: 24px;
  background: rgba(20, 25, 33, 0.7);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: 0 40px 80px rgba(0,0,0,0.6), inset 0 1px 1px rgba(255,255,255,0.1);
  overflow: hidden;
}

.panel-glow {
  position: absolute;
  inset: -1px;
  border-radius: 24px;
  background: transparent;
  box-shadow: 0 0 15px 2px rgba(166,138,100,0.4);
  pointer-events: none;
  z-index: 0;
}

.panel-inner {
  position: relative;
  padding: 40px 36px 36px;
  backdrop-filter: blur(20px);
  z-index: 1;
}

.panel-close {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(10, 16, 24, 0.35);
  cursor: pointer;
  transition: background 160ms ease, transform 160ms ease, border-color 160ms ease;
}

.panel-close:hover {
  background: rgba(10, 16, 24, 0.55);
  border-color: rgba(255,255,255,0.16);
  transform: translateY(-1px);
}

.panel-close:focus-visible {
  outline: 2px solid rgba(255, 225, 170, 0.95);
  outline-offset: 4px;
}

.panel-close::before,
.panel-close::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 16px;
  height: 2px;
  background: rgba(255,255,255,0.82);
  border-radius: 999px;
  transform-origin: center;
}

.panel-close::before {
  transform: translate(-50%, -50%) rotate(45deg);
}

.panel-close::after {
  transform: translate(-50%, -50%) rotate(-45deg);
}

.panel-title .badge {
  display: inline-flex;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(255,255,255,0.1);
  font-size: 10px;
  letter-spacing: 0.5px;
  color: rgba(255,255,255,0.8);
  text-transform: uppercase;
}

.panel-title .title {
  margin-top: 16px;
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 1px;
}

.panel-title .subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

.form {
  margin-top: 24px;
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.input {
  height: 48px;
  padding: 0 16px;
  border-radius: 8px;
  background: rgba(15, 25, 34, 0.8);
  border: 1px solid rgba(255,255,255,0.05);
  color: #ffffff;
  font-size: 14px;
  outline: none;
  transition: all 200ms ease;
}

.input::placeholder {
  color: rgba(255,255,255,0.3);
}

.input:focus {
  border-color: rgba(182, 138, 86, 0.6);
  box-shadow: 0 0 0 1px rgba(182, 138, 86, 0.6);
  background: rgba(15, 25, 34, 0.95);
}

.help {
  min-height: 14px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.0);
  transition: color 160ms ease;
}

.help.on {
  color: rgba(255, 154, 154, 0.92);
}

.primary {
  margin-top: 8px;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(180deg, #c19866 0%, #a67c4b 100%);
  border: none;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 1px;
  cursor: pointer;
  transition: filter 200ms ease, transform 100ms ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.primary:disabled {
  opacity: 0.55;
  filter: grayscale(0.2);
  cursor: not-allowed;
}

.primary:not(:disabled):hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.ghost {
  height: 40px;
  border-radius: 14px;
  background: transparent;
  border: 1px dashed rgba(255,255,255,0.20);
  color: rgba(255,255,255,0.70);
  transition: background 160ms ease, border-color 160ms ease;
}

.ghost:hover {
  background: rgba(255,255,255,0.06);
  border-color: rgba(166,138,100,0.35);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.toast {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  font-size: 13px;
  border: 1px solid rgba(255,255,255,0.14);
  background: rgba(8,12,18,0.55);
  color: rgba(255,255,255,0.80);
}

.toast.success {
  border-color: rgba(98, 197, 132, 0.35);
  background: rgba(24, 56, 34, 0.42);
}

.toast.error {
  border-color: rgba(255, 118, 118, 0.38);
  background: rgba(64, 22, 22, 0.45);
}

.hint {
  position: absolute;
  left: clamp(16px, 5vw, 56px);
  bottom: 26px;
  display: grid;
  gap: 8px;
}

@media (max-width: 860px) {
  .hint {
    position: relative;
    left: 0;
    bottom: 0;
    justify-items: center;
    margin-top: 4px;
  }
}

.hint-pill {
  display: inline-flex;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  color: rgba(255,255,255,0.76);
  font-weight: 700;
  letter-spacing: 0.4px;
}

.hint-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.55);
}

.loading-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(0,0,0,0.28);
  backdrop-filter: blur(6px);
}

.loading-card {
  width: 220px;
  height: 140px;
  border-radius: 26px;
  border: 1px solid rgba(255,255,255,0.14);
  background: rgba(10, 16, 24, 0.55);
  display: grid;
  place-items: center;
  gap: 10px;
  box-shadow: 0 36px 70px rgba(0,0,0,0.45);
}

.loading-ring {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  border: 3px solid rgba(255,255,255,0.18);
  border-top-color: rgba(166,138,100,0.95);
  animation: spin 900ms linear infinite;
}

.loading-text {
  font-weight: 700;
  color: rgba(255,255,255,0.78);
}

.dots i {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin: 0 3px;
  border-radius: 999px;
  background: rgba(255,255,255,0.8);
  animation: blink 1s infinite ease-in-out;
}

.dots i:nth-child(2) { animation-delay: 0.15s; }
.dots i:nth-child(3) { animation-delay: 0.30s; }

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes blink {
  0%, 100% { opacity: 0.25; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-1px); }
}

.panel-enter-active, .panel-leave-active {
  transition: opacity 360ms cubic-bezier(0.34, 1.56, 0.64, 1), transform 360ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
.panel-enter-from, .panel-leave-to {
  opacity: 0;
  transform: translateY(18px) scale(0.98);
}

.mask-enter-active, .mask-leave-active { transition: opacity 220ms ease; }
.mask-enter-from, .mask-leave-to { opacity: 0; }

.hint-enter-active, .hint-leave-active { transition: opacity 260ms ease, transform 260ms ease; }
.hint-enter-from, .hint-leave-to { opacity: 0; transform: translateY(8px); }

.toast-enter-active, .toast-leave-active { transition: opacity 200ms ease, transform 200ms ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(6px); }
</style>
