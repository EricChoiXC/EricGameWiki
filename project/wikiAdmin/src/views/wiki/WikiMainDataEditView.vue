<template>
  <div class="wiki-data-edit">
    <el-card shadow="never">
      <template #header>
        <div class="wiki-data-edit__header">
          <span>{{ isEdit ? '编辑数据项' : '新建数据项' }}</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="名称" prop="fieldName">
              <el-input v-model="form.fieldName" placeholder="请输入名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称" prop="fieldDataName">
              <el-input
                v-model="form.fieldDataName"
                placeholder="小写英文和数字"
                maxlength="15"
                :disabled="isEdit"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="数据项类型" prop="fieldDataType">
          <el-select
            v-model="form.fieldDataType"
            placeholder="请选择数据项类型"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option
              v-for="opt in dataTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <!-- 类型说明 -->
        <el-form-item v-if="form.fieldDataType" label=" ">
          <div class="wiki-data-edit__hint">
            <span v-if="form.fieldDataType === 'data'">图鉴类：如道具、人物、技能等图鉴信息</span>
            <span v-else-if="form.fieldDataType === 'join'">关联项：图鉴间关联信息，或需明细表记录的数据信息</span>
            <span v-else-if="form.fieldDataType === 'doc'">文档类：流程攻略等非图鉴型信息</span>
          </div>
        </el-form-item>

        <!-- 数据项明细表（仅图鉴类/关联项显示） -->
        <el-form-item
          v-if="form.fieldDataType === 'data' || form.fieldDataType === 'join'"
          label="数据项明细"
        >
          <el-table :data="detailRows" border style="width: 100%">
            <el-table-column label="名称" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="请输入名称" :disabled="row._fixed" />
              </template>
            </el-table-column>
            <el-table-column label="简称" min-width="140">
              <template #default="{ row }">
                <el-input
                  v-model="row.dataName"
                  placeholder="英文/数字/字符"
                  :disabled="row._fixed"
                />
              </template>
            </el-table-column>
            <el-table-column label="数据类型" width="150">
              <template #default="{ row }">
                <el-select v-model="row.type" placeholder="请选择" :disabled="row._fixed">
                  <el-option
                    v-for="opt in detailTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="枚举项/关联项" min-width="200">
              <template #default="{ row }">
                <!-- enum 显示枚举项填写 -->
                <el-input
                  v-if="row.type === 'enum'"
                  v-model="row.enumsText"
                  placeholder="枚举项，逗号分隔"
                />
                <!-- join 显示关联项选择 -->
                <el-select
                  v-else-if="row.type === 'join'"
                  v-model="row.join"
                  placeholder="请选择关联数据项"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="src in joinSources"
                    :key="src.fieldId"
                    :label="`${src.fieldName}（${src.fieldDataName}）`"
                    :value="src.fieldId"
                  />
                </el-select>
                <span v-else class="wiki-data-edit__placeholder">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row, $index }">
                <el-button
                  v-if="!row._fixed"
                  link
                  type="danger"
                  size="small"
                  @click="removeDetail($index)"
                >
                  删除
                </el-button>
                <span v-else class="wiki-data-edit__fixed">固定</span>
              </template>
            </el-table-column>
          </el-table>
          <el-button
            v-if="form.fieldDataType === 'join'"
            class="wiki-data-edit__add-detail"
            :icon="Plus"
            @click="addDetail"
          >
            新增明细
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { wikiMainDataApi } from '@/api/wiki'

const route = useRoute()
const router = useRouter()

const fieldMainId = computed(() => route.params.mainId || '')
const fieldId = computed(() => route.params.id || '')
const isEdit = computed(() => !!fieldId.value)

const formRef = ref(null)
const saving = ref(false)

/** 数据项类型可选项 */
const dataTypeOptions = [
  { value: 'data', label: '图鉴类' },
  { value: 'join', label: '关联项' },
  { value: 'doc', label: '文档类' }
]

/** 明细行数据类型可选项 */
const detailTypeOptions = [
  { value: 'text', label: '文本' },
  { value: 'blob', label: '富文本' },
  { value: 'number', label: '数字' },
  { value: 'date', label: '日期' },
  { value: 'datetime', label: '日期时间' },
  { value: 'time', label: '时间' },
  { value: 'boolean', label: '布尔' },
  { value: 'enum', label: '枚举' },
  { value: 'attachment', label: '附件' },
  { value: 'join', label: '关联数据' }
]

const form = reactive({
  fieldId: '',
  fieldMainId: '',
  fieldName: '',
  fieldDataName: '',
  fieldDataType: '',
  fieldDataJson: []
})

/**
 * 明细行列表（含 _fixed 标识图鉴类固定列）。
 * 图鉴类固定显示 name/code 两行，不可修改。
 */
