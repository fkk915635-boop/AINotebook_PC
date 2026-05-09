<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../services/http'
import { setToken } from '../services/auth'
import * as THREE from 'three'

const router = useRouter()
const route = useRoute()

const form = ref({ phone: '', code: '' })
const msg = ref({ text: '', type: '' })
const loading = ref(false)

const countdown = ref(0)
let timer = null

const canSubmit = computed(() => form.value.phone.trim().length === 11 && form.value.code.trim().length >= 4)

const isTyping = ref(false)
const isSurprised = ref(false)
let typingTimer = 0

function markTyping() {
  isTyping.value = true
  window.clearTimeout(typingTimer)
  typingTimer = window.setTimeout(() => {
    isTyping.value = false
  }, 650)
}

function showMsg(text, type = 'info') {
  msg.value = { text, type }
  window.clearTimeout(showMsg._t)
  showMsg._t = window.setTimeout(() => (msg.value = { text: '', type: '' }), 2600)
}

async function sendCode() {
  if (form.value.phone.length !== 11) {
    showMsg('请输入正确的手机号', 'error')
    return
  }
  try {
    await http.post('/auth/send-code', { phone: form.value.phone })
    showMsg('验证码已发送 (测试环境验证码: 123456)', 'success')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    showMsg(e?.response?.data?.message || '发送失败', 'error')
  }
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  isSurprised.value = true
  try {
    const res = await http.post('/auth/login', { phone: form.value.phone.trim(), code: form.value.code.trim() })
    const token = res?.data?.token || res?.data
    if (!token) throw new Error('登录失败')
    setToken(token)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await new Promise((r) => setTimeout(r, 800))
    router.replace(redirect)
  } catch (e) {
    isSurprised.value = false
    showMsg(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    loading.value = false
  }
}

const canvasRef = ref(null)
let raf = 0
let scene, camera, renderer, particles

function resize() {
  if (!camera || !renderer) return
  const w = window.innerWidth
  const h = window.innerHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

function initThree() {
  const el = canvasRef.value
  if (!el) return

  const w = window.innerWidth
  const h = window.innerHeight

  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x050b12, 0.0012)

  camera = new THREE.PerspectiveCamera(75, w / h, 1, 2000)
  camera.position.z = 1000

  renderer = new THREE.WebGLRenderer({ canvas: el, alpha: true, antialias: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(w, h)
  renderer.setClearColor(0x050b12, 1)

  const geometry = new THREE.BufferGeometry()
  const count = 2200
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)
  const color = new THREE.Color()

  for (let i = 0; i < positions.length; i += 3) {
    const r = 820 + Math.random() * 820
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)

    positions[i] = r * Math.sin(phi) * Math.cos(theta)
    positions[i + 1] = r * Math.sin(phi) * Math.sin(theta)
    positions[i + 2] = r * Math.cos(phi)

    const hue = 0.55 + Math.random() * 0.15
    const lightness = 0.45 + Math.random() * 0.5
    color.setHSL(hue, 0.75, lightness)
    colors[i] = color.r
    colors[i + 1] = color.g
    colors[i + 2] = color.b
  }

  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))

  const material = new THREE.PointsMaterial({
    size: 4,
    vertexColors: true,
    transparent: true,
    opacity: 0.75,
    sizeAttenuation: true,
    blending: THREE.AdditiveBlending
  })

  particles = new THREE.Points(geometry, material)
  scene.add(particles)

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
  scene.add(ambientLight)

  const pointLight = new THREE.PointLight(0xffdcb4, 1, 1000)
  pointLight.position.set(0, 0, 200)
  scene.add(pointLight)
}

function draw() {
  raf = requestAnimationFrame(draw)
  if (particles) {
    const time = Date.now() * 0.00005
    particles.rotation.y = time * 0.5
    particles.rotation.x = time * 0.18
  }
  renderer.render(scene, camera)
}

onMounted(() => {
  initThree()
  window.addEventListener('resize', resize)
  draw()
})

const charARef = ref(null)
const charCRef = ref(null)
let eyeRaf = 0
let lastPoint = null

function clamp(n, min, max) {
  return Math.max(min, Math.min(max, n))
}

function applyLook(el, clientX, clientY) {
  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const dx = clientX - cx
  const dy = clientY - cy
  const max = 6
  const x = clamp(dx / 80, -max, max)
  const y = clamp(dy / 80, -max, max)
  el.style.setProperty('--look-x', `${x}px`)
  el.style.setProperty('--look-y', `${y}px`)
}

function scheduleEyeUpdate() {
  if (eyeRaf) return
  eyeRaf = requestAnimationFrame(() => {
    eyeRaf = 0
    if (!lastPoint) return
    const a = charARef.value
    const c = charCRef.value
    if (a) applyLook(a, lastPoint.x, lastPoint.y)
    if (c) applyLook(c, lastPoint.x, lastPoint.y)
  })
}

