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

export default {
  wikiMainApi,
  wikiMainDataApi
}
