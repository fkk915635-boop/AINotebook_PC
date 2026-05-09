<template>
  <div class="app-layout">
    <!-- 全局 3D 导航栏（登录页排除在外） -->
    <Navbar3D v-if="!isLoginPage" />
    
    <!-- 主内容区域 -->
    <main class="main-content" :class="{ 'with-sidebar': !isLoginPage }">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import Navbar3D from './components/Navbar3D.vue'

const route = useRoute()
const isLoginPage = computed(() => route.name === 'login')
</script>

<style>
.app-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.main-content {
  flex: 1;
  width: 100%;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.main-content.with-sidebar {
  /* 默认导航栏宽度为 240px，通过 CSS 变量由 Navbar3D 动态修改 */
  margin-left: var(--sidebar-width, 240px);
}

@media (max-width: 768px) {
  .main-content.with-sidebar {
    margin-left: var(--sidebar-width, 80px); /* 小屏幕默认收起状态的间距 */
  }
}
</style>
