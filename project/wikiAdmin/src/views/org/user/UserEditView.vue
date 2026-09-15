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
          <AvatarUploader
            field-model-name="user"
            :field-model-id="form.fieldId"
            field-key="avatar"
          />
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
import { ArrowLeft } from '@element-plus/icons-vue'
import AvatarUploader from '@/components/AvatarUploader.vue'
import { userApi } from '@/api/org'
import { idApi } from '@/api/system'

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

onMounted(async () => {
  if (isEdit.value) {
    await loadDetail()
  } else {
    // 新建场景：父组件负责预生成主键，供头像组件以 field_model_id 关联上传
    try {
      form.fieldId = (await idApi.init())?.data || ''
    } catch {
      // 预生成失败不阻塞，头像上传时由组件提示
    }
  }
})
</script>

<style lang="scss" scoped>
.user-edit {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }
}
</style>
