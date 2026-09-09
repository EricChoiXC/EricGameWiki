/**
 * 数据脱敏工具
 * 用于列表页对手机号、邮箱等敏感字段进行脱敏展示
 */

/**
 * 手机号脱敏：保留前 3 位和后 4 位，中间用 **** 替换
 * @param {string} phone
 * @returns {string}
 */
export function maskPhone(phone) {
  if (!phone) return ''
  const s = String(phone).replace(/\s+/g, '')
  if (s.length <= 7) {
    return s.length <= 4 ? s : s.slice(0, 3) + '****'
  }
  return s.slice(0, 3) + '****' + s.slice(-4)
}

/**
 * 邮箱脱敏：保留首字符和 @ 后域名，中间用 *** 替换
 * @param {string} email
 * @returns {string}
 */
export function maskEmail(email) {
  if (!email) return ''
  const s = String(email)
  const at = s.indexOf('@')
  if (at <= 1) return s
  return s[0] + '***' + s.slice(at)
}
