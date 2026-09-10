/**
 * 文件工具
 * 通用约束遵循 docs/common/前端公共组件.md 附件组件与 docs/admin/附件机制.md
 * 提供文件大小格式化、文件类型/大小校验等可复用逻辑
 */

/**
 * 文件大小单位（按 1024 进制）
 */
const FILE_SIZE_UNITS = [
  { unit: 'GB', bytes: 1024 * 1024 * 1024 },
  { unit: 'MB', bytes: 1024 * 1024 },
  { unit: 'KB', bytes: 1024 },
  { unit: 'B', bytes: 1 }
]

/**
 * 将字节数格式化为带单位的可读字符串
 * 规则：优先用满足 >= 1 的最大单位，保留两位小数；B 为整数
 * @param {number} size 字节数
 * @param {number} [digits=2] 小数位数
 * @returns {string} 如 '1.50 MB' / '512 KB' / '200 B'；size 为空时返回 '-'
 */
export function formatFileSize(size, digits = 2) {
  if (size == null || size === '' || Number.isNaN(Number(size))) return '-'
  const n = Number(size)
  if (n < 0) return '-'
  for (const { unit, bytes } of FILE_SIZE_UNITS) {
    if (n >= bytes) {
      return `${(n / bytes).toFixed(unit === 'B' ? 0 : digits)} ${unit}`
    }
  }
  return '0 B'
}

/**
 * 以 KB 为单位展示文件大小（超出 maxSize 时提示用）
 * @param {number} size 字节数
 * @returns {string} 如 '512.00 KB'
 */
export function formatFileSizeKb(size) {
  if (size == null || Number.isNaN(Number(size))) return '0.00 KB'
  return `${(Number(size) / 1024).toFixed(2)} KB`
}

/**
 * 将 maxSize（byte）格式化为人类可读的阈值提示
 * 优先输出 KB，使提示更直观
 * @param {number} maxSize 字节数
 * @returns {string} 如 '1024.00 KB' / '1.00 MB'
 */
export function formatMaxSize(maxSize) {
  if (!maxSize) return ''
  return formatFileSize(maxSize)
}

/**
 * 规范化文件后缀为小写，带点号
 * @param {string} ext 如 'jpg' / '.JPG' / '.jpg'
 * @returns {string} 如 '.jpg'
 */
function normalizeExt(ext) {
  let e = String(ext || '').trim().toLowerCase()
  if (!e.startsWith('.')) e = '.' + e
  return e
}

/**
 * 校验文件后缀是否符合允许的类型
 * @param {File} file 文件对象
 * @param {string|string[]} [allowedTypes] 允许的后缀，如 '.jpg' 或 ['.jpg', '.png']；为空表示不限制
 * @returns {boolean}
 */
export function validateFileType(file, allowedTypes) {
  if (!allowedTypes) return true
  const list = Array.isArray(allowedTypes) ? allowedTypes : [allowedTypes]
  const allowed = list.map(normalizeExt).filter((e) => e)
  if (allowed.length === 0) return true
  const name = (file?.name || '').toLowerCase()
  return allowed.some((ext) => name.endsWith(ext))
}

/**
 * 校验文件大小是否超出上限
 * @param {File} file 文件对象
 * @param {number} [maxSize] 最大字节数；为空或 <=0 表示不限制
 * @returns {boolean} true 表示通过（未超限）
 */
export function validateFileSize(file, maxSize) {
  if (!maxSize || maxSize <= 0) return true
  const size = file?.size ?? 0
  return size <= maxSize
}

/**
 * 构造文件类型不匹配的提示文案
 * @param {string|string[]} allowedTypes
 * @returns {string}
 */
export function buildFileTypeTip(allowedTypes) {
  if (!allowedTypes) return ''
  const list = Array.isArray(allowedTypes) ? allowedTypes : [allowedTypes]
  const exts = list.map(normalizeExt).filter((e) => e)
  if (exts.length === 0) return ''
  return `仅支持 ${exts.join('、')} 格式`
}

/**
 * 构造文件大小超限的提示文案（至少以 KB 计算）
 * @param {File} file 实际文件（用于展示实际大小）
 * @param {number} maxSize 最大字节数
 * @returns {string}
 */
export function buildFileSizeTip(file, maxSize) {
  if (!maxSize || maxSize <= 0) return ''
  const actual = formatFileSizeKb(file?.size ?? 0)
  const limit = formatMaxSize(maxSize)
  return `文件大小 ${actual}，不能超过 ${limit}`
}
