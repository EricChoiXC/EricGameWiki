<template>
  <el-dialog
    :model-value="modelValue"
    :title="`导入数据${title ? '：' + title : ''}`"
    width="640px"
    @update:model-value="onUpdateVisible"
    @open="onOpen"
  >
    <div class="wiki-import">
      <!-- 模板下载 -->
      <div class="wiki-import__section">
        <div class="wiki-import__section-label">第一步：下载导入模板</div>
        <el-button :icon="Download" :loading="templating" @click="onDownloadTemplate">
          下载模板
        </el-button>
        <div class="wiki-import__section-tip">
          模板首行为标题行，显示该数据项的所有数据明细；关联数据显示「${关联数据}编号」；不列出附件类明细。
        </div>
      </div>

      <!-- 文件上传 -->
      <div class="wiki-import__section">
        <div class="wiki-import__section-label">第二步：上传 xlsx 导入文件</div>
        <el-upload
          :show-file-list="false"
          :accept="'.xlsx'"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          :disabled="uploading"
        >
          <el-button type="primary" :icon="Upload" :loading="uploading">选择文件</el-button>
        </el-upload>
        <div v-if="attachment" class="wiki-import__file">
          <el-icon class="wiki-import__file-icon"><Document /></el-icon>
          <span class="wiki-import__file-name">{{ attachment.fileName }}</span>
        </div>
        <div v-else class="wiki-import__section-tip">请选择需要导入的 .xlsx 文件（仅支持 xlsx 格式）。</div>
      </div>

      <!-- 跳过开关 -->
      <div class="wiki-import__section">
        <div class="wiki-import__section-label">第三步：导入设定</div>
        <el-checkbox v-model="skipFail">失败数据跳过</el-checkbox>
        <el-checkbox v-model="skipError">异常数据跳过</el-checkbox>
      </div>

      <!-- 结果展示 -->
      <div v-if="result" class="wiki-import__result">
        <div class="wiki-import__result-summary">
          共 {{ result.totalCount }} 条，成功 {{ result.successCount }} 条，跳过 {{ result.skipCount }} 条
        </div>
        <el-table v-if="result.failDetails && result.failDetails.length > 0" :data="result.failDetails" border size="small">
          <el-table-column prop="row" label="行号" width="80" align="center" />
          <el-table-column prop="reason" label="失败原因" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
    </div>

    <template #footer>
      <el-button @click="onCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!attachment" @click="onSubmit">
        提交
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, Upload } from '@element-plus/icons-vue'
import { wikiDataApi } from '@/api/wiki'
import { attachmentApi } from '@/api/attachment'
import { idApi } from '@/api/system'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  fieldDataId: {
    type: String,
    default: ''
  },
  /** 项目简称（附件 field_model_name = ${项目简称}::${数据项简称}） */
  simpleName: {
    type: String,
    default: ''
  },
  /** 数据项简称 */
  dataName: {
    type: String,
    default: ''
  },
  /** 展示标题（数据项名称） */
  title: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'imported'])

const templating = ref(false)
const uploading = ref(false)
const submitting = ref(false)
const skipFail = ref(false)
const skipError = ref(false)

/** 已上传附件信息：{ attachmentId, fileName } */
const attachment = ref(null)

/** 导入结果：{ totalCount, successCount, skipCount, failDetails } */
const result = ref(null)

/** 附件上传用临时 modelId（预生成主键） */
const tempModelId = ref('')

watch(
  () => props.modelValue,
  (val) => {
    if (val) reset()
  }
)

function onUpdateVisible(val) {
  emit('update:modelValue', val)
}

function onOpen() {
  reset()
}

function reset() {
  attachment.value = null
  result.value = null
  skipFail.value = false
  skipError.value = false
  preGenerateModelId()
}

async function preGenerateModelId() {
  try {
    const res = await idApi.init()
    tempModelId.value = res.data || ''
  } catch {
    tempModelId.value = ''
  }
}

function modelName() {
  if (props.simpleName && props.dataName) {
    return `${props.simpleName}::${props.dataName}`
  }
  return 'wiki-import'
}

/**
 * 模板下载（API-W207）。
 */
async function onDownloadTemplate() {
  if (!props.fieldDataId) return
  templating.value = true
  try {
    const res = await wikiDataApi.template(props.fieldDataId)
    const name = `${props.dataName || props.title || 'wiki'}_导入模板.xlsx`
    saveBlob(res, name)
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    templating.value = false
  }
}

/**
 * 上传前校验：仅允许 xlsx 格式。
 */
function beforeUpload(file) {
  const name = (file?.name || '').toLowerCase()
  if (!name.endsWith('.xlsx')) {
    ElMessage.error('仅支持 .xlsx 格式文件')
    return false
  }
  return true
}

/**
 * 自定义上传：经附件组件上传 xlsx，获得附件 id（docs/admin/wiki/wiki技术方案.md 6.7）。
 */
async function handleUpload({ file }) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fieldModelName', modelName())
    formData.append('fieldModelId', tempModelId.value)
    formData.append('fieldKey', 'import')
    const res = await attachmentApi.upload(formData)
    attachment.value = { attachmentId: res.data, fileName: file.name }
    ElMessage.success('上传成功')
  } finally {
    uploading.value = false
  }
}

/**
 * 提交导入（API-W208）。
 */
async function onSubmit() {
  if (!attachment.value) return
  submitting.value = true
  try {
    const res = await wikiDataApi.import({
      data: {
        fieldDataId: props.fieldDataId,
        fieldAttachmentId: attachment.value.attachmentId,
        skipFail: skipFail.value,
        skipError: skipError.value
      }
    })
    result.value = res.data || {}
    ElMessage.success('导入完成')
    emit('imported')
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function onCancel() {
  onUpdateVisible(false)
}

function saveBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
</script>

<style lang="scss" scoped>
.wiki-import {
  &__section {
    margin-bottom: var(--spacing-md);

    &-label {
      font-weight: var(--font-weight-bold);
      margin-bottom: var(--spacing-sm);
    }

    &-tip {
      margin-top: var(--spacing-xs);
      font-size: var(--font-size-sm);
      color: var(--el-text-color-secondary);
      line-height: var(--line-height-base);
    }
  }

  &__file {
    display: flex;
    align-items: center;
    gap: var(--spacing-xs);
    margin-top: var(--spacing-sm);

    &-icon {
      color: var(--el-text-color-secondary);
    }

    &-name {
      font-size: var(--font-size-sm);
    }
  }

  &__result {
    margin-top: var(--spacing-sm);
    padding: var(--spacing-md);
    border: 1px solid var(--el-border-color);
    border-radius: var(--el-border-radius-base);

    &-summary {
      margin-bottom: var(--spacing-sm);
      font-size: var(--font-size-sm);
    }
  }
}
</style>
