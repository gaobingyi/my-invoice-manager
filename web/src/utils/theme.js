import { ref } from 'vue'

const STORAGE_KEY = 'theme'

const isDark = ref(localStorage.getItem(STORAGE_KEY) === 'dark')

export function applyTheme() {
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem(STORAGE_KEY, isDark.value ? 'dark' : 'light')
}

export { isDark }
