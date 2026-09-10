<template>
  <div class="avatar-uploader" :class="{ 'is-view': isView }">
    <!-- 头像预览 -->
    <div class="avatar-uploader__preview">
      <el-avatar v-if="avatarUrl" :size="size" :src="avatarUrl" />
      <el-avatar v-else :size="size">
        <el-icon :size="size / 2"><User /></el-icon>
      </el-avatar>
    </div>

    <!-- 上传控件与操作（编辑态） -->
    <div v-if="!isView" class="avatar-uploader__actions">
      <el-upload
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        :http-request="handleAvatarUpload"
        accept=".jpg,image/jpeg"
        :disabled="uploading || !canUpload"
      >
        <el-button :loading="uploading" :disabled="!canUpload">选择头像</el-button>
      </el-upload>
      <el-button v-if="hasAvatar" link type="danger" @click="onRemove">移除</el-button>
      <p class="avatar-uploader__tip">
        仅支持 .jpg 格式，文件大小不超过 {{ limitLabel }}
        <span v-if="!canUpload" class="avatar-uploader__tip-warning">（请先保存主体后再上传头像）</span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { attachmentApi } from '@/api/attachment'
import { buildListQuery, eq } from '@/utils/query'
import { formatFileSize, validateFileSize, validateFileType } from '@/utils/file'

const props = defineProps({
  /** 状态：edit 编辑 / view 查看；查看态仅展示头像，不显示上传与移除 */
  status: {
    type: String,
    default: 'edit',
    validator: (v) => ['edit', 'view'].includes(v)
  },
  /** 传递给接口的 fieldModelName（docs/admin/附件机制.md 附件关联） */
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
  /** 头像尺寸（px） */
  size: {
    type: Number,
    default: 80
  }
})

const emit = defineEmits(['upload', 'delete'])

/** 头像固定限制：.jpg 且不超过 1MB */
const AVATAR_ACCEPT_TYPE = '.jpg'
const AVATAR_MAX_SIZE = 1024 * 1024 // 1MB

const isView = computed(() => props.status === 'view')
const canUpload = computed(() => !!props.fieldModelName && !!props.fieldModelId)

const uploading = ref(false)
const avatarUrl = ref('')
const avatarMainId = ref('')

const hasAvatar = computed(() => !!avatarMainId.value)
const limitLabel = computed(() => formatFileSize(AVATAR_MAX_SIZE))

// 监听关联对象变化，重新加载头像
watch(
  () => [props.fieldModelName, props.fieldModelId, props.fieldKey],
  () => loadAvatar(),
  { immediate: true }
)

/**
 * 加载并回显当前关联的头像
 * 约定：field_model_name + field_model_id + field_key 三字段关联，仅取一条
 */
async function loadAvatar() {
  revokeAvatarUrl()
  avatarUrl.value = ''
  avatarMainId.value = ''
  if (!props.fieldModelName || !props.fieldModelId) return
  try {
    const res = await attachmentApi.list(
      buildListQuery(
        [
          eq('fieldModelName', props.fieldModelName),
          eq('fieldModelId', props.fieldModelId),
          eq('fieldKey', props.fieldKey),
          eq('fieldDeleteFlag', 0)
        ],
        { pageNum: 1, pageSize: 1, needPage: false }
      )
    )
    const list = res.list || []
    if (list.length === 0) return
    const att = list[0]
    avatarMainId.value = att.fieldId || ''
    // 通过下载接口获取头像二进制生成预览
    try {
      const blob = await attachmentApi.download(att.fieldId)
      avatarUrl.value = URL.createObjectURL(new Blob([blob]))
    } catch {
      // 下载失败仅清空预览，不阻断流程
      avatarUrl.value = ''
    }
  } catch {
    // 错误提示由响应拦截器统一处理
  }
}

/**
 * 上传前校验：仅允许 .jpg 且不超过 1MB
 */
function beforeAvatarUpload(file) {
  // 编辑态 + 业务主键就绪才允许上传
  if (!canUpload.value) {
    ElMessage.warning('请先保存主体后再上传头像')
    return false
  }
  // 校验扩展名（.jpg）
  if (!validateFileType(file, AVATAR_ACCEPT_TYPE)) {
    ElMessage.error('头像仅支持 .jpg 格式')
    return false
  }
  // 校验文件大小
  if (!validateFileSize(file, AVATAR_MAX_SIZE)) {
    // 超出限制时至少以 KB 计算进行提示
    ElMessage.error(`文件大小不能超过 ${limitLabel.value}（当前 ${formatFileSize(file.size)}）`)
    return false
  }
  return true
}

/**
 * 自定义上传：上传至附件服务并建立与当前对象的关联
 * 保证一个对象仅一个头像：上传新头像前先逻辑删除旧附件
 */
async function handleAvatarUpload({ file }) {
  uploading.value = true
  try {
    // 若已存在旧头像，先逻辑删除旧附件（保证一对一）
    if (avatarMainId.value) {
      try {
        await attachmentApi.remove(avatarMainId.value)
      } catch {
        // 旧附件删除失败不阻断新头像上传
      }
    }
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fieldModelName', props.fieldModelName)
    formData.append('fieldModelId', props.fieldModelId)
    formData.append('fieldKey', props.fieldKey)
    const mainId = await attachmentApi.upload(formData)
    avatarMainId.value = mainId
    // 立即生成本地预览
    revokeAvatarUrl()
    avatarUrl.value = URL.createObjectURL(file)
    ElMessage.success('头像上传成功')
    emit('upload', { fieldId: mainId, file })
  } finally {
    uploading.value = false
  }
}

/**
 * 移除头像（逻辑删除已上传的附件）
 */
function onRemove() {
  if (avatarMainId.value) {
    attachmentApi
      .remove(avatarMainId.value)
      .then(() => {
        emit('delete', { fieldId: avatarMainId.value })
      })
      .catch(() => {
        // 删除失败提示由响应拦截器统一处理
      })
  }
  revokeAvatarUrl()
  avatarUrl.value = ''
  avatarMainId.value = ''
}

function revokeAvatarUrl() {
  if (avatarUrl.value && avatarUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarUrl.value)
  }
}

defineExpose({ loadAvatar, avatarMainId })
</script>

<style lang="scss" scoped>
.avatar-uploader {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-lg);

  &__preview {
    flex-shrink: 0;
  }

  &__actions {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  &__tip {
    margin: 0;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    line-height: var(--line-height-base);
  }

  &__tip-warning {
    color: var(--color-warning);
  }
}
</style>
