import axios from 'axios'

const http = axios.create({ baseURL: '/api' })

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

export function fileUrl(id, disposition = 'inline') {
  return `/api/invoices/${id}/file?disposition=${disposition}`
}
