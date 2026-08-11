import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'

// dark class 由 index.html <head> 同步脚本注入，避免刷新白闪

createApp(App).use(ElementPlus, { locale: zhCn }).mount('#app')
