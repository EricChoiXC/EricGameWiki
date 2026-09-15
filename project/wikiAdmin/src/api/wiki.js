import request from './request'
import { createStandardApi } from './system'

/**
 * wiki 模块 API
 * 模块映射：docs/admin/wiki/wikiAPI接口设计文档.md
 *
 * 路径前缀对齐 AGENTS.md §4.2/§5：baseURL=/api/v1/admin，模块路径 /wiki/...
 * 通用接口规范遵循 docs/common/接口公共规范.md 第 3.4 章
 *
 *  - 项目管理 → /wiki/...
 *  - 数据项管理 → /wiki/data/...
 *  - 数据明细维护 → /wiki/data-item/...
 */

// ---------------------------------------------------------------------------
// 项目管理（wiki_main）
// ---------------------------------------------------------------------------
export const wikiMainApi = {
  ...createStandardApi('/wiki'),

  /**
   * 启用/停用项目（PATCH /update 仅更新状态字段）
   * @param {string} fieldId
   * @param {number} fieldStatus 1 开启 / 0 停用
   */
  updateStatus(fieldId, fieldStatus) {
    return request.patch('/wiki/update', {
      data: { fieldId, fieldStatus }
    })
  }
}

// ---------------------------------------------------------------------------
// 数据项管理（wiki_main_data）
// ---------------------------------------------------------------------------
export const wikiMainDataApi = {
  ...createStandardApi('/wiki/data'),

  /**
   * 数据项列表查询（携带 fieldMainId 用于鉴权与过滤）
   * @param {string} fieldMainId 项目id
   * @param {Object} [pageQuery] { pageNum, pageSize, needPage, sortField, sortOrder }
   */
  listByMain(fieldMainId, pageQuery = {}) {
    const { pageNum = 1, pageSize = 15, needPage = false, sortField = 'fieldId', sortOrder = 'desc' } = pageQuery
    return request.post('/wiki/data/list', {
      data: { fieldMainId },
      query: { pageNum, pageSize, needPage, sortField, sortOrder }
    })
  },

  /**
   * 数据项初始化（返回数据项类型枚举、关联项选择源）
   * @param {string} fieldMainId 项目id
   */
  init(fieldMainId) {
    return request.get('/wiki/data/init', { params: { fieldMainId } })
  }
}

// ---------------------------------------------------------------------------
// 数据明细维护（wiki 动态表数据，docs/admin/wiki/wikiAPI接口设计文档.md 第 6 节）
// 以 fieldDataId 为入口，后端按数据项类型分发；动态列值统一放 fieldData（小驼峰 key）
// ---------------------------------------------------------------------------
export const wikiDataApi = {
  /**
   * API-W201 数据明细列表查询
   * @param {Object} payload { data: { fieldDataId }, query: { pageNum, pageSize, needPage, data, sortField, sortOrder } }
   */
  list(payload) {
    return request.post('/wiki/data-item/list', payload)
  },

  /**
   * API-W202 数据明细新建（返回新建记录 id）
   */
  save(payload) {
    return request.post('/wiki/data-item/save', payload)
  },

  /**
   * API-W203 数据明细加载
   * @param {string} fieldId 明细记录 id
   * @param {string} fieldDataId 数据项 id
   */
  load(fieldId, fieldDataId) {
    return request.get('/wiki/data-item/load', { params: { fieldId, fieldDataId } })
  },

  /**
   * API-W204 数据明细更新
   */
  update(payload) {
    return request.patch('/wiki/data-item/update', payload)
  },

  /**
   * API-W205 数据明细删除
   * @param {string} fieldId 明细记录 id
   * @param {string} fieldDataId 数据项 id
   */
  remove(fieldId, fieldDataId) {
    return request.delete('/wiki/data-item/delete', { params: { fieldId, fieldDataId } })
  },

  /**
   * API-W206 数据明细批量删除（仅关联项）
   */
  batchDelete(payload) {
    return request.post('/wiki/data-item/batch-delete', payload)
  },

  /**
   * API-W207 导入模板下载（xlsx 文件流）
   * @param {string} fieldDataId 数据项 id
   */
  template(fieldDataId) {
    return request.get('/wiki/data-item/template', { params: { fieldDataId }, responseType: 'blob' })
  },

  /**
   * API-W208 数据明细导入
   * @param {Object} payload { data: { fieldDataId, fieldAttachmentId, skipFail, skipError } }
   */
  import(payload) {
    return request.post('/wiki/data-item/import', payload)
  },

/**
 * API-W209 数据明细导出（xlsx 文件流）
 */
export(payload) {
  return request.post('/wiki/data-item/export', payload, { responseType: 'blob' })
}
}

// ---------------------------------------------------------------------------
// wiki 页面维护（wiki_main_data_wiki_page，docs/admin/wiki/wikiAPI接口设计文档.md 第 7 节）
// ---------------------------------------------------------------------------
export const wikiPageApi = {
  /**
   * API-W301 wiki 页面配置加载
   * @param {string} fieldDataId 数据项 id
   * @returns 响应 data: { fieldId, fieldDataId, fieldWikiPage: { displayInfos, displayFields } }
   */
  load(fieldDataId) {
    return request.get('/wiki/page/load', { params: { fieldDataId } })
  },

  /**
   * API-W302 wiki 页面配置保存
   * @param {Object} payload { data: { fieldDataId, fieldWikiPage: { displayInfos, displayFields } } }
   */
  save(payload) {
    return request.post('/wiki/page/save', payload)
  },

  /**
   * API-W303 wiki 页面配置初始化
   * @param {string} fieldDataId 数据项 id
   * @returns 响应 data: { dataItem: { fieldId, fieldName, fieldDataName, fieldDataType, details[] }, joinItems: [...] }
   */
  init(fieldDataId) {
    return request.get('/wiki/page/init', { params: { fieldDataId } })
  }
}

export default {
  wikiMainApi,
  wikiMainDataApi,
  wikiDataApi,
  wikiPageApi
}
