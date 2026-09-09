/**
 * 查询条件构造工具
 * 通用约束遵循 docs/common/查询标准.md：
 *  query.data 支持 and / or 嵌套，内部为 eq/ne/like/lt/lte/gt/gte/in/between 等条件对象
 *
 * 示例：
 *   buildListQuery(
 *     [like('fieldName', '张'), eq('fieldStatus', 'ENABLED')],
 *     { pageNum: 1, pageSize: 15, sortField: 'fieldCreateTime', sortOrder: 'desc' }
 *   )
 *   =>
 *   {
 *     query: {
 *       pageNum: 1, pageSize: 15, needPage: true,
 *       data: { and: [ {like:{fieldName:'张'}}, {eq:{fieldStatus:'ENABLED'}} ] },
 *       sortField: 'fieldCreateTime', sortOrder: 'desc'
 *     }
 *   }
 */

/**
 * 构造列表查询报文
 * @param {Array<Object|null|undefined>} conditions 顶层 and 条件数组（自动过滤空值）
 * @param {Object} [options]
 * @param {number} [options.pageNum=1]
 * @param {number} [options.pageSize=15]
 * @param {boolean} [options.needPage=true]
 * @param {string} [options.sortField]
 * @param {string} [options.sortOrder='desc'] 'asc' | 'desc'
 * @returns {{ query: Object }}
 */
export function buildListQuery(conditions = [], options = {}) {
  const {
    pageNum = 1,
    pageSize = 15,
    needPage = true,
    sortField,
    sortOrder = 'desc'
  } = options

  const query = { pageNum, pageSize, needPage }

  const filtered = conditions.filter((c) => c != null)
  if (filtered.length > 0) {
    query.data = { and: filtered }
  }

  if (sortField) {
    query.sortField = sortField
    query.sortOrder = sortOrder
  }

  return { query }
}

// ---------------------------------------------------------------------------
// 条件构造器：返回单个条件对象，传入空值时返回 null 以便过滤
// ---------------------------------------------------------------------------

export function eq(field, value) {
  return value === '' || value == null ? null : { eq: { [field]: value } }
}

export function ne(field, value) {
  return value === '' || value == null ? null : { ne: { [field]: value } }
}

export function like(field, value) {
  return !value ? null : { like: { [field]: value } }
}

export function start(field, value) {
  return !value ? null : { start: { [field]: value } }
}

export function end(field, value) {
  return !value ? null : { end: { [field]: value } }
}

export function lt(field, value) {
  return value === '' || value == null ? null : { lt: { [field]: value } }
}

export function lte(field, value) {
  return value === '' || value == null ? null : { lte: { [field]: value } }
}

export function gt(field, value) {
  return value === '' || value == null ? null : { gt: { [field]: value } }
}

export function gte(field, value) {
  return value === '' || value == null ? null : { gte: { [field]: value } }
}

export function between(field, range) {
  if (!Array.isArray(range) || range.length !== 2) return null
  const [from, to] = range
  if (!from && !to) return null
  return { between: { [field]: [from, to] } }
}

export function inOp(field, values) {
  if (!Array.isArray(values) || values.length === 0) return null
  return { in: { [field]: values } }
}

/**
 * 构造时间范围条件（按 day 维度的 between，前端拆分为起止时刻）
 * @param {string} field
 * @param {[string,string]|null} range [startDate, endDate]
 * @returns {Object|null}
 */
export function dateRange(field, range) {
  if (!Array.isArray(range) || range.length !== 2) return null
  const [from, to] = range
  if (!from && !to) return null
  return { between: { [field]: [from ? `${from} 00:00:00` : null, to ? `${to} 23:59:59` : null] } }
}
