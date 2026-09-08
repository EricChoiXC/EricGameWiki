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
 * @param {string} baseUrl 模块基础路径，如 '/sys/org/user'
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
export const sysOrgApi = createStandardApi('/sys/org/user')
export const sysAttachmentApi = createStandardApi('/sys/attachment')
export const sysAuditApi = createStandardApi('/sys/audit')

export default {
  sysOrgApi,
  sysAttachmentApi,
  sysAuditApi,
  createStandardApi
}
