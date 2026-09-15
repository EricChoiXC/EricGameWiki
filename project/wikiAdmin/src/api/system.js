import request from './request'

/**
 * 标准接口封装
 * 通用约束遵循 docs/common/接口公共规范.md 第 3.4 章：
 *  /init    GET
 *  /load?fieldId=    GET
 *  /list    POST
 *  /save    POST
 *  /update  PATCH
 *  /delete?fieldId=  DELETE
 *  /import  POST
 *  /export  POST
 *  /download?fieldId=  GET
 *
 * 各模块可基于此工厂快速生成标准 CRUD 接口集；非标准接口在各模块 API 文件中单独声明
 */

/**
 * 创建标准接口集合
 * @param {string} baseUrl 模块基础路径，如 '/org/user'
 * @returns {Record<string, Function>} 标准接口集合
 */
export function createStandardApi(baseUrl) {
  const url = baseUrl.replace(/\/$/, '')

  return {
    // GET /init
    init(params) {
      return request.get(`${url}/init`, { params })
    },
    // GET /load?fieldId=
    load(fieldId) {
      return request.get(`${url}/load`, { params: { fieldId } })
    },
    // POST /list
    list(payload) {
      // payload 形如 { data, query: { pageNum, pageSize, needPage, data }, map }
      return request.post(`${url}/list`, payload)
    },
    // POST /save
    save(payload) {
      // payload 形如 { data, map }
      return request.post(`${url}/save`, payload)
    },
    // PATCH /update
    update(payload) {
      // payload 形如 { data, map }
      return request.patch(`${url}/update`, payload)
    },
    // DELETE /delete?fieldId=
    remove(fieldId) {
      return request.delete(`${url}/delete`, { params: { fieldId } })
    },
    // POST /import
    import(payload, onUploadProgress) {
      const formData = payload instanceof FormData ? payload : new FormData()
      if (!(payload instanceof FormData)) {
        Object.entries(payload).forEach(([k, v]) => formData.append(k, v))
      }
      return request.post(`${url}/import`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress
      })
    },
    // POST /export
    export(payload) {
      return request.post(`${url}/export`, payload, {
        responseType: 'blob'
      })
    },
    // GET /download?fieldId=
    download(fieldId) {
      return request.get(`${url}/download`, {
        params: { fieldId },
        responseType: 'blob'
      })
    }
  }
}

/**
 * 系统公共模块 API
 * 模块映射：docs/AGENTS.md 第 9 章 公共系统（sys）
 *  - org 用户管理 / attachment 附件管理 / audit 审计
 */
export const sysOrgApi = createStandardApi('/org/user')

/**
 * 公共 ID 预生成接口
 * 模块映射：docs/admin/公共服务.md
 * 用于新建场景下需在落库前先取得主键（如先上传附件再保存主单据）的场景，
 * 返回的 id 与各模块 Service 落库时调用的 IDUtil.initID(id) 配合：前端回传相同 id 时后端原样保留。
 */
export const idApi = {
  /**
   * 预生成业务主键
   * @returns 响应 data 为 32 位业务主键字符串
   */
  init() {
    return request.get('/sys/id/init')
  }
}

export default {
  sysOrgApi,
  idApi,
  createStandardApi
}
