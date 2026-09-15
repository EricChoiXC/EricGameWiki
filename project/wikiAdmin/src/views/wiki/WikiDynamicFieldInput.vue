<template>
  <!-- 文本 -->
  <el-input
    v-if="detail.type === 'text'"
    :model-value="modelValue"
    placeholder="请输入"
    maxlength="200"
    @update:model-value="onInput"
  />

  <!-- 富文本 -->
  <RichTextEditor
    v-else-if="detail.type === 'blob'"
    :model-value="stringValue"
    :placeholder="`请输入${detail.name}`"
    @update:model-value="onInput"
  />

  <!-- 数字 -->
  <el-input-number
    v-else-if="detail.type === 'number'"
    :model-value="modelValue"
    :controls="false"
    placeholder="请输入数字"
    style="width: 100%"
    @update:model-value="onInput"
  />

  <!-- 日期 -->
  <el-date-picker
    v-else-if="detail.type === 'date'"
    :model-value="modelValue"
    type="date"
    value-format="YYYY-MM-DD"
    placeholder="请选择日期"
    style="width: 100%"
    @update:model-value="onInput"
  />

  <!-- 日期时间 -->
  <el-date-picker
    v-else-if="detail.type === 'datetime'"
    :model-value="modelValue"
    type="datetime"
    value-format="YYYY-MM-DDTHH:mm:ss"
    placeholder="请选择日期时间"
    style="width: 100%"
    @update:model-value="onInput"
  />

  <!-- 时间 -->
  <el-time-picker
    v-else-if="detail.type === 'time'"
    :model-value="modelValue"
    value-format="HH:mm:ss"
    placeholder="请选择时间"
    style="width: 100%"
    @update:model-value="onInput"
  />

  <!-- 布尔 -->
  <el-switch
    v-else-if="detail.type === 'boolean'"
    :model-value="modelValue"
    :active-value="1"
    :inactive-value="0"
    active-text="是"
    inactive-text="否"
    @update:model-value="onInput"
  />

  <!-- 枚举 -->
  <el-select
    v-else-if="detail.type === 'enum'"
    :model-value="modelValue"
    placeholder="请选择"
    clearable
    filterable
    style="width: 100%"
    @update:model-value="onInput"
  >
    <el-option
      v-for="opt in (detail.enums || [])"
      :key="opt"
      :label="opt"
      :value="opt"
    />
  </el-select>

  <!-- 附件 -->
  <AttachmentUploader
    v-else-if="detail.type === 'attachment'"
    :field-model-name="modelName"
    :field-model-id="recordId"
    :field-key="fieldKey"
    :multi="true"
  />

  <!-- 关联数据 -->
  <el-select
    v-else-if="detail.type === 'join'"
    :model-value="modelValue"
    placeholder="请选择关联数据"
    clearable
    filterable
    style="width: 100%"
    @update:model-value="onInput"
  >
    <el-option
      v-for="opt in joinOptions"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>

  <span v-else class="wiki-dynamic-field__placeholder">-</span>
</template>

<script setup>
import { computed } from 'vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'

const props = defineProps({
  /** 当前值 */
  modelValue: {
    type: [String, Number, Boolean, Array, Object],
    default: null
  },
  /** 明细行定义 */
  detail: {
    type: Object,
    required: true
  },
  /** 记录 id（附件上传关联 field_model_id） */
  recordId: {
    type: String,
    default: ''
  },
  /** 关联项选择控件下拉源：[{ label: fieldName, value: fieldId }] */
  joinOptions: {
    type: Array,
    default: () => []
  },
  /** 项目简称（附件 field_model_name） */
  simpleName: {
    type: String,
    default: ''
  },
  /** 数据项简称（附件 field_model_name） */
  dataName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const stringValue = computed(() => (props.modelValue == null ? '' : String(props.modelValue)))

/** 附件 field_model_name = ${项目简称}::${数据项简称}（docs/admin/附件机制.md） */
const modelName = computed(() =>
  props.simpleName && props.dataName ? `${props.simpleName}::${props.dataName}` : 'wiki-import'
)

/** 附件 field_key：按动态列命名 */
const fieldKey = computed(() => 'field' + upperFirst(props.detail.dataName))

function onInput(value) {
  emit('update:modelValue', value)
}

function upperFirst(str) {
  if (!str) return str
  return str.charAt(0).toUpperCase() + str.slice(1)
}
</script>

<style lang="scss" scoped>
.wiki-dynamic-field__placeholder {
  color: var(--el-text-color-placeholder);
}
</style>
