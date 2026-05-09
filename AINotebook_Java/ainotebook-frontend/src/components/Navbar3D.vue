<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as THREE from 'three'
import { gsap } from 'gsap' // 需要安装 gsap: npm install gsap
import { LayoutDashboard, FileText, MessageSquare, User, Sparkles, ShieldCheck, ChevronDown } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const isCollapsed = ref(false)
const canvasRef = ref(null)

let scene, camera, renderer
let rafId = 0

// 导航菜单配置
const navItems = [
  { id: 'home', label: '笔记广场', path: '/community', yOffset: 2.25, color: 0xc19866, icon: LayoutDashboard },
  { id: 'notes', label: '我的笔记', path: '/', yOffset: 0.75, color: 0x8d7352, icon: FileText },
  { id: 'chat', label: '聊天', path: '/chat', yOffset: -0.75, color: 0x5f7a91, icon: MessageSquare, badge: 'NEW' },
  { id: 'profile', label: '个人中心', path: '/profile', yOffset: -2.25, color: 0x6b4c3a, icon: User }
]

const meshes = [] // 存储3D模型
const raycaster = new THREE.Raycaster()
const mouse = new THREE.Vector2()
let hoveredMesh = null

function initThree() {
  const container = canvasRef.value
  if (!container) return

  scene = new THREE.Scene()
  
  // 尺寸随侧边栏变化，默认宽度 240px
  const width = container.clientWidth
  const height = container.clientHeight
  
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100)
  camera.position.z = 6

  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  container.appendChild(renderer.domElement)

  // 添加光照
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)
  
  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(5, 5, 5)
  scene.add(directionalLight)

  const geometries = {
    home: new THREE.TorusGeometry(0.45, 0.18, 18, 40),
    notes: new THREE.BoxGeometry(0.8, 0.8, 0.8),
    chat: new THREE.TorusKnotGeometry(0.35, 0.14, 96, 12),
    profile: new THREE.IcosahedronGeometry(0.55, 1),
  }

  navItems.forEach((item) => {
    const material = new THREE.MeshStandardMaterial({ 
      color: item.color,
      roughness: 0.4,
      metalness: 0.35,
      emissive: new THREE.Color(0x000000),
      emissiveIntensity: 0.6
    })
    const geometry = geometries[item.id] || geometries.notes
    const mesh = new THREE.Mesh(geometry, material)
    
    // 初始位置
    mesh.position.y = item.yOffset
    mesh.position.x = isCollapsed.value ? 0 : -1.5 // 展开时偏左，为右侧文字留空间
    
    // 存储业务数据
    mesh.userData = { id: item.id, path: item.path, isHovered: false }
    
    scene.add(mesh)
    meshes.push(mesh)
  })

  // 轻量粒子背景
  createParticles()

  window.addEventListener('resize', onWindowResize)
  container.addEventListener('mousemove', onMouseMove)
  container.addEventListener('click', onClick)
  
  animate()
}

function createParticles() {
  const particleCount = 200
  const geometry = new THREE.BufferGeometry()
  const positions = new Float32Array(particleCount * 3)

  for(let i = 0; i < particleCount * 3; i++) {
    positions[i] = (Math.random() - 0.5) * 10
  }

  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const material = new THREE.PointsMaterial({ 
    color: 0xc19866, 
    size: 0.05,
    transparent: true,
    opacity: 0.4
  })

  const particles = new THREE.Points(geometry, material)
  scene.add(particles)
}

