<template>
  <div class="wiki-data-edit">
    <el-card shadow="never">
      <template #header>
        <div class="wiki-data-edit__header">
          <span>{{ pageTitle }}</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <!-- 图鉴类：名称/编号 + 动态明细 -->
      <el-form
        v-if="dataType === 'data'"
        ref="formRef"
        :model="form"
        :rules="dataRules"
        label-width="120px"
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

        <el-form-item
          v-for="detail in editableDetails"
          :key="detail.dataName"
          :label="detail.name"
        >
          <WikiDynamicFieldInput
            v-model="form.fieldData[columnKey(detail)]"
            :detail="detail"
            :record-id="form.fieldId"
            :join-options="joinOptions[detail.dataName] || []"
            :simple-name="simpleName"
            :data-name="dataName"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          <el-button @click="onBack">取消</el-button>
        </el-form-item>
      </el-form>

      <!-- 关联项：动态明细 -->
      <el-form
        v-else-if="dataType === 'join'"
        ref="formRef"
        :model="form"
        :rules="joinRules"
        label-width="120px"
      >
        <el-form-item
          v-for="detail in editableDetails"
          :key="detail.dataName"
          :label="detail.name"
        >
          <WikiDynamicFieldInput
            v-model="form.fieldData[columnKey(detail)]"
            :detail="detail"
            :record-id="form.fieldId"
            :join-options="joinOptions[detail.dataName] || []"
            :simple-name="simpleName"
            :data-name="dataName"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          <el-button @click="onBack">取消</el-button>
        </el-form-item>
      </el-form>

      <!-- 文档类：标题/编号/内容（富文本整行） -->
      <el-form
        v-else-if="dataType === 'doc'"
        ref="formRef"
        :model="form"
        :rules="docRules"
        label-width="120px"
      >
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="标题" prop="fieldName">
              <el-input v-model="form.fieldName" placeholder="请输入标题" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编号" prop="fieldCode">
              <el-input v-model="form.fieldCode" placeholder="请输入编号" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="内容" prop="fieldContext">
          <RichTextEditor v-model="form.fieldContext" placeholder="请输入文档内容" />
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { wikiDataApi, wikiMainApi, wikiMainDataApi } from '@/api/wiki'
import { idApi } from '@/api/system'
import RichTextEditor from '@/components/RichTextEditor.vue'
import WikiDynamicFieldInput from './WikiDynamicFieldInput.vue'

const route = useRoute()
const router = useRouter()

const fieldMainId = computed(() => route.params.mainId || '')
const dataId = computed(() => route.params.dataId || '')
const recordId = computed(() => route.params.id || '')
const isEdit = computed(() => !!recordId.value)

const formRef = ref(null)
const saving = ref(false)

/** 数据项元数据 */
const meta = ref(null)
const dataType = computed(() => meta.value?.fieldDataType || '')
const dataName = computed(() => meta.value?.fieldDataName || '')
const dataTitle = computed(() => meta.value?.fieldName || '')

/** 项目简称（附件 field_model_name = ${项目简称}::${数据项简称}） */
const simpleName = ref('')

/** 关联项选择控件下拉源：dataName → [{ label: fieldName, value: fieldId }] */
const joinOptions = ref({})

const form = ref({
  fieldId: '',
  fieldDataId: '',
  fieldName: '',
  fieldCode: '',
  fieldContext: '',
  fieldData: {}
})

const pageTitle = computed(() => `${isEdit.value ? '编辑' : '新建'}${dataTitle.value || ''}数据`)

/** 动态明细行（排除图鉴类固定列 name/code） */
const editableDetails = computed(() =>
  (meta.value?.fieldDataJson || []).filter(
    (d) => d.dataName !== 'name' && d.dataName !== 'code'
  )
)

const dataRules = {
  fieldName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  fieldCode: [{ required: true, message: '请输入编号', trigger: 'blur' }]
}

const docRules = {
  fieldName: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  fieldCode: [{ required: true, message: '请输入编号', trigger: 'blur' }]
}

const joinRules = {}

/** 动态列在 fieldData 中的 key（小驼峰）：join 类型带 Id 后缀 */
function columnKey(detail) {
  const prefix = 'field' + upperFirst(detail.dataName)
  return detail.type === 'join' ? prefix + 'Id' : prefix
}

function upperFirst(str) {
  if (!str) return str
  return str.charAt(0).toUpperCase() + str.slice(1)
}

/**
 * 加载初始化数据：数据项元数据 + 项目简称 + 关联项下拉源 + 新建预生成记录 id。
 */
async function loadInit() {
  const [dataRes, mainRes] = await Promise.all([
    wikiMainDataApi.load(dataId.value),
    fieldMainId.value ? wikiMainApi.load(fieldMainId.value) : Promise.resolve({ data: {} })
  ])
  meta.value = dataRes.data || {}
  simpleName.value = mainRes.data?.fieldSimpleName || ''
  form.value.fieldDataId = dataId.value

  if (!isEdit.value) {
    await preGenerateRecordId()
  }
  await loadJoinOptions()
}

/** 新建场景预生成记录 id：供附件列上传关联（后端保存时原样保留） */
async function preGenerateRecordId() {
  try {
    const res = await idApi.init()
    form.value.fieldId = res.data || ''
  } catch {
    form.value.fieldId = ''
  }
}

/**
 * 加载关联项选择控件下拉源：来自目标动态表 field_name + field_id。
 */
async function loadJoinOptions() {
  const targets = new Map()
  for (const detail of editableDetails.value) {
    if (detail.type === 'join' && detail.join) {
      targets.set(detail.dataName, detail.join)
    }
  }
  if (targets.size === 0) return

  const options = {}
  await Promise.all(
    [...targets.entries()].map(async ([dataName, targetId]) => {
      try {
        const res = await wikiDataApi.list({
          data: { fieldDataId: targetId },
          query: { pageNum: 1, pageSize: 999, needPage: false, sortField: 'fieldId', sortOrder: 'desc' }
        })
        options[dataName] = (res.list || []).map((row) => ({
          label: row.fieldName || row.fieldCode || row.fieldId,
          value: row.fieldId
        }))
      } catch {
        options[dataName] = []
      }
    })
  )
  joinOptions.value = options
}

/**
 * 编辑模式加载明细记录。
 */
async function loadDetail() {
  if (!isEdit.value) return
  const res = await wikiDataApi.load(recordId.value, dataId.value)
  const data = res.data || {}
  form.value = {
    fieldId: data.fieldId || '',
    fieldDataId: dataId.value,
    fieldName: data.fieldName || '',
    fieldCode: data.fieldCode || '',
    fieldContext: data.fieldContext || '',
    fieldData: data.fieldData || {}
  }
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
    const data = {
      fieldDataId: dataId.value,
      fieldId: form.value.fieldId,
      fieldData: form.value.fieldData
    }
    if (dataType.value === 'data' || dataType.value === 'doc') {
      data.fieldName = form.value.fieldName
      data.fieldCode = form.value.fieldCode
    }
    if (dataType.value === 'doc') {
      data.fieldContext = form.value.fieldContext
    }

    const payload = { data }
    if (isEdit.value) {
      await wikiDataApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await wikiDataApi.save(payload)
      ElMessage.success('新建成功')
    }
    router.push(`/admin/wiki/${fieldMainId.value}/data-item/${dataId.value}`)
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.push(`/admin/wiki/${fieldMainId.value}/data-item/${dataId.value}`)
}

onMounted(async () => {
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
}
</style>
