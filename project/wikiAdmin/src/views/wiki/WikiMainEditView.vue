<template>
  <div class="wiki-main-edit">
    <el-card shadow="never">
      <template #header>
        <div class="wiki-main-edit__header">
          <span>{{ isEdit ? '编辑项目' : '新建项目' }}</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="名称" prop="fieldName">
              <el-input v-model="form.fieldName" placeholder="请输入名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称" prop="fieldSimpleName">
              <el-input
                v-model="form.fieldSimpleName"
                placeholder="小写英文和数字"
                maxlength="15"
                :disabled="isEdit"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="英文名称">
              <el-input v-model="form.fieldEnName" placeholder="请输入英文名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日文名称">
              <el-input v-model="form.fieldJpName" placeholder="请输入日文名称" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="发行日期">
              <DateTimePicker
                v-model="form.fieldPublishDate"
                field-name="发行日期"
                calendar-type="datetime"
                :datetime-format="'yyyy-MM-dd\'T\'HH:mm:ss'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开启状态">
              <el-switch
                v-model="form.fieldStatus"
                :active-value="1"
                :inactive-value="0"
                active-text="开启"
                inactive-text="停用"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 维护人员（普通选择器多选） -->
        <el-form-item label="维护人员">
          <SimpleSelector
            v-model:field-id="managerIdsText"
            v-model:field-name="managerNamesText"
            url="/org/user/list"
            :filter="managerFilter"
            :multi="true"
            :septarator="';'"
          />
        </el-form-item>

        <!-- 拓展信息明细行（动态行） -->
        <el-form-item label="拓展信息">
          <el-table :data="form.fieldExtend" border style="width: 100%">
            <el-table-column label="名称" min-width="160">
              <template #default="{ row, $index }">
                <el-input v-model="row.name" placeholder="请输入名称" />
                <span class="sr-only">{{ $index }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="160">
              <template #default="{ row }">
                <el-select v-model="row.type" placeholder="请选择类型">
                  <el-option
                    v-for="opt in extendTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="值" min-width="200">
              <template #default="{ row }">
                <el-input v-if="row.type === 'text'" v-model="row.value" placeholder="请输入值" />
                <el-input-number
                  v-else-if="row.type === 'number'"
                  v-model="row.value"
                  :controls="false"
                  style="width: 100%"
                />
                <DateTimePicker v-else-if="row.type === 'date'" v-model="row.value" calendar-type="date" />
                <DateTimePicker
                  v-else-if="row.type === 'datetime'"
                  v-model="row.value"
                  calendar-type="datetime"
                  :datetime-format="'yyyy-MM-dd\'T\'HH:mm:ss'"
                />
                <DateTimePicker v-else-if="row.type === 'time'" v-model="row.value" calendar-type="time" />
                <el-switch
                  v-else-if="row.type === 'boolean'"
                  v-model="row.value"
                  :active-value="true"
                  :inactive-value="false"
                />
                <span v-else class="wiki-main-edit__hint">请先选择类型</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeExtend($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button
            class="wiki-main-edit__add-extend"
            :icon="Plus"
            @click="addExtend"
          >
            新增拓展信息
          </el-button>
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
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { wikiMainApi } from '@/api/wiki'
import SimpleSelector from '@/components/SimpleSelector.vue'
import DateTimePicker from '@/components/DateTimePicker.vue'

const route = useRoute()
const router = useRouter()

const fieldId = computed(() => route.params.id || '')
const isEdit = computed(() => !!fieldId.value)

const formRef = ref(null)
const saving = ref(false)

/** 拓展信息类型可选项 */
const extendTypeOptions = [
  { value: 'text', label: '文本' },
  { value: 'number', label: '数字' },
  { value: 'date', label: '日期' },
  { value: 'datetime', label: '日期时间' },
  { value: 'time', label: '时间' },
  { value: 'boolean', label: '是否' }
]

const form = reactive({
  fieldId: '',
  fieldName: '',
  fieldEnName: '',
  fieldJpName: '',
  fieldSimpleName: '',
  fieldPublishDate: null,
  fieldStatus: 1,
  fieldManagers: [],
  fieldExtend: []
})

const rules = {
  fieldName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  fieldSimpleName: [
    { required: true, message: '请输入简称', trigger: 'blur' },
    {
      pattern: /^[a-z0-9]+$/,
      message: '简称仅支持小写英文和数字',
      trigger: 'blur'
    }
  ]
}

/** 维护人员候选用户列表（来自 /wiki/init） */
const managerOptions = ref([])

/** 维护人员选择：SimpleSelector 多选绑定值（';' 分隔），保存时还原为 id 数组 */
const managerIdsText = ref('')
const managerNamesText = ref('')
const managerFilter = [
  { text: '名称', name: 'fieldName' },
  { text: '登录名', name: 'fieldLoginName' }
]

function addExtend() {
  form.fieldExtend.push({ name: '', type: 'text', value: '' })
}

function removeExtend(index) {
  form.fieldExtend.splice(index, 1)
}

async function loadInit() {
  const res = await wikiMainApi.init()
  managerOptions.value = res.data?.managers || []
}

async function loadDetail() {
  if (!isEdit.value) return
  const res = await wikiMainApi.load(fieldId.value)
  const data = res.data || {}
  Object.assign(form, {
    fieldId: data.fieldId || '',
    fieldName: data.fieldName || '',
    fieldEnName: data.fieldEnName || '',
    fieldJpName: data.fieldJpName || '',
    fieldSimpleName: data.fieldSimpleName || '',
    fieldPublishDate: data.fieldPublishDate || null,
    fieldStatus: data.fieldStatus ?? 1,
    fieldManagers: data.fieldManagers || [],
    fieldExtend: data.fieldExtend || []
  })
  // 维护人员：按 init 返回的用户列表还原名称，供 SimpleSelector 展示
  const pairs = (data.fieldManagers || [])
    .map((id) => {
      const hit = managerOptions.value.find((user) => user.fieldId === id)
      return [id, hit ? hit.fieldName : '']
    })
    .filter(([, name]) => name)
  managerIdsText.value = pairs.map(([id]) => id).join(';')
  managerNamesText.value = pairs.map(([, name]) => name).join(';')
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
        fieldManagers: managerIdsText.value ? managerIdsText.value.split(';') : []
      }
    }
    if (isEdit.value) {
      await wikiMainApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await wikiMainApi.save(payload)
      ElMessage.success('新建成功')
    }
    router.push('/admin/wiki')
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.push('/admin/wiki')
}

onMounted(async () => {
  await loadInit()
  await loadDetail()
})
</script>

<style lang="scss" scoped>
.wiki-main-edit {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__hint {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__add-extend {
    margin-top: 8px;
    width: 100%;
    border-style: dashed;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