function onPointerMove(e) {
  lastPoint = { x: e.clientX, y: e.clientY }
  scheduleEyeUpdate()
}

function resetLook() {
  const a = charARef.value
  const c = charCRef.value
  if (a) {
    a.style.setProperty('--look-x', `0px`)
    a.style.setProperty('--look-y', `0px`)
  }
  if (c) {
    c.style.setProperty('--look-x', `0px`)
    c.style.setProperty('--look-y', `0px`)
  }
}

function poke(refEl) {
  const el = refEl?.value
  if (!el) return
  el.classList.remove('poke')
  void el.offsetWidth
  el.classList.add('poke')
  window.setTimeout(() => el.classList.remove('poke'), 420)
}

onMounted(() => {
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerleave', resetLook)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  cancelAnimationFrame(eyeRaf)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerleave', resetLook)
  window.removeEventListener('resize', resize)
  if (timer) clearInterval(timer)
  window.clearTimeout(typingTimer)
  if (renderer) renderer.dispose()
})
</script>

<template>
  <div class="auth-root">
    <canvas ref="canvasRef" class="auth-bg" aria-hidden="true"></canvas>

    <div class="auth-shell">
      <section class="hero-card" aria-hidden="true">
        <div class="hero-top">
          <div class="brand">
            <span class="brand-dot"></span>
            <span class="brand-text">AINotebook</span>
          </div>
          <div class="brand-sub">动态登录</div>
        </div>

        <div class="hero-characters" :class="{ typing: isTyping, surprised: isSurprised }">
          <div class="char char-a" ref="charARef" @click="poke(charARef)" role="button" tabindex="-1">
            <div class="eyes"><span></span><span></span></div>
            <div class="mouth"></div>
          </div>
          <div class="char char-b">
            <div class="eyes"><span></span><span></span></div>
          </div>
          <div class="char char-c" ref="charCRef" @click="poke(charCRef)" role="button" tabindex="-1">
            <div class="eyes"><span></span><span></span></div>
            <div class="mouth"></div>
          </div>
        </div>

        <div class="hero-foot">
          <div class="hero-link">隐私政策</div>
          <div class="hero-link">服务条款</div>
          <div class="hero-link">联系我们</div>
        </div>
      </section>

      <section class="login-card" aria-label="登录或注册">
        <div class="login-title">
          <div class="login-h1">欢迎回来！</div>
          <div class="login-sub">请输入手机号与验证码</div>
        </div>

        <form class="login-form" @submit.prevent="submit">
          <label class="field">
            <span class="field-label">手机号</span>
            <input
              v-model="form.phone"
              @input="markTyping"
              class="field-input"
              name="phone"
              type="tel"
              autocomplete="tel"
              placeholder="11 位手机号"
              maxlength="11"
              required
            />
          </label>

          <label class="field">
            <span class="field-label">验证码</span>
            <div class="code-row">
              <input
                v-model="form.code"
                @input="markTyping"
                class="field-input code-input"
                name="code"
                type="text"
                autocomplete="one-time-code"
                placeholder="4-6 位"
                maxlength="6"
                required
              />
              <button
                type="button"
                class="btn-ghost"
                @click="sendCode"
                :disabled="countdown > 0 || form.phone.length !== 11"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </label>

          <button class="btn-primary" type="submit" :disabled="!canSubmit || loading">
            <span v-if="!loading">登录 / 注册</span>
            <span v-else class="btn-loading"></span>
          </button>
        </form>

        <div v-if="msg.text" class="toast" :class="msg.type" role="status" aria-live="polite">{{ msg.text }}</div>
      </section>
    </div>
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

.auth-shell {
  position: relative;
  max-width: 1080px;
  margin: 0 auto;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  padding: clamp(18px, 4vw, 44px);
  align-items: center;
  z-index: 1;
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
    align-items: start;
    padding-top: 54px;
  }
}

.hero-card {
  position: relative;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(15, 25, 34, 0.78);
  backdrop-filter: blur(14px);
  min-height: 560px;
  box-shadow: 0 24px 60px rgba(0,0,0,0.35);
}

.hero-top {
  padding: 18px 18px 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 3;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  color: #f8f3ed;
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #c19866, #f5d9b8);
  box-shadow: 0 0 16px rgba(193, 152, 102, 0.7);
}

.brand-text {
  letter-spacing: 0.4px;
}

.brand-sub {
  color: rgba(248, 243, 237, 0.55);
  font-size: 12px;
}

.hero-characters {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  z-index: 2;
  --eye-size: 10px;
}

.hero-characters.typing {
  --eye-size: 14px;
}

.hero-characters.surprised {
  --eye-size: 16px;
}

.char {
  position: absolute;
  border-radius: 28px;
  box-shadow: 0 24px 50px rgba(0,0,0,0.35);
  transform-origin: center;
  cursor: pointer;
  user-select: none;
  touch-action: manipulation;
  will-change: transform;
}

