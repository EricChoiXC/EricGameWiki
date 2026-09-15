<template>
  <div class="attachment-uploader" :class="{ 'is-view': isView }">
    <!-- 编辑态：上传按钮 -->
    <el-upload
      v-if="!isView"
      class="attachment-uploader__upload"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      :accept="acceptAttr"
      :disabled="uploading || !canUpload"
    >
      <el-button type="primary" :icon="Upload" :loading="uploading" :disabled="!canUpload">
        选择文件
      </el-button>
      <template #tip>
        <div class="attachment-uploader__tip">
          <template v-if="tipText">{{ tipText }}</template>
          <template v-if="!canUpload">
            <span class="attachment-uploader__tip-warning">（请先保存主体后再上传附件）</span>
          </template>
        </div>
      </template>
    </el-upload>

    <!-- 附件列表（编辑态与查看态均展示） -->
    <div v-loading="loading" class="attachment-uploader__list">
      <template v-if="files.length > 0">
        <div
          v-for="(item, index) in files"
          :key="item.fieldId"
          class="attachment-uploader__item"
        >
          <el-icon class="attachment-uploader__item-icon"><Document /></el-icon>
          <span
            class="attachment-uploader__item-name"
            :title="item.fieldName"
            @click="onDownload(item)"
          >
            {{ item.fieldName }}
          </span>
          <span class="attachment-uploader__item-size">{{ formatFileSize(item.fieldFileSize) }}</span>
          <el-button
            v-if="!isView"
            link
            type="danger"
            :icon="Delete"
            class="attachment-uploader__item-remove"
            :loading="removingId === item.fieldId"
            @click="onRemove(index, item)"
          />
        </div>
      </template>
      <div v-else-if="!loading" class="attachment-uploader__empty">
        {{ isView ? '暂无附件' : '暂无附件，请上传' }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document, Upload } from '@element-plus/icons-vue'
import { attachmentApi } from '@/api/attachment'
import { buildListQuery, eq } from '@/utils/query'
import {
  buildFileSizeTip,
  buildFileTypeTip,
  formatFileSize,
  validateFileSize,
  validateFileType
} from '@/utils/file'