function onWindowResize() {
  const container = canvasRef.value
  if (!container || !camera || !renderer) return
  const width = container.clientWidth
  const height = container.clientHeight
  
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

function onMouseMove(event) {
  const container = canvasRef.value
  const rect = container.getBoundingClientRect()
  
  // 计算归一化设备坐标 (NDC)
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(meshes)

  if (intersects.length > 0) {
    const object = intersects[0].object
    if (hoveredMesh !== object) {
      // 鼠标移入新物体
      if (hoveredMesh) resetMesh(hoveredMesh)
      hoveredMesh = object
      hoverMesh(hoveredMesh)
      container.style.cursor = 'pointer'
    }
  } else {
    if (hoveredMesh) {
      // 鼠标移出物体
      resetMesh(hoveredMesh)
      hoveredMesh = null
      container.style.cursor = 'default'
    }
  }
}

function onClick(event) {
  if (hoveredMesh) {
    const path = hoveredMesh.userData.path
    if (route.path !== path) {
      router.push(path)
    }
  }
}

// 交互动画：Hover
function hoverMesh(mesh) {
  gsap.to(mesh.rotation, {
    y: Math.PI / 4,
    x: Math.PI / 8,
    duration: 0.4,
    ease: "power2.out"
  })
  gsap.to(mesh.scale, {
    x: 1.1, y: 1.1, z: 1.1,
    duration: 0.3
  })
}

// 交互动画：Reset
function resetMesh(mesh) {
  const isCurrentRoute = route.path === mesh.userData.path
  if (isCurrentRoute) return // 当前路由保持高亮状态
  
  gsap.to(mesh.rotation, {
    y: 0, x: 0,
    duration: 0.4,
    ease: "power2.out"
  })
  gsap.to(mesh.scale, {
    x: 1, y: 1, z: 1,
    duration: 0.3
  })
}

// 监听路由变化，更新 3D 选中状态
watch(() => route.path, (newPath) => {
  meshes.forEach(mesh => {
    if (mesh.userData.path === newPath) {
      // 选中状态：放大、高亮
      gsap.to(mesh.scale, { x: 1.2, y: 1.2, z: 1.2, duration: 0.4 })
      gsap.to(mesh.rotation, { y: Math.PI * 2, duration: 1, ease: "power1.inOut" }) // 转一圈
      mesh.material.emissive.setHex(0x442200) // 添加发光
    } else {
      // 恢复非选中状态
      resetMesh(mesh)
      mesh.material.emissive.setHex(0x000000)
    }
  })
})

// 监听折叠状态，更新模型 X 轴位置
watch(isCollapsed, (collapsed) => {
  meshes.forEach(mesh => {
    gsap.to(mesh.position, {
      x: collapsed ? 0 : -1.5,
      duration: 0.5,
      ease: "power2.inOut"
    })
  })
})

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  
  // 派发事件，让父组件 App.vue 知道现在的状态以调整 margin-left
  document.documentElement.style.setProperty('--sidebar-width', isCollapsed.value ? '80px' : '240px')
  
  // 等待 CSS 过渡完成后触发 Three.js resize
  setTimeout(onWindowResize, 350)
}

function animate() {
  rafId = requestAnimationFrame(animate)
  
  // 让所有模型保持轻微的自转
  meshes.forEach(mesh => {
    if (mesh !== hoveredMesh && route.path !== mesh.userData.path) {
      mesh.rotation.y += 0.005
      mesh.rotation.x += 0.002
    }
    if (route.path === mesh.userData.path) {
      const t = Date.now() * 0.002
      mesh.material.emissive.setHex(0x2a1400)
      mesh.material.emissiveIntensity = 0.45 + Math.sin(t) * 0.15
    }
  })

  renderer.render(scene, camera)
}

onMounted(() => {
  initThree()
  // 初始化当前路由状态
  meshes.forEach(mesh => {
    if (mesh.userData.path === route.path) {
      gsap.to(mesh.scale, { x: 1.2, y: 1.2, z: 1.2, duration: 0 })
      mesh.material.emissive.setHex(0x442200)
    }
  })
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onWindowResize)
  if (canvasRef.value) {
    canvasRef.value.removeEventListener('mousemove', onMouseMove)
    canvasRef.value.removeEventListener('click', onClick)
  }
  if (renderer) renderer.dispose()
})
</script>

