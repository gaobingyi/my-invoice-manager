<template>
  <el-container class="layout">
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
          <span class="header-current">{{ activeMenu === 'upload' ? '发票上传' : '发票列表' }}</span>
       </div>
        <el-switch
          class="theme-switch"
          v-model="isDark"
          inline-prompt
          active-text="暗色"
          inactive-text="亮色"
          @change="applyTheme"
        />
      </el-header>
      <el-main class="main">
        <InvoiceUpload
          v-if="activeMenu === 'upload'"
          @uploaded="onUploaded"
        />
        <InvoiceList v-else />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UploadFilled, Tickets, Fold, Expand } from '@element-plus/icons-vue'
import InvoiceList from './views/InvoiceList.vue'
import InvoiceUpload from './views/InvoiceUpload.vue'

const isDark = ref(localStorage.getItem('theme') === 'dark')
const activeMenu = ref(localStorage.getItem('activeMenu') || 'upload')
const collapsed = ref(localStorage.getItem('sidebarCollapsed') === 'true')

function applyTheme() {
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

function onMenuSelect(index) {
  activeMenu.value = index
  localStorage.setItem('activeMenu', index)
  if (window.innerWidth <= 768) {
    collapsed.value = true
    localStorage.setItem('sidebarCollapsed', 'true')
  }
}

function onUploaded() {
  activeMenu.value = 'list'
  localStorage.setItem('activeMenu', 'list')
}

function toggleCollapsed() {
  collapsed.value = !collapsed.value
  localStorage.setItem('sidebarCollapsed', String(collapsed.value))
}

onMounted(() => {
  applyTheme()
  if (window.innerWidth <= 768 && !localStorage.getItem('sidebarCollapsed')) {
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
/* 开关加高、加宽，文字留呼吸空间 */
.theme-switch {
  min-width: 64px;
}
.theme-switch .el-switch__core {
  height: 26px;
  border-radius: 13px;
}
.theme-switch .el-switch__core .el-switch__inner {
  padding: 0 12px 0 26px;
}
.theme-switch.is-checked .el-switch__core .el-switch__inner {
  padding: 0 26px 0 12px;
}
.theme-switch .el-switch__core .el-switch__action {
  width: 20px;
  height: 20px;
}
.theme-switch.is-checked .el-switch__core .el-switch__action {
  left: calc(100% - 21px);
}
/* 开关 inline-prompt 文字默认硬编码白（.el-switch__inner-wrapper），亮色下跟随主题文字色 */
html:not(.dark) .theme-switch .el-switch__inner-wrapper {
  color: var(--el-text-color-regular);
}
</style>
