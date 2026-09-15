<template>
  <!-- 时间（calendarType=time）：el-time-picker -->
  <el-time-picker
    v-if="calendarType === 'time'"
    :model-value="modelValue || null"
    :format="timeFormat"
    :value-format="timeFormat"
    :placeholder="placeholder"
    :disabled-hours="timeDisabled.disabledHours"
    :disabled-minutes="timeDisabled.disabledMinutes"
    :disabled-seconds="timeDisabled.disabledSeconds"
    clearable
    style="width: 100%"
    @update:model-value="onInput"
  />

  <!-- 日期 / 日期时间 / 年 / 年月 / 周：el-date-picker -->
  <el-date-picker
    v-else
    :model-value="modelValue || null"
    :type="pickerType"
    :format="displayFormat"
    :value-format="valueFormat"
    :placeholder="placeholder"
    :disabled-date="disabledDate"
    :disabled-time="pickerType === 'datetime' ? disabledTime : undefined"
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
  buildTimePickerDisabled,
  parseRangeOrList,
  toDayjsFormat
} from '@/utils/datePicker'

/**
 * 单日期时间选择器
 * 契约来源：docs/common/前端公共组件.md 四.1
 * - 支持 year / month / week / date / time / datetime 六种类型
 * - 通过 v-model 双向绑定格式化后的字符串值（清除时为 null）
 */

const props = defineProps({
  /** 选择器关联的字段，必填（用于占位提示等场景） */
  fieldName: {
    type: String,
    default: ''
  },
  /** 选择器类型，默认 datetime */
  calendarType: {
    type: String,
    default: 'datetime',
    validator: (v) => ['year', 'month', 'week', 'date', 'time', 'datetime'].includes(v)
  },
  /** 日期格式，默认 'yyyy-MM-dd' */
  dateFormat: {
    type: String,
    default: 'yyyy-MM-dd'
  },
  /** 时间格式，默认 'HH:mm:ss' */
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
  /** 当前值（格式化字符串，如 '2024-01-01 08:30:00'） */
  modelValue: {
    type: [String, Number, Array],
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])

const CALENDAR_CONFIG = {
  year: { type: 'year', valueFormat: 'yyyy', withMonth: false, withDay: false },
  month: { type: 'month', valueFormat: 'yyyy-MM', withMonth: true, withDay: false },
  week: { type: 'week', valueFormat: null, withMonth: true, withDay: true },
  date: { type: 'date', valueFormat: null, withMonth: true, withDay: true },
  datetime: { type: 'datetime', valueFormat: null, withMonth: true, withDay: true }
}

const config = computed(() => CALENDAR_CONFIG[props.calendarType])
const pickerType = computed(() => (props.calendarType === 'time' ? 'time' : config.value.type))

/** 展示格式（Java 风格 → dayjs 风格） */
const displayFormat = computed(() => {
  if (props.calendarType === 'time') return props.timeFormat
  if (props.calendarType === 'datetime') return toDayjsFormat(props.datetimeFormat)
  if (props.calendarType === 'week' || props.calendarType === 'date') {
    return toDayjsFormat(props.dateFormat)
  }
  return toDayjsFormat(config.value.valueFormat)
})

/** 绑定值格式（与展示格式一致；week 类型由 el-date-picker 返回所在日期） */
const valueFormat = computed(() => {
  if (props.calendarType === 'datetime') return toDayjsFormat(props.datetimeFormat)
  if (props.calendarType === 'week' || props.calendarType === 'date') {
    return toDayjsFormat(props.dateFormat)
  }
  return toDayjsFormat(config.value.valueFormat)
})

const placeholder = computed(() => (props.fieldName ? `请选择${props.fieldName}` : '请选择'))

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

const timeDisabled = computed(() =>
  buildTimePickerDisabled({ hourRule: hourRule.value, minuteRule: minuteRule.value })
)

function onInput(value) {
  emit('update:modelValue', value || null)
}
</script>

<style lang="scss" scoped>
/* 宽度由使用方控制，组件默认占满容器 */
</style>
