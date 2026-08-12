import axios from 'axios'

const TOKEN_KEY = 'token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function getUsername() {
  return localStorage.getItem('username')
}

export function setUsername(name) {
  if (name) localStorage.setItem('username', name)
  else localStorage.removeItem('username')
}

const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use(cfg => {
  const token = getToken()
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  res => res,
  err => {
    const isLoginCall = err.config?.url?.endsWith('/auth/login')
    if (err.response?.status === 401 && !isLoginCall) {
      setToken(null)
      setUsername(null)
      // 避免路由依赖：直接整页跳登录
      if (!location.pathname.startsWith('/login')) location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

export function uploadInvoice(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/invoices/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listInvoices(page, size) {
  return http.get('/invoices', { params: { page, size } })
}

export function deleteInvoice(id) {
  return http.delete(`/invoices/${id}`)
}

/** 预览/下载统一走 blob（裸 URL 带不了 Authorization header）。
 * 返回 { url, blob }：url 必须由调用方 URL.revokeObjectURL() 释放，否则 PDF 字节驻留内存。
 * 返回 blob 是为了下载场景下调用方能在 click 后立刻 revoke —— 浏览器已开始下载即可。 */
export async function fetchFile(id, disposition = 'inline') {
  const { data } = await http.get(`/invoices/${id}/file`, {
    params: { disposition },
    responseType: 'blob'
  })
  return { url: URL.createObjectURL(data), blob: data }
}