const detailRows = ref([])

/** 关联项选择源（项目已有图鉴类/文档类数据项） */
const joinSources = ref([])

const rules = {
  fieldName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  fieldDataName: [
    { required: true, message: '请输入简称', trigger: 'blur' },
    {
      pattern: /^[a-z0-9]+$/,
      message: '简称仅支持小写英文和数字',
      trigger: 'blur'
    }
  ],
  fieldDataType: [{ required: true, message: '请选择数据项类型', trigger: 'change' }]
}

/**
 * 图鉴类固定列：name/code（text 类型，不可修改）。
 */
function buildFixedDetails() {
  return [
    { name: '名称', dataName: 'name', type: 'text', _fixed: true },
    { name: '编号', dataName: 'code', type: 'text', _fixed: true }
  ]
}

/**
 * 类型切换时联动明细表：
 *  - 图鉴类：初始化固定列 name/code
 *  - 关联项：清空明细（无固定列）
 *  - 文档类：清空明细
 */
watch(() => form.fieldDataType, (type) => {
  if (type === 'data') {
    detailRows.value = buildFixedDetails()
  } else {
    detailRows.value = []
  }
})

function addDetail() {
  detailRows.value.push({ name: '', dataName: '', type: 'text', enumsText: '', join: '' })
}

function removeDetail(index) {
  detailRows.value.splice(index, 1)
}

/**
 * 将明细行列表转换为提交格式：固定列不提交（后端自动补充），enum 的 enumsText 拆为数组。
 */
function buildSubmitDetails() {
  const result = []
  for (const row of detailRows.value) {
    if (row._fixed) continue
    const detail = {
      name: row.name,
      dataName: row.dataName,
      type: row.type
    }
    if (row.type === 'enum' && row.enumsText) {
      detail.enums = row.enumsText.split(',').map((s) => s.trim()).filter(Boolean)
    }
    if (row.type === 'join') {
      detail.join = row.join || ''
    }
    result.push(detail)
  }
  return result
}

/**
 * 加载初始化数据（关联项选择源）。
 */
async function loadInit() {
  const res = await wikiMainDataApi.init(fieldMainId.value)
  joinSources.value = res.data?.joinSources || []
}

/**
 * 编辑模式加载数据项详情。
 */
async function loadDetail() {
  if (!isEdit.value) return
  const res = await wikiMainDataApi.load(fieldId.value)
  const data = res.data || {}
  Object.assign(form, {
    fieldId: data.fieldId || '',
    fieldMainId: data.fieldMainId || fieldMainId.value,
    fieldName: data.fieldName || '',
    fieldDataName: data.fieldDataName || '',
    fieldDataType: data.fieldDataType || '',
    fieldDataJson: data.fieldDataJson || []
  })
  // 还原明细行：图鉴类前两行为固定列（后端已补充），编辑时仅展示，不可修改
  const rows = (data.fieldDataJson || []).map((d) => {
    const row = {
      name: d.name,
      dataName: d.dataName,
      type: d.type,
      enumsText: Array.isArray(d.enums) ? d.enums.join(',') : '',
      join: d.join || '',
      _fixed: false
    }
    // 图鉴类固定列 name/code 标记为固定
    if (form.fieldDataType === 'data' && (d.dataName === 'name' || d.dataName === 'code')) {
      row._fixed = true
    }
    return row
  })
  detailRows.value = rows
}

async function onSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 校验明细行（图鉴类/关联项）
  if (form.fieldDataType === 'data' || form.fieldDataType === 'join') {
    for (const row of detailRows.value) {
      if (!row._fixed && (!row.name || !row.dataName || !row.type)) {
        ElMessage.warning('请完整填写数据项明细行的名称、简称与数据类型')
        return
      }
      if (row.type === 'join' && !row.join) {
        ElMessage.warning('关联数据类型的明细行必须选择关联数据项')
        return
      }
    }
  }

  saving.value = true
  try {
    const payload = {
      data: {
        ...form,
        fieldMainId: fieldMainId.value,
        fieldDataJson: buildSubmitDetails()
      }
    }
    if (isEdit.value) {
      await wikiMainDataApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await wikiMainDataApi.save(payload)
      ElMessage.success('新建成功')
    }
    router.push(`/admin/wiki/${fieldMainId.value}/data`)
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.push(`/admin/wiki/${fieldMainId.value}/data`)
}

onMounted(async () => {
  form.fieldMainId = fieldMainId.value
  await loadInit()
  await loadDetail()
})
</script>

<style lang="scss" scoped>
.wiki-data-edit {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__hint {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  &__placeholder {
    color: var(--el-text-color-placeholder);
  }

  &__fixed {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  &__add-detail {
    margin-top: 8px;
    width: 100%;
    border-style: dashed;
  }
}
</style>
