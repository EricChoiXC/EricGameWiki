<template>
  <div class="user-edit">
    <el-card shadow="never">
      <template #header>
        <div class="user-edit__header">
          <span>{{ isEdit ? '编辑用户' : '新建用户' }}</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 560px"
      >
        <el-form-item label="头像">
          <div class="user-edit__avatar">
            <!-- 头像预览 -->
            <div class="user-edit__avatar-preview">
              <el-avatar
                v-if="avatarUrl"
                :size="80"
                :src="avatarUrl"
              />
              <el-avatar v-else :size="80">
                <el-icon><User /></el-icon>
              </el-avatar>
            </div>
            <!-- 上传控件 -->
            <div class="user-edit__avatar-actions">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
                accept=".jpg,image/jpeg"
              >
                <el-button :loading="avatarUploading">选择头像</el-button>
              </el-upload>
              <el-button
                v-if="avatarUrl"
                link
                type="danger"
                @click="onRemoveAvatar"
              >
                移除
              </el-button>
              <p class="user-edit__avatar-tip">
                仅支持 .jpg 格式，文件大小不超过 1MB；非必填
              </p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="名称" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="请输入名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="登录名" prop="fieldLoginName">
          <el-input
            v-model="form.fieldLoginName"
            placeholder="请输入登录名"
            maxlength="200"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="fieldPhone">
          <el-input v-model="form.fieldPhone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱" prop="fieldEmail">
          <el-input v-model="form.fieldEmail" placeholder="请输入邮箱" maxlength="200" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          <el-button @click="onBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User } from '@element-plus/icons-vue'
import { userApi } from '@/api/org'
import { idApi } from '@/api/system'
import { attachmentApi } from '@/api/attachment'
import { buildListQuery, eq } from '@/utils/query'

const route = useRoute()
const router = useRouter()

const fieldId = computed(() => route.params.id || '')
const isEdit = computed(() => !!fieldId.value)

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  fieldId: '',
  fieldName: '',
  fieldLoginName: '',
  fieldPhone: '',
  fieldEmail: ''
})

// 头像相关
// 附件关联约定（docs/admin/附件机制.md）：field_model_name="user", field_model_id=用户id, field_key="avatar"
const AVATAR_MODEL_NAME = 'user'
const AVATAR_FIELD_KEY = 'avatar'
const AVATAR_ACCEPT_TYPE = '.jpg'
const AVATAR_MAX_SIZE = 1024 * 1024 // 1MB

const avatarUploading = ref(false)
// 当前已关联的头像附件 mainId（删除/换图时需逻辑删除旧附件）
const avatarMainId = ref('')
// 头像预览地址
const avatarUrl = ref('')

// 登录名不可为纯数字（手机号格式）或包含 '@'（邮箱格式）
const validateLoginName = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入登录名'))
    return
  }
  if (/^\d+$/.test(value)) {
    callback(new Error('登录名不可为纯数字（手机号格式）'))
    return
  }
  if (value.includes('@')) {
    callback(new Error('登录名不可包含 @（邮箱格式）'))
    return
  }
  callback()
}

const validatePhone = (_rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^1\d{10}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
    return
  }
  callback()
}

const validateEmail = (_rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(value)) {
    callback(new Error('请输入正确的邮箱'))
    return
  }
  callback()
}

const rules = {
  fieldName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  fieldLoginName: [{ required: true, validator: validateLoginName, trigger: 'blur' }],
  fieldPhone: [{ validator: validatePhone, trigger: 'blur' }],
  fieldEmail: [{ validator: validateEmail, trigger: 'blur' }]
}

// 头像上传前校验：仅允许 .jpg，且不超过 1MB
function beforeAvatarUpload(file) {
  // 校验扩展名（.jpg）
  const fileName = file.name || ''
  if (!fileName.toLowerCase().endsWith(AVATAR_ACCEPT_TYPE)) {
    ElMessage.error('头像仅支持 .jpg 格式')
    return false
  }
  // 校验文件大小
  if (file.size > AVATAR_MAX_SIZE) {
    ElMessage.error('头像文件大小不能超过 1MB')
    return false
  }
  return true
}

// 自定义上传：将头像上传至附件服务并建立与当前用户的关联
async function handleAvatarUpload({ file }) {
  // 新建场景下需先取得用户主键，再上传附件（附件以 field_model_id 关联用户）
  if (!isEdit.value && !form.fieldId) {
    try {
      const id = await idApi.init()
      form.fieldId = id || ''
    } catch {
      return
    }
  }
  if (!form.fieldId) {
    ElMessage.error('无法获取用户标识，请重试')
    return
  }
  avatarUploading.value = true
  try {
    // 若已存在旧头像，先逻辑删除旧附件（保证一个用户仅一个头像）
    if (avatarMainId.value) {
      await attachmentApi.remove(avatarMainId.value)
    }
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fieldModelName', AVATAR_MODEL_NAME)
    formData.append('fieldModelId', form.fieldId)
    formData.append('fieldKey', AVATAR_FIELD_KEY)
    const mainId = await attachmentApi.upload(formData)
    avatarMainId.value = mainId
    // 立即生成本地预览
    revokeAvatarUrl()
    avatarUrl.value = URL.createObjectURL(file)
    ElMessage.success('头像上传成功')
  } finally {
    avatarUploading.value = false
  }
}

function onRemoveAvatar() {
  if (avatarMainId.value) {
    // 逻辑删除已上传的头像附件
    attachmentApi.remove(avatarMainId.value).catch(() => {
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

// 加载并回显当前用户的头像（编辑场景）
async function loadAvatar() {
  const res = await attachmentApi.list(
    buildListQuery(
      [
        eq('fieldModelName', AVATAR_MODEL_NAME),
        eq('fieldModelId', form.fieldId),
        eq('fieldKey', AVATAR_FIELD_KEY),
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
}

async function loadDetail() {
  if (!isEdit.value) return
  const res = await userApi.load(fieldId.value)
  const data = res.data || {}
  Object.assign(form, {
    fieldId: data.fieldId || '',
    fieldName: data.fieldName || '',
    fieldLoginName: data.fieldLoginName || '',
    fieldPhone: data.fieldPhone || '',
    fieldEmail: data.fieldEmail || ''
  })
  // 加载并回显头像
  await loadAvatar()
}

async function onSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    // 登录名、手机号、邮箱唯一性由后端校验（409 冲突），前端不再重复校验
    if (isEdit.value) {
      await userApi.update({ data: { ...form } })
      ElMessage.success('编辑成功')
    } else {
      await userApi.save({ data: { ...form } })
      ElMessage.success('新建成功')
    }
    router.push('/admin/org/user')
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.push('/admin/org/user')
}

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.user-edit {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__avatar {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-lg);
  }

  &__avatar-preview {
    flex-shrink: 0;
  }

  &__avatar-actions {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  &__avatar-tip {
    margin: 0;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    line-height: var(--line-height-base);
  }
}
</style>