<template>
  <div 
    class="navbar-container"
    :class="{ 'collapsed': isCollapsed }"
  >
    <!-- Three.js 渲染容器 -->
    <div ref="canvasRef" class="canvas-container"></div>
    
    <!-- HTML 叠加层：用于文字和布局（混合 UI） -->
    <div class="nav-content">
      <div class="logo-area">
        <div class="brand-row">
          <div class="brand-dot"></div>
          <div class="logo" v-if="!isCollapsed">AINotebook</div>
          <div class="logo" v-else>AI</div>
        </div>
        <div v-if="!isCollapsed" class="brand-subtitle">Personal Thinking OS</div>
      </div>

      <div class="workspace-card" v-if="!isCollapsed">
        <div class="workspace-left">
          <Sparkles :size="14" />
          <span>AINotebook Space</span>
        </div>
        <ChevronDown :size="14" />
      </div>

      <nav class="nav-menu" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.id"
          class="nav-item-html"
          :class="{ 'active': route.path === item.path }"
          @click="router.push(item.path)"
          type="button"
        >
          <span class="nav-icon-wrap">
            <component :is="item.icon" :size="16" />
          </span>
          <span class="label" v-show="!isCollapsed">{{ item.label }}</span>
          <span v-if="item.badge && !isCollapsed" class="badge">{{ item.badge }}</span>
        </button>
      </nav>

      <div class="account-card" v-if="!isCollapsed">
        <div class="account-avatar">AI</div>
        <div class="account-info">
          <div class="account-name">本地助手</div>
          <div class="account-role">deepseek-r1:7b</div>
        </div>
        <ShieldCheck :size="14" class="account-shield" />
      </div>

      <div class="toggle-btn" @click="toggleCollapse">
        <svg v-if="!isCollapsed" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
      </div>
    </div>
  </div>
</template>

<style scoped>
.navbar-container {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 240px;
  background: rgba(15, 25, 34, 0.85);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 10px 0 30px rgba(0, 0, 0, 0.3);
}

.navbar-container.collapsed {
  width: 80px;
}

.canvas-container {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: auto; /* 允许 Three.js 捕获鼠标事件 */
}

.nav-content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  pointer-events: none; /* 让鼠标事件穿透到 Canvas，需要点击的元素单独恢复 */
}

.logo-area {
  padding: 18px 14px 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #c19866, #f5d9b8);
  box-shadow: 0 0 16px rgba(193, 152, 102, 0.8);
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #f8f3ed;
  letter-spacing: 0.4px;
}

.brand-subtitle {
  margin-top: 6px;
  padding-left: 20px;
  color: rgba(248, 243, 237, 0.55);
  font-size: 11px;
  letter-spacing: 0.3px;
}

.workspace-card {
  margin: 14px 12px 10px 12px;
  pointer-events: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(248, 243, 237, 0.85);
  font-size: 12px;
}

.workspace-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding: 6px 10px 0 10px;
  margin: 0;
  gap: 6px;
}

/* 
  HTML 层只负责文字渲染和路由点击（备用）。
  由于 3D 图标在 Y 轴上是 -1.5, 0, 1.5 分布的，我们需要让 HTML 文字垂直居中对齐它们。
*/
.nav-item-html {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 10px;
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.collapsed .nav-item-html {
  justify-content: center;
  padding: 0;
}

.nav-icon-wrap {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(248, 243, 237, 0.75);
  margin-right: 10px;
}

.collapsed .nav-icon-wrap {
  margin-right: 0;
}

.label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(248, 243, 237, 0.7);
  transition: color 0.3s ease, transform 0.3s ease;
}

.badge {
  margin-left: auto;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(123, 160, 255, 0.18);
  color: #9dc2ff;
  border: 1px solid rgba(157, 194, 255, 0.28);
}

.nav-item-html:hover {
  border-color: rgba(193, 152, 102, 0.25);
  background: rgba(193, 152, 102, 0.07);
}

.nav-item-html:hover .label,
.nav-item-html:hover .nav-icon-wrap {
  color: #fff;
}

.nav-item-html.active .label {
  color: #c19866;
  font-weight: 700;
}

.nav-item-html.active .nav-icon-wrap {
  color: #d7b284;
}

.nav-item-html.active {
  border-color: rgba(193, 152, 102, 0.42);
  background: rgba(193, 152, 102, 0.12);
}

.account-card {
  margin: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  align-items: center;
  gap: 10px;
  pointer-events: auto;
}

.account-avatar {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: linear-gradient(135deg, #c19866, #8d7352);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.account-info {
  min-width: 0;
  flex: 1;
}

.account-name {
  color: #f8f3ed;
  font-size: 12px;
  font-weight: 600;
}

.account-role {
  color: rgba(248, 243, 237, 0.55);
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.account-shield {
  color: rgba(130, 200, 150, 0.8);
}

.toggle-btn {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  pointer-events: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

.toggle-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}
</style>
