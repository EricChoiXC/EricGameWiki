/**
 * 日期时间选择器工具
 * 契约来源：docs/common/前端公共组件.md 四. 日期时间选择器
 * - Java 风格日期格式（yyyy-MM-dd）到 dayjs 格式（YYYY-MM-DD）的转换
 * - 范围/列表两种形态的范围配置解析与命中判断（yearRange/monthRange/hourRange/minuteRange/weekDays）
 * - 由范围配置构建 Element Plus 的 disabled-date / disabled-time / time-picker 禁用函数
 */

/**
 * 将 Java 风格日期格式转换为 dayjs 格式（Element Plus value-format 使用）
 * @param {string} fmt 如 'yyyy-MM-dd HH:mm:ss'
 * @returns {string} 如 'YYYY-MM-DD HH:mm:ss'
 */
export function toDayjsFormat(fmt) {
  return String(fmt || '')
    .replace(/yyyy/gi, 'YYYY')
    .replace(/dd/gi, 'DD')
}

/**
 * 解析范围配置，支持范围字符串与数值列表两种形态
 * 形态示例：'2024-2030' / '2024-' / '-2030' / [2024, 2025, 2026] / 2024
 * @param {string|number|number[]} [value]
 * @returns {{ kind: 'range', min: number|null, max: number|null } | { kind: 'list', values: number[] } | null}
 */
export function parseRangeOrList(value) {
  if (value == null || value === '') return null
  if (Array.isArray(value)) {
    const values = value.map(Number).filter((n) => !Number.isNaN(n))
    return values.length > 0 ? { kind: 'list', values } : null
  }
  const str = String(value).trim()
  if (!str) return null
  const matched = str.match(/^(\d*)\s*-\s*(\d*)$/)
  if (matched) {
    const min = matched[1] === '' ? null : Number(matched[1])
    const max = matched[2] === '' ? null : Number(matched[2])
    if (min == null && max == null) return null
    return { kind: 'range', min, max }
  }
  const single = Number(str)
  return Number.isNaN(single) ? null : { kind: 'list', values: [single] }
}

/**
 * 判断数值是否命中范围配置（未配置时全部允许）
 * @param {number} value
 * @param {{kind: string, min?: number, max?: number, values?: number[]}|null} rule
 * @returns {boolean}
 */
export function isAllowed(value, rule) {
  if (!rule) return true
  if (rule.kind === 'range') {
    if (rule.min != null && value < rule.min) return false
    if (rule.max != null && value > rule.max) return false
    return true
  }
  return rule.values.includes(value)
}

/**
 * 获取星期序号（约定 1 为星期一，7 为星期日，对齐文档 weekDays 约定）
 * @param {Date} date
 * @returns {number}
 */
export function weekdayNo(date) {
  const day = date.getDay()
  return day === 0 ? 7 : day
}

function hours() {
  return Array.from({ length: 24 }, (_, i) => i)
}

function minutes() {
  return Array.from({ length: 60 }, (_, i) => i)
}

/**
 * 构建 el-date-picker 的 disabled-date 函数
 * @param {Object} options
 * @param {Object|null} options.yearRule 年份范围配置
 * @param {Object|null} options.monthRule 月份范围配置
 * @param {Object|null} options.weekDaysRule 星期配置
 * @param {boolean} [options.withMonth] 是否按月份粒度限制
 * @param {boolean} [options.withDay] 是否按星期粒度限制
 * @returns {(date: Date) => boolean} true 表示该日期不可选
 */
export function buildDisabledDate({ yearRule, monthRule, weekDaysRule, withMonth, withDay }) {
  return (date) => {
    if (date == null) return false
    if (!isAllowed(date.getFullYear(), yearRule)) return true
    if (withMonth && !isAllowed(date.getMonth() + 1, monthRule)) return true
    if (withDay && !isAllowed(weekdayNo(date), weekDaysRule)) return true
    return false
  }
}

/**
 * 构建 el-date-picker（datetime/datetimerange）的 disabled-time 函数
 * @param {Object} options
 * @param {Object|null} options.hourRule 小时范围配置
 * @param {Object|null} options.minuteRule 分钟范围配置
 * @returns {(date: Date) => { disabledHours: Function, disabledMinutes: Function, disabledSeconds: Function }}
 */
export function buildDisabledTime({ hourRule, minuteRule }) {
  return (date) => {
    const hour = date ? date.getHours() : 0
    const hourOk = isAllowed(hour, hourRule)
    return {
      disabledHours: () => hours().filter((h) => !isAllowed(h, hourRule)),
      disabledMinutes: () => (hourOk ? minutes().filter((m) => !isAllowed(m, minuteRule)) : minutes()),
      disabledSeconds: () => []
    }
  }
}

/**
 * 构建 el-time-picker 的 disabled-hours / disabled-minutes / disabled-seconds 函数
 * 分钟粒度通过禁用该分钟下的全部秒实现（Element Plus 时间面板的秒级限制）
 * @param {Object} options
 * @param {Object|null} options.hourRule 小时范围配置
 * @param {Object|null} options.minuteRule 分钟范围配置
 * @returns {{ disabledHours: Function, disabledMinutes: Function, disabledSeconds: Function }}
 */
export function buildTimePickerDisabled({ hourRule, minuteRule }) {
  return {
    disabledHours: (hour) => !isAllowed(hour, hourRule),
    disabledMinutes: (hour) => !isAllowed(hour, hourRule),
    disabledSeconds: (hour, minute) => {
      if (!isAllowed(hour, hourRule)) return true
      if (minute == null) return false
      return !isAllowed(minute, minuteRule)
    }
  }
}
