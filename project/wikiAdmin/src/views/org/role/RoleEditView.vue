<template>
  <div class="role-edit">
    <el-card shadow="never">
      <template #header>
        <div class="role-edit__header">
          <span>{{ isEdit ? '编辑角色' : '新建角色' }}</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="名称" prop="fieldName">
              <el-input v-model="form.fieldName" placeholder="请输入名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编号" prop="fieldCode">
              <el-input v-model="form.fieldCode" placeholder="请输入编号" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 权限（占整行） -->
        <el-form-item label="权限" prop="permissionIds">
          <el-transfer
            v-model="permissionIds"
            :data="permissionData"
            :titles="['未分配权限', '已分配权限']"
            filterable
            filter-placeholder="请输入权限名搜索"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 用户（占整行）：普通选择器多选 -->
        <el-form-item label="用户" prop="userIds">
          <SimpleSelector
            v-model:field-id="userIdsText"
            v-model:field-name="userNamesText"
            url="/org/user/list"
            :filter="userFilter"
            :multi="true"
            :septarator="';'"
          />
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
import { permissionApi, roleApi, userApi } from '@/api/org'
import { buildListQuery } from '@/utils/query'
import SimpleSelector from '@/components/SimpleSelector.vue'

const route = useRoute()
const router = useRouter()

const fieldId = computed(() => route.params.id || '')
const isEdit = computed(() => !!fieldId.value)

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  fieldId: '',
  fieldName: '',
  fieldCode: '',
  fieldStatus: 'ENABLED'
})

const rules = {
  fieldName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  fieldCode: [{ required: true, message: '请输入编号', trigger: 'blur' }]
}

// 权限穿梭框数据源
const permissionData = ref([])

// 已选中的权限 id
const permissionIds = ref([])

// 用户选择：SimpleSelector 多选绑定值（';' 分隔），保存时还原为 id 数组
const userIdsText = ref('')
const userNamesText = ref('')
const userNameMap = new Map()
const userFilter = [
  { text: '名称', name: 'fieldName' },
  { text: '登录名', name: 'fieldLoginName' }
]

async function loadAllPermissions() {
  const res = await permissionApi.list(buildListQuery([], { needPage: false }))
  const list = res.list || []
  permissionData.value = list.map((item) => ({
    key: item.fieldId,
    label: item.fieldName || item.fieldCode,
    disabled: false
  }))
}

async function loadAllUsers() {
  const res = await userApi.list(buildListQuery([], { needPage: false }))
  const list = res.list || []
  userNameMap.clear()
  list.forEach((item) => {
    userNameMap.set(item.fieldId, item.fieldName || item.fieldLoginName || '')
  })
}

async function loadDetail() {
  if (!isEdit.value) return
  const res = await roleApi.load(fieldId.value)
  const data = res.data || {}
  Object.assign(form, {
    fieldId: data.fieldId || '',
    fieldName: data.fieldName || '',
    fieldCode: data.fieldCode || '',
    fieldStatus: data.fieldStatus || 'ENABLED'
  })
  // 已分配权限（后端 load 返回 data.roleIds 纯 id 数组）
  permissionIds.value = data.roleIds || []
  // 已分配用户：按全量用户映射还原名称，供 SimpleSelector 展示
  const pairs = (data.userIds || [])
    .map((id) => [id, userNameMap.get(id)])
    .filter(([, name]) => name)
  userIdsText.value = pairs.map(([id]) => id).join(';')
  userNamesText.value = pairs.map(([, name]) => name).join(';')
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
    const payload = {
      data: {
        ...form,
        roleIds: permissionIds.value,
        userIds: userIdsText.value ? userIdsText.value.split(';') : []
      }
    }
    if (isEdit.value) {
      await roleApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await roleApi.save(payload)
      ElMessage.success('新建成功')
    }
    router.push('/admin/org/role')
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.push('/admin/org/role')
}

onMounted(async () => {
  await Promise.all([loadAllPermissions(), loadAllUsers()])
  await loadDetail()
})
</script>

<style lang="scss" scoped>
.role-edit {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  :deep(.el-transfer) {
    display: flex;
    justify-content: center;
  }
}
</style>
