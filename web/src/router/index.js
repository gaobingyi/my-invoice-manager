import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/invoice'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/', redirect: '/upload' },
  { path: '/upload', name: 'upload', component: () => import('../views/InvoiceUpload.vue') },
  { path: '/list', name: 'list', component: () => import('../views/InvoiceList.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  if (!to.meta.public && !getToken()) {
    return { path: '/login' }
  }
  if (to.path === '/login' && getToken()) {
    return { path: '/upload' }
  }
})

export default router
