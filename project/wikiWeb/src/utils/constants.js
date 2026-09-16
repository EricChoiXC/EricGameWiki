/**
 * wiki 前台系统公共常量
 * 数据项类型与后端 com.wiki.admin.wiki.util.WikiConstants 保持一致
 */

// 数据项类型
export const WIKI_DATA_TYPE = {
  DATA: 'data',
  JOIN: 'join',
  DOC: 'doc'
}

// 数据项类型选项（项目首页"数据项类型"筛选）
export const WIKI_DATA_TYPE_OPTIONS = [
  { value: WIKI_DATA_TYPE.DATA, label: '图鉴类' },
  { value: WIKI_DATA_TYPE.JOIN, label: '关联项' },
  { value: WIKI_DATA_TYPE.DOC, label: '文档类' }
]

// 页面配置显示信息来源（WikiPageConfigDo.displayInfos[].type）
export const PAGE_SOURCE_TYPE = {
  SELF: 'self',
  JOIN: 'join'
}

/**
 * 将属性键转换为可读标签（详情页兜底展示用）
 * 动态列 key 为小驼峰（如 fieldPrice），转换为 "Field Price" 形态；
 * 若后续接口返回字段名称元数据，应优先使用元数据名称替换本兜底逻辑。
 * @param {string} key 属性键
 * @returns {string}
 */
export function humanizeFieldKey(key) {
  const str = String(key || '').replace(/^field/, '')
  if (!str) return String(key || '')
  return str
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (c) => c.toUpperCase())
    .trim()
}

/**
 * 解析记录字段值：优先取记录顶层属性，其次取动态列 fieldData
 * @param {Object} record WebWikiRecordVo
 * @param {string} key 属性键（如 fieldName / fieldPrice）
 * @returns {*}
 */
export function resolveRecordField(record, key) {
  if (record == null) return ''
  if (record[key] !== undefined && record[key] !== null) {
    return record[key]
  }
  const data = record.fieldData || {}
  return data[key] ?? ''
}
