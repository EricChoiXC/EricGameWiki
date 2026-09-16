import request from './request'
import { createStandardApi } from './system'

/**
 * 附件模块 API
 * 模块映射：docs/admin/附件机制.md
 *
 * 表与语义对应关系（docs/admin/附件机制.md 表结构）：
 *  - admin_attachment_main  → 附件信息（关联业务对象）
 *  - admin_attachment_file  → 附件文件（文件元数据与存储路径）
 *
 * 路径前缀对齐 AGENTS.md §4.2/§5：baseURL=/api/v1/admin，模块路径 /attachment/...
 * 通用接口规范遵循 docs/common/接口公共规范.md 第 3.4 章
 * 附件关联约定（docs/admin/附件机制.md 业务逻辑）：
 *  - 通过 field_model_name + field_model_id + field_key 三个字段关联
 *  - 如：系统图标 field_model_name="system", field_key="system_icon"
 */

// ---------------------------------------------------------------------------
// 附件管理（admin_attachment_main + admin_attachment_file）
// ---------------------------------------------------------------------------
export const attachmentApi = {
  ...createStandardApi('/attachment'),

  /**
   * 上传附件（对应前端附件组件，docs/common/前端公共组件.md 附件组件）
   * @param {FormData} formData 包含 file + fieldModelName + fieldModelId + fieldKey
   * @param {(progress: number) => void} [onUploadProgress] 上传进度回调
   * @returns 响应含新创建的附件信息id（admin_attachment_main.field_id）
   */
  upload(formData, onUploadProgress) {
    return request.post('/attachment/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    })
  },

  /**
   * 下载附件
   * @param {string} fieldId 附件信息id
   * @returns Blob 响应
   */
  download(fieldId) {
    return request.get('/attachment/download', {
      params: { fieldId },
      responseType: 'blob'
    })
  },

  /**
   * 按业务关联下载附件，取关联的第一条（如当前用户头像回显）
   * @param {string} fieldModelName 关联对象模型名
   * @param {string} fieldModelId 关联对象id
   * @param {string} [fieldKey] 关联字段key
   * @returns Blob 响应；无关联附件时后端返回 404
   */
  downloadByModel(fieldModelName, fieldModelId, fieldKey) {
    return request.get('/attachment/downloadByModel', {
      params: { fieldModelName, fieldModelId, fieldKey },
      responseType: 'blob',
      // 无关联附件时后端返回 404，属预期场景，不弹全局错误提示
      silent: true
    })
  }
}

export default {
  attachmentApi
}
