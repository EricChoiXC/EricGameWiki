<template>
  <el-date-picker
    :model-value="modelValue || null"
    :type="pickerType"
    :format="displayFormats"
    :value-format="valueFormats"
    :start-placeholder="startPlaceholder"
    :end-placeholder="endPlaceholder"
    :disabled-date="disabledDate"
    :disabled-time="pickerType === 'datetimerange' ? disabledTime : undefined"
    range-separator="至"
    clearable
    style="width: 100%"
    @update:model-value="onInput"
  />
</template>

<script setup>
import { computed } from 'vue'
import {
  buildDisabledDate,
  buildDisabledTime,
  parseRangeOrList,
  toDayjsFormat
} from '@/utils/datePicker'

/**
 * 范围日期选择器
 * 契约来源：docs/common/前端公共组件.md 四.2（该节参数未展开，参数约定与单日期时间选择器保持一致）
 * - 支持 daterange / datetimerange / monthrange 三种范围类型
 * - 通过 v-model 双向绑定 [start, end] 格式化值（清除时为 null）
 */

const props = defineProps({
  /** 选择器关联的字段，必填（用于占位提示等场景） */
  fieldName: {
    type: String,
    default: ''
  },
  /** 选择器类型，默认 datetimerange */
  calendarType: {
    type: String,
    default: 'datetimerange',
    validator: (v) => ['daterange', 'datetimerange', 'monthrange'].includes(v)
  },
  /** 日期格式，默认 'yyyy-MM-dd' */
  dateFormat: {
    type: String,
    default: 'yyyy-MM-dd'
  },
  /** 时间格式，默认 'HH:mm:ss'（范围类型仅用于占位提示） */
  timeFormat: {
    type: String,
    default: 'HH:mm:ss'
  },
  /** 日期时间格式，默认 'yyyy-MM-dd HH:mm:ss' */
  datetimeFormat: {
    type: String,
    default: 'yyyy-MM-dd HH:mm:ss'
  },
  /** 可选年份范围或年份列表，如 '2024-2030' / [2024, 2025, 2026] / '2024-' / '-2030' */
  yearRange: {
    type: [String, Array, Number],
    default: ''
  },
  /** 可选月份范围或月份列表，如 '1-12' / [1, 3, 5, 7, 9, 11] */
  monthRange: {
    type: [String, Array, Number],
    default: ''
  },
  /** 可选小时范围或小时列表，如 '6-18' / [8, 9, 10, 14, 15, 16] */
  hourRange: {
    type: [String, Array, Number],
    default: ''
  },
  /** 可选分钟范围或分钟列表，如 '0-59' / [0, 15, 30, 45] */
  minuteRange: {
    type: [String, Array, Number],
    default: ''
  },
  /** 可选星期列表，如 [1] / [1, 2, 3, 4, 5]；约定 1 为星期一，7 为星期日 */
  weekDays: {
    type: [String, Array, Number],
    default: ''
  },
  /** 当前值：[start, end] 格式化字符串数组 */
  modelValue: {
    type: Array,
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])

const RANGE_CONFIG = {
  daterange: {
    type: 'daterange',
    valueFormat: null,
    withMonth: true,
    withDay: true,
    range: true
  },
  datetimerange: {
    type: 'datetimerange',
    valueFormat: null,
    withMonth: true,
    withDay: true,
    range: true
  },
  monthrange: {
    type: 'monthrange',
    valueFormat: 'yyyy-MM',
    withMonth: true,
    withDay: false,
    range: true
  }
}

const config = computed(() => RANGE_CONFIG[props.calendarType])
const pickerType = computed(() => config.value.type)

/** 展示格式（范围数组） */
const displayFormats = computed(() => {
  if (props.calendarType === 'datetimerange') {
    return [toDayjsFormat(props.datetimeFormat), toDayjsFormat(props.datetimeFormat)]
  }
  if (props.calendarType === 'daterange') {
    return [toDayjsFormat(props.dateFormat), toDayjsFormat(props.dateFormat)]
  }
  return [toDayjsFormat(config.value.valueFormat), toDayjsFormat(config.value.valueFormat)]
})

/** 绑定值格式（与展示格式一致） */
const valueFormats = computed(() => displayFormats.value)

const startPlaceholder = computed(() => (props.fieldName ? `开始${props.fieldName}` : '开始日期'))
const endPlaceholder = computed(() => (props.fieldName ? `结束${props.fieldName}` : '结束日期'))

const yearRule = computed(() => parseRangeOrList(props.yearRange))
const monthRule = computed(() => parseRangeOrList(props.monthRange))
const hourRule = computed(() => parseRangeOrList(props.hourRange))
const minuteRule = computed(() => parseRangeOrList(props.minuteRange))
const weekDaysRule = computed(() => parseRangeOrList(props.weekDays))

const disabledDate = computed(() =>
  buildDisabledDate({
    yearRule: yearRule.value,
    monthRule: monthRule.value,
    weekDaysRule: weekDaysRule.value,
    withMonth: config.value.withMonth,
    withDay: config.value.withDay
  })
)

const disabledTime = computed(() =>
  buildDisabledTime({ hourRule: hourRule.value, minuteRule: minuteRule.value })
)

function onInput(value) {
  emit('update:modelValue', value && value.length === 2 ? value : null)
}
</script>
