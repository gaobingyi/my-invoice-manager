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
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadInvoice } from '../api/invoice'

const uploading = ref(false)
const files = ref([])
const uploadRef = ref(null)

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

const emit = defineEmits(['uploaded'])

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
    if (ok) {
      ElMessage.success(`成功 ${ok} 张${fail ? `，失败 ${fail} 张` : ''}`)
      if (!fail) emit('uploaded')
    } else if (fail) {
      ElMessage.error(`上传失败 ${fail} 张，请检查 PDF 是否损坏或已存在`)
    }
    files.value = []
    uploadRef.value?.clearFiles()
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
:deep(.main > div > .el-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 100%;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 3px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex: 1;
}
.toolbar .el-upload {
  flex: 1;
  max-width: 560px;
  min-width: 0;
  display: flex;
}
.toolbar .el-upload-dragger {
  width: 100%;
  flex: 1;
  padding: 8px;
  min-height: 220px;
}
.toolbar .upload-btn {
  align-self: center;
}
.upload-box {
  padding: 8px 24px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.upload-box .el-icon {
  font-size: 40px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 8px;
}
/* 窄屏：工具栏换行 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  .toolbar .upload-btn {
    width: 100%;
  }
}
</style>
