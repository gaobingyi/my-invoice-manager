<template>
  <el-container v-if="routeReady && !isLoginPage" class="layout">
    <el-aside :width="collapsed ? '64px' : '180px'" class="sidebar">
      <div class="sidebar-logo">
        <img src="/invoice-icon.svg" class="sidebar-logo-icon" alt="logo" />
        <span v-show="!collapsed">发票管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        @select="onMenuSelect"
        :collapse="collapsed"
        class="sidebar-menu"
      >
        <el-menu-item index="upload">
          <el-icon><upload-filled /></el-icon>
          <template #title>发票上传</template>
        </el-menu-item>
        <el-menu-item index="list">
          <el-icon><tickets /></el-icon>
          <template #title>发票列表</template>
        </el-menu-item>
      </el-menu>
      <el-tooltip :content="collapsed ? '展开菜单' : '收起菜单'" placement="left" :show-after="300">
        <button class="sidebar-toggle" @click="toggleCollapsed">
          <el-icon><fold v-if="!collapsed" /><expand v-else /></el-icon>
        </button>
      </el-tooltip>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">
          <span class="header-crumb">首页</span>
          <span class="header-current">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-switch
            class="theme-switch"
            v-model="isDark"
            inline-prompt
            active-text="暗色"
            inactive-text="亮色"
            @change="applyTheme"
          />
          <el-dropdown trigger="click" @command="onUserCommand">
            <span class="user-entry">
              <el-icon><user-filled /></el-icon>
              <span>{{ username }}</span>
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  <router-view v-else-if="routeReady" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { UploadFilled, Tickets, Fold, Expand, UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { getUsername, setToken, setUsername } from './api/invoice'
import { isDark, applyTheme } from './utils/theme'

const router = useRouter()
const route = useRoute()

const collapsed = ref(localStorage.getItem('sidebarCollapsed') === 'true')
const username = ref(getUsername() || 'admin')

const activeMenu = computed(() => (route.path.startsWith('/list') ? 'list' : 'upload'))
const pageTitle = computed(() => (activeMenu.value === 'upload' ? '发票上传' : '发票列表'))
const isLoginPage = computed(() => route.path === '/login')
// 首次导航 resolve 前不渲染，避免暗黑下先闪 Layout 再跳登录页
const routeReady = ref(false)
router.isReady().finally(() => { routeReady.value = true })

function onMenuSelect(index) {
  router.push(index === 'upload' ? '/upload' : '/list')
  if (window.matchMedia('(max-width: 768px)').matches) {
    collapsed.value = true
    localStorage.setItem('sidebarCollapsed', 'true')
  }
}

function onUserCommand(command) {
  if (command === 'logout') {
    setToken(null)
    setUsername(null)
    router.push('/login')
  }
}

function toggleCollapsed() {
  collapsed.value = !collapsed.value
  localStorage.setItem('sidebarCollapsed', String(collapsed.value))
}

onMounted(() => {
  applyTheme()
  if (window.matchMedia('(max-width: 768px)').matches && !localStorage.getItem('sidebarCollapsed')) {
    collapsed.value = true
  }
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: var(--el-bg-color-page); }
.layout { height: 100vh; overflow: hidden; }
.main {
  padding: 3px;
  overflow: auto;
  width: 100%;
  background: var(--el-bg-color-page);
}
/* 子级撑满：el-main 内部 div 默认 block，子元素 height:100% 失效 */
.main > div { display: flex; flex-direction: column; min-height: 100%; }
.main > div > * { flex: 1; min-height: 0; }
.main::-webkit-scrollbar { width: 8px; height: 8px; }
.main::-webkit-scrollbar-thumb { background: var(--el-border-color); border-radius: 4px; }
.main::-webkit-scrollbar-thumb:hover { background: var(--el-border-color-light); }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  border-bottom: 1px solid var(--el-border-color-light);
}
.header-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; }
.header-crumb { color: var(--el-text-color-secondary); font-weight: 400; }
.header-crumb::after { content: " / "; margin: 0 6px; color: var(--el-text-color-placeholder); }
.header-current { color: var(--el-text-color-regular); }
.header-right { display: flex; align-items: center; gap: 16px; }
.user-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  font-size: 14px;
  outline: none;
}
.user-entry:hover { color: var(--el-color-primary); }
/* 左侧菜单：占满高度，无外边框；overflow hidden 防止收起时任何内部横向溢出带出滚动条 */
.sidebar {
  position: relative;
  overflow: hidden;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  transition: width 0.2s;
}
.sidebar .el-menu {
  border-right: none;
}
.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 60px;
  font-size: 17px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  white-space: nowrap;
  overflow: hidden;
}
.sidebar-logo-icon {
  width: 22px;
  height: 22px;
}
/* 菜单项去掉高亮下边框（EP 默认 active 项有 2px 底部指示线） */
.sidebar .el-menu-item {
  border-bottom: none;
}
.sidebar .el-menu-item.is-active {
  border-bottom: none;
}
/* 折叠按钮：悬浮在菜单底部 */
.sidebar-toggle {
  position: absolute;
  bottom: 12px;
  width: 100%;
  border: none;
  font-size: 18px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  background: none;
  padding: 8px 0;
}
.sidebar-toggle:hover {
  color: var(--el-color-primary);
}
/* .theme-switch 样式见 src/styles/tokens.css（App.vue / Login.vue 共享） */
</style>