.char .eyes {
  position: absolute;
  top: 56px;
  left: 50%;
  transform: translateX(-50%) translate(var(--look-x, 0px), var(--look-y, 0px));
  display: flex;
  gap: 16px;
  transition: transform 120ms ease;
}

.char .eyes span {
  width: var(--eye-size);
  height: var(--eye-size);
  background: rgba(15, 25, 34, 0.95);
  border-radius: 999px;
  animation: blink 4.6s infinite;
}

.char-a {
  width: 320px;
  height: 360px;
  background: linear-gradient(135deg, #4f6bed, #6c5ce7);
  left: 42%;
  top: 50%;
  animation: floatA 5.5s ease-in-out infinite;
}

.char-b {
  width: 140px;
  height: 300px;
  background: linear-gradient(180deg, #0b1220, #111827);
  left: 58%;
  top: 54%;
  border-radius: 18px;
  animation: floatB 6.2s ease-in-out infinite;
}

.char-c {
  width: 210px;
  height: 340px;
  background: linear-gradient(180deg, #f8e35b, #f4d03f);
  left: 74%;
  top: 60%;
  border-radius: 90px;
  animation: floatC 5.9s ease-in-out infinite;
  border: 2px solid rgba(15, 25, 34, 0.75);
}

.char-a .mouth,
.char-c .mouth {
  position: absolute;
  width: 54px;
  height: 10px;
  border-radius: 999px;
  background: rgba(15, 25, 34, 0.75);
  top: 90px;
  left: 50%;
  transform: translateX(-50%);
}

.hero-characters.surprised .char-a .mouth,
.hero-characters.surprised .char-c .mouth {
  width: 44px;
  height: 34px;
  border-radius: 18px 18px 999px 999px;
}

.hero-foot {
  position: absolute;
  bottom: 18px;
  left: 18px;
  right: 18px;
  display: flex;
  gap: 18px;
  justify-content: center;
  z-index: 3;
}

.hero-link {
  color: rgba(248, 243, 237, 0.55);
  font-size: 12px;
}

.login-card {
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(15, 25, 34, 0.86);
  backdrop-filter: blur(14px);
  box-shadow: 0 24px 60px rgba(0,0,0,0.35);
  padding: 22px;
}

.login-title {
  margin-bottom: 14px;
}

.login-h1 {
  font-size: 28px;
  font-weight: 800;
  color: #f8f3ed;
  letter-spacing: -0.2px;
}

.login-sub {
  margin-top: 6px;
  color: rgba(248, 243, 237, 0.55);
  font-size: 13px;
}

.login-form {
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.field-label {
  font-size: 12px;
  color: rgba(248, 243, 237, 0.65);
}

.field-input {
  width: 100%;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(255, 255, 255, 0.05);
  padding: 12px 14px;
  color: #f8f3ed;
  outline: none;
}

.field-input:focus {
  border-color: rgba(193, 152, 102, 0.45);
  box-shadow: 0 0 0 4px rgba(193, 152, 102, 0.14);
}

.code-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.code-input {
  flex: 1;
}

.btn-ghost {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(255, 255, 255, 0.05);
  padding: 12px 12px;
  color: rgba(248, 243, 237, 0.85);
  cursor: pointer;
  min-width: 110px;
}

.btn-ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  margin-top: 4px;
  border-radius: 14px;
  border: 1px solid rgba(193, 152, 102, 0.45);
  background: rgba(193, 152, 102, 0.22);
  padding: 12px 14px;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 120ms ease, background 200ms ease;
}

.btn-primary:hover {
  background: rgba(193, 152, 102, 0.30);
}

.btn-primary:active {
  transform: translateY(1px);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-loading {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
}

.toast {
  margin-top: 14px;
  border-radius: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(248, 243, 237, 0.9);
  font-size: 13px;
}

.toast.success {
  border-color: rgba(130, 200, 150, 0.35);
  background: rgba(130, 200, 150, 0.10);
}

.toast.error {
  border-color: rgba(255, 120, 120, 0.35);
  background: rgba(255, 120, 120, 0.10);
}

@keyframes blink {
  0%, 90%, 100% { transform: scaleY(1); }
  92%, 96% { transform: scaleY(0.12); }
}

@keyframes floatA {
  0%, 100% { transform: translate(-50%, -50%) translateY(0); }
  50% { transform: translate(-50%, -50%) translateY(-10px); }
}

@keyframes floatB {
  0%, 100% { transform: translate(-50%, -50%) translateY(0); }
  50% { transform: translate(-50%, -50%) translateY(12px); }
}

@keyframes floatC {
  0%, 100% { transform: translate(-50%, -50%) translateY(0); }
  50% { transform: translate(-50%, -50%) translateY(-8px); }
}

@keyframes poke {
  0% { transform: translate(-50%, -50%) scale(1); }
  40% { transform: translate(-50%, -50%) scale(1.06) rotate(-2deg); }
  100% { transform: translate(-50%, -50%) scale(1); }
}

.char.poke {
  animation: poke 420ms ease;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
