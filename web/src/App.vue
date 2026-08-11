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
        <button class="sidebar-toggle" @click="collapsed = !collapsed">
          <el-icon><fold v-if="!collapsed" /><expand v-else /></el-icon>
        </button>
      </el-tooltip>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ activeMenu === 'upload' ? '发票上传' : '发票列表' }}</span>
        <el-switch
          class="theme-switch"
          v-model="isDark"
          inline-prompt
          active-text="暗色"
          inactive-text="亮色"
          @change="applyTheme"
        />
      </el-header>
      <el-main>
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

const isDark = ref(false)
const activeMenu = ref('upload')
const collapsed = ref(false)

function applyTheme() {
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

function onMenuSelect(index) {
  activeMenu.value = index
}

function onUploaded() {
  activeMenu.value = 'list'
}

onMounted(() => {
  isDark.value = localStorage.getItem('theme') === 'dark'
  applyTheme()
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: var(--el-bg-color-page); }
.layout { height: 100vh; overflow: hidden; }
.el-main { padding: 20px; overflow: auto; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 16px;
  font-weight: 600;
}
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
html.dark .header {
  border-bottom: 1px solid var(--el-border-color-light);
}
</style>
