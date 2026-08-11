<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        multiple
        :limit="20"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :on-exceed="onExceed"
        accept="application/pdf"
      >
        <div class="upload-box">
          <el-icon><upload-filled /></el-icon>
          <div>拖拽 PDF 到此处，或点击选择（可多选）</div>
        </div>
      </el-upload>
      <el-button
        class="upload-btn"
        type="primary"
        :loading="uploading"
        :disabled="!files.length"
        @click="doUpload"
      >
        上传发票（{{ files.length }}）
      </el-button>
    </div>
  </el-card>

  <el-card shadow="never" class="table-card">
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="invoiceNumber" label="发票号码" width="220" />
      <el-table-column prop="invoiceDate" label="开票日期" width="120" />
      <el-table-column prop="sellerName" label="销售方" min-width="180" show-overflow-tooltip />
      <el-table-column prop="buyerName" label="购买方" min-width="180" show-overflow-tooltip />
      <el-table-column prop="category" label="项目名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="totalAmount" label="金额" width="100" align="right" />
      <el-table-column prop="taxAmount" label="税额" width="100" align="right" />
      <el-table-column prop="totalWithTax" label="价税合计" width="120" align="right" />
      <el-table-column prop="createdAt" label="上传时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="preview(row)">预览</el-button>
          <el-button link @click="download(row)">下载</el-button>
          <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="onPageChange"
    />
  </el-card>

  <el-dialog v-model="previewVisible" :title="previewTitle" width="70%" top="5vh" destroy-on-close>
    <iframe :src="previewUrl" class="preview-frame" />
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadInvoice, listInvoices, deleteInvoice, fileUrl } from '../api/invoice'

const rows = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const loading = ref(false)
const uploading = ref(false)
const files = ref([])
const uploadRef = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewTitle = ref('')

async function load() {
  loading.value = true
  try {
    const { data } = await listInvoices(currentPage.value - 1, pageSize)
    rows.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function onFileChange(f) {
  if (!files.value.some(x => x.uid === f.uid)) {
    files.value.push(f)
  }
}

function onFileRemove(f) {
  files.value = files.value.filter(x => x.uid !== f.uid)
}

function onExceed() {
  ElMessage.warning('一次最多选 20 张（后端单请求限制 10MB，20 张超出请分批）')
}

async function doUpload() {
  if (!files.value.length) return
  uploading.value = true
  let ok = 0, fail = 0
  try {
    for (const f of files.value) {
      try {
        await uploadInvoice(f.raw)
        ok++
      } catch (e) {
        fail++
        ElMessage.error(`${f.name} 上传失败: ${e.response?.data || ''}`)
      }
    }
    // pocfile: a duplicate number counts as a failure (the backend rejects it with 409)
    if (ok) ElMessage.success(`成功 ${ok} 张${fail ? `，失败 ${fail} 张` : ''}`)
    files.value = []
    uploadRef.value?.clearFiles()
    currentPage.value = 1
    await load()
  } finally {
    uploading.value = false
  }
}

function onPageChange(p) {
  currentPage.value = p
  load()
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function preview(row) {
  previewTitle.value = `发票预览 - ${row.invoiceNumber}`
  previewUrl.value = fileUrl(row.id, 'inline')
  previewVisible.value = true
}

function download(row) {
  const a = document.createElement('a')
  a.href = fileUrl(row.id, 'download')
  a.download = `${row.invoiceNumber}.pdf`
  a.click()
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除发票 ${row.invoiceNumber}？该 PDF 文件将一并删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteInvoice(row.id)
    ElMessage.success('删除成功')
    if (rows.value.length === 1 && currentPage.value > 1) currentPage.value--
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
}
.upload-box {
  padding: 8px 24px;
  font-size: 14px;
  color: #606266;
}
.upload-box .el-icon {
  font-size: 40px;
  color: #c0c4cc;
  margin-bottom: 8px;
}
.table-card {
  margin-top: 16px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.preview-frame {
  width: 100%;
  height: 70vh;
  border: none;
}
</style>
