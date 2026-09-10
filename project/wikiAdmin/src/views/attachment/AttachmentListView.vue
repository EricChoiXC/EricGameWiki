<template>
  <div class="u-list-page">
    <!-- 筛选区（规范第 14 章：el-row/el-col，3 栅栏宽划分每行） -->
    <el-card class="u-list-page__filter" shadow="never">
      <el-row :gutter="0">
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">文件名称</span>
            <el-input
              v-model="filter.fieldName"
              placeholder="请输入文件名称"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">模型名称</span>
            <el-input
              v-model="filter.fieldModelName"
              placeholder="请输入模型名称"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">上传者</span>
            <el-input
              v-model="filter.fieldUploaderId"
              placeholder="请输入上传者id"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 排序和按钮区 -->
    <div class="u-list-page__toolbar">
      <div class="u-list-page__sort">
        <button
          class="u-list-page__sort-btn"
          :class="{ 'is-active': sortField === 'fieldCreateTime' }"
          @click="toggleSort('fieldCreateTime')"
        >
          创建时间
          <el-icon v-if="sortField === 'fieldCreateTime'">
            <CaretTop v-if="sortOrder === 'asc'" />
            <CaretBottom v-else />
          </el-icon>
        </button>
      </div>
      <div class="u-list-page__actions">
        <el-button :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="onReset">重置</el-button>
        <el-upload
          v-if="hasAttachmentPermission"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
        >
          <el-button type="primary" :icon="Upload" :loading="uploading">上传附件</el-button>
        </el-upload>
      </div>
    </div>

    <!-- 列表区 -->
    <el-card class="u-list-page__table" shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="fieldName" label="文件名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="fieldModelName" label="模型名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="fieldModelId" label="模型id" min-width="120" show-overflow-tooltip />
        <el-table-column prop="fieldKey" label="key" min-width="120" show-overflow-tooltip />
        <el-table-column label="上传者" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.fieldUploaderName || row.fieldUploaderId || '-' }}</template>
        </el-table-column>
        <el-table-column label="文件大小" min-width="120" align="right">
          <template #default="{ row }">{{ formatFileSize(row.fieldFileSize) }}</template>
        </el-table-column>
        <el-table-column prop="fieldFilePath" label="文件路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="删除标志" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.fieldDeleteFlag ? 'danger' : 'success'" size="small">
              {{ row.fieldDeleteFlag ? '已删除' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onDownload(row)">下载</el-button>
            <el-button
              v-if="hasAttachmentPermission"
              link
              type="danger"
              size="small"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.pageNum"
        v-model:page-size="page.pageSize"
        :total="page.total"
        :page-sizes="[15, 30, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 上传参数弹窗（填写模型名称、模型id、key） -->
    <el-dialog v-model="uploadDialogVisible" title="上传附件" width="480px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="文件名称">
          <el-input :model-value="uploadForm.fileName" disabled />
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input v-model="uploadForm.fieldModelName" placeholder="请输入模型名称，如 system" />
        </el-form-item>
        <el-form-item label="模型id">
          <el-input v-model="uploadForm.fieldModelId" placeholder="请输入模型id（可空）" />
        </el-form-item>
        <el-form-item label="key">
          <el-input v-model="uploadForm.fieldKey" placeholder="请输入关联key（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="confirmUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onActivated, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CaretBottom, CaretTop, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { attachmentApi } from '@/api/attachment'
import { useUserStore } from '@/stores/user'
import { buildListQuery, eq, like } from '@/utils/query'
import { PERMISSION_CODE } from '@/utils/constants'

const userStore = useUserStore()

const hasAttachmentPermission = userStore.hasPermission(PERMISSION_CODE.ATTACHMENT_ADMIN)

// 筛选条件
const filter = reactive({
  fieldName: '',
  fieldModelName: '',
  fieldUploaderId: ''
})

// 排序（单排序）
const sortField = ref('fieldCreateTime')
const sortOrder = ref('desc')

// 分页与数据
const loading = ref(false)
const tableData = ref([])
const page = reactive({
  pageNum: 1,
  pageSize: 15,
  total: 0
})

// 上传相关
const uploading = ref(false)
const uploadDialogVisible = ref(false)
const pendingFile = ref(null)
const uploadForm = reactive({
  fileName: '',
  fieldModelName: '',
  fieldModelId: '',
  fieldKey: ''
})

function formatFileSize(size) {
  if (size == null) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function buildPayload() {
  return buildListQuery(
    [
      like('fieldName', filter.fieldName),
      like('fieldModelName', filter.fieldModelName),
      eq('fieldUploaderId', filter.fieldUploaderId)
    ],
    {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      sortField: sortField.value,
      sortOrder: sortOrder.value
    }
  )
}

async function fetchList() {
  loading.value = true
  try {
    const res = await attachmentApi.list(buildPayload())
    tableData.value = res.list || []
    page.total = res.query?.total ?? 0
  } finally {
    loading.value = false
  }
}

function toggleSort(field) {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortOrder.value = 'desc'
  }
  onSearch()
}

function onSearch() {
  page.pageNum = 1
  fetchList()
}

function onReset() {
  filter.fieldName = ''
  filter.fieldModelName = ''
  filter.fieldUploaderId = ''
  sortField.value = 'fieldCreateTime'
  sortOrder.value = 'desc'
  onSearch()
}

// 上传前拦截：暂存文件并弹出参数弹窗
function beforeUpload(file) {
  pendingFile.value = file
  uploadForm.fileName = file.name
  uploadForm.fieldModelName = ''
  uploadForm.fieldModelId = ''
  uploadForm.fieldKey = ''
  uploadDialogVisible.value = true
  // 返回 false 阻止自动上传，由自定义 http-request 处理
  return false
}

async function confirmUpload() {
  if (!pendingFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!uploadForm.fieldModelName) {
    ElMessage.warning('模型名称不能为空')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', pendingFile.value)
    formData.append('fieldModelName', uploadForm.fieldModelName)
    formData.append('fieldModelId', uploadForm.fieldModelId || '')
    formData.append('fieldKey', uploadForm.fieldKey || '')
    await attachmentApi.upload(formData)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    pendingFile.value = null
    fetchList()
  } finally {
    uploading.value = false
  }
}

// 自定义上传请求（当 beforeUpload 返回 false 时不会触发，此处保留用于直接上传场景）
async function handleUpload() {
  // 由 confirmUpload 统一处理，此处不实现
}

async function onDownload(row) {
  try {
    const res = await attachmentApi.download(row.fieldId)
    const blob = new Blob([res])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.fieldName || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch {
    // 下载失败提示由响应拦截器统一处理
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除附件「${row.fieldName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await attachmentApi.remove(row.fieldId)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
onActivated(fetchList)
</script>

<style lang="scss" scoped>
.u-list-page__filter {
  :deep(.el-card__body) {
    padding: 0;
  }

  .u-list-page__filter-item {
    :deep(.el-select) {
      width: 100%;
    }
  }
}

.u-list-page__table {
  :deep(.el-card__body) {
    padding: var(--spacing-md);
  }
}
</style>