const props = defineProps({
  /** 状态：edit 编辑 / view 查看；查看态仅展示与下载，不显示上传与删除 */
  status: {
    type: String,
    default: 'edit',
    validator: (v) => ['edit', 'view'].includes(v)
  },
  /** 传递给接口的 fieldModelName，必填（docs/admin/附件机制.md 附件关联） */
  fieldModelName: {
    type: String,
    required: true
  },
  /** 传递给接口的 fieldModelId；新建场景由父组件预生成主键后传入 */
  fieldModelId: {
    type: String,
    default: ''
  },
  /** 传递给接口的 fieldKey */
  fieldKey: {
    type: String,
    default: ''
  },
  /** 可上传文件格式，如 '.jpg' 或 ['.jpg', '.png']；不填则不限制 */
  fileType: {
    type: [String, Array],
    default: ''
  },
  /** 可上传文件大小限制（byte）；不填或 <=0 表示不限制 */
  maxSize: {
    type: Number,
    default: 0
  },
  /** 是否可上传多个附件 */
  multi: {
    type: Boolean,
    default: false
  },
  /** 上传结束后的回调函数（docs/common/前端公共组件.md 附件组件） */
  uploadCallback: {
    type: Function,
    default: null
  },
  /** 确认上传文件到触发后端上传接口中间的校验函数；函数不为空时必须返回 true 才能继续上传 */
  uploadValidate: {
    type: Function,
    default: null
  },
  /** 删除结束后的回调函数 */
  deleteCallback: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['upload', 'delete'])

const isView = computed(() => props.status === 'view')
const canUpload = computed(() => !!props.fieldModelName && !!props.fieldModelId)

const loading = ref(false)
const uploading = ref(false)
const removingId = ref('')
const files = ref([])

// accept 属性：将 fileType 规范化后交给 el-upload
const acceptAttr = computed(() => {
  if (!props.fileType) return undefined
  const list = Array.isArray(props.fileType) ? props.fileType : [props.fileType]
  return list.map((e) => (String(e).trim().startsWith('.') ? String(e).trim() : '.' + String(e).trim())).join(',')
})

// 提示文案
const tipText = computed(() => {
  const parts = []
  const typeTip = buildFileTypeTip(props.fileType)
  if (typeTip) parts.push(typeTip)
  if (props.maxSize && props.maxSize > 0) parts.push(`单个文件不能超过 ${formatFileSize(props.maxSize)}`)
  return parts.length > 0 ? parts.join('；') : ''
})

// 监听关联对象变化，重新加载附件列表
watch(
  () => [props.fieldModelName, props.fieldModelId, props.fieldKey],
  () => loadList(),
  { immediate: true }
)

/**
 * 加载已关联的附件列表
 * 查询条件遵循 docs/admin/附件机制.md 附件关联约定
 */
async function loadList() {
  if (!props.fieldModelName || !props.fieldModelId) {
    files.value = []
    return
  }
  loading.value = true
  try {
    const res = await attachmentApi.list(
      buildListQuery(
        [
          eq('fieldModelName', props.fieldModelName),
          eq('fieldModelId', props.fieldModelId),
          eq('fieldKey', props.fieldKey),
          eq('fieldDeleteFlag', 0)
        ],
        { pageNum: 1, pageSize: 999, needPage: false }
      )
    )
    files.value = res.list || []
  } catch {
    // 错误提示由响应拦截器统一处理
    files.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 上传前校验：文件类型与大小
 */
function beforeUpload(file) {
  // 编辑态 + 业务主键就绪才允许上传
  if (!canUpload.value) {
    ElMessage.warning('请先保存主体后再上传附件')
    return false
  }
  if (!validateFileType(file, props.fileType)) {
    ElMessage.error(buildFileTypeTip(props.fileType))
    return false
  }
  if (!validateFileSize(file, props.maxSize)) {
    // 超出限制时至少以 KB 计算进行提示
    ElMessage.error(buildFileSizeTip(file, props.maxSize))
    return false
  }
  // uploadValidate：确认上传到触发后端接口前的自定义校验，必须返回 true 才继续
  if (typeof props.uploadValidate === 'function' && props.uploadValidate(file) !== true) {
    return false
  }
  return true
}

/**
 * 自定义上传
 */
async function handleUpload({ file }) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fieldModelName', props.fieldModelName)
    formData.append('fieldModelId', props.fieldModelId)
    formData.append('fieldKey', props.fieldKey)
    const res = await attachmentApi.upload(formData)
    const result = { fieldId: res.data, file }
    ElMessage.success('上传成功')
    emit('upload', result)
    if (typeof props.uploadCallback === 'function') {
      props.uploadCallback(result)
    }
    // 重新拉取列表以反映最新状态
    await loadList()
  } finally {
    uploading.value = false
  }
}

/**
 * 下载附件
 */
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

/**
 * 删除附件（逻辑删除）
 */
async function onRemove(index, row) {
  try {
    await ElMessageBox.confirm(`确定删除附件「${row.fieldName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  removingId.value = row.fieldId
  try {
    await attachmentApi.remove(row.fieldId)
    const result = { fieldId: row.fieldId, index }
    ElMessage.success('删除成功')
    emit('delete', result)
    if (typeof props.deleteCallback === 'function') {
      props.deleteCallback(result)
    }
    await loadList()
  } finally {
    removingId.value = ''
  }
}

defineExpose({ loadList, files })
</script>

<style lang="scss" scoped>
.attachment-uploader {
  &__upload {
    :deep(.el-upload-list) {
      display: none;
    }
  }

  &__tip {
    margin-top: var(--spacing-xs);
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    line-height: var(--line-height-base);
  }

  &__tip-warning {
    color: var(--color-warning);
  }

  &__list {
    margin-top: var(--spacing-sm);
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  &__item {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    padding: var(--spacing-xs) var(--spacing-sm);
    border: 1px solid var(--color-border-lighter);
    border-radius: var(--radius-base);
    background-color: var(--color-bg-primary);
    transition: border-color var(--transition-fast);

    &:hover {
      border-color: var(--color-primary-light-5);
    }
  }

  &__item-icon {
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }

  &__item-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--color-primary);
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  &__item-size {
    flex-shrink: 0;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
  }

  &__item-remove {
    flex-shrink: 0;
  }

  &__empty {
    padding: var(--spacing-md);
    text-align: center;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    border: 1px dashed var(--color-border-light);
    border-radius: var(--radius-base);
  }

  &.is-view {
    &__list {
      margin-top: 0;
    }
  }
}
</style>

