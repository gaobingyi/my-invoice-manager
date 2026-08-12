<template>
  <div class="login-page">
    <el-switch
      class="theme-switch"
      v-model="isDark"
      inline-prompt
      active-text="暗色"
      inactive-text="亮色"
      @change="applyTheme"
    />
    <div class="login-card">
      <div class="login-title">
        <img src="/invoice-icon.svg" class="login-logo" alt="logo" />
        <span>发票管理系统</span>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { isDark, applyTheme } from '../utils/theme'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login, setToken, setUsername } from '../api/invoice'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const { data } = await login(form.username, form.password)
    setToken(data.token)
    setUsername(data.username)
    router.push('/upload')
  } catch (e) {
    ElMessage.error(e.response?.data || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page);
}
/* 右上角主题切换，与主界面 header 同款样式 */
.login-page .theme-switch {
  position: absolute;
  top: 24px;
  right: 24px;
}
.login-card {
  width: 360px;
  padding: 40px 32px 32px;
  border-radius: 8px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  box-shadow: var(--el-box-shadow-light);
}
.login-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 28px;
}
.login-logo {
  width: 26px;
  height: 26px;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
}
</style>
