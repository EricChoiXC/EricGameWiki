import request from './request'

/**
 * wiki 前台系统 API
 * 模块映射：docs/wiki/*（首页 / 项目首页 / 图鉴类数据项页面 / 文档类页面）
 *
 * 路径前缀对齐 AGENTS.md §4.2/§5：baseURL=/api/v1/wiki，模块路径 /main /data /data-item。
 * 后台接口见 project/wikiService/src/main/java/com/wiki/web/wiki/controller/ 下
 * WebWikiMainController / WebWikiMainDataController / WebWikiDataItemController。
 *
 * 前台只读约束（docs/wiki/wiki访问.md）：
 *  - 无登录校验，仅允许发起 "/wiki/*" 接口请求
 *  - 仅允许读取操作，禁止使用这些接口进行修改操作
 */

// ---------------------------------------------------------------------------
// 项目管理（docs/wiki/首页.md / docs/wiki/项目首页.md）
// ---------------------------------------------------------------------------
export const wikiMainApi = {
  /**
   * 首页项目列表：关键字模糊匹配中文名称、英文名称、日文名称、简称，仅返回开启项目，不分页。
   * @param {string} [keyword]
   * @returns 响应 list 为 WebWikiProjectVo[]（fieldId/fieldName/fieldEnName/fieldJpName/fieldSimpleName）
   */
  list(keyword) {
    return request.get('/main/list', { params: { keyword } })
  },

  /**
   * 项目信息加载：按项目简称加载（项目首页 / 顶部公共布局）。
   * @param {string} fieldSimpleName 项目简称
   */
  load(fieldSimpleName) {
    return request.get('/main/load', { params: { fieldSimpleName } })
  }
}

// ---------------------------------------------------------------------------
// 数据项管理（docs/wiki/项目首页.md）
// ---------------------------------------------------------------------------
export const wikiDataItemApi = {
  /**
   * 项目数据项列表：关键字模糊匹配数据项名称、简称，可按数据项类型过滤，不分页。
   * @param {string} fieldSimpleName 项目简称
   * @param {Object} [params] { keyword, fieldDataType }
   * @returns 响应 list 为 WebWikiDataItemVo[]（fieldId/fieldName/fieldDataName/fieldDataType）
   */
  list(fieldSimpleName, params = {}) {
    return request.get('/data/list', { params: { fieldSimpleName, ...params } })
  }
}

// ---------------------------------------------------------------------------
// 数据明细维护（docs/wiki/图鉴类数据项页面.md / docs/wiki/文档类页面.md）
// ---------------------------------------------------------------------------
export const wikiRecordApi = {
  /**
   * 数据明细记录列表：图鉴类关键字匹配名称/编号，文档类关键字匹配标题，不分页。
   * @param {string} fieldSimpleName 项目简称
   * @param {string} fieldDataName 数据项简称
   * @param {string} [keyword]
   * @returns 响应 data 为 { dataItem: WebWikiDataItemVo, records: WebWikiRecordVo[] }
   */
  list(fieldSimpleName, fieldDataName, keyword) {
    return request.get('/data-item/list', {
      params: { fieldSimpleName, fieldDataName, keyword }
    })
  },

  /**
   * 数据明细记录详情：返回记录数据、wiki 页面配置与引用本记录的关联项记录。
   * @param {string} fieldSimpleName 项目简称
   * @param {string} fieldDataName 数据项简称
   * @param {string} fieldId 明细记录 id
   * @returns 响应 data 为 WebWikiRecordDetailVo
   *   {
   *     dataItem, record,
   *     pageConfig: { displayInfos[], displayFields[] },
   *     relatedRecords,
   *     details: [{ dataName, name, type, fieldKey }],
   *     joinItems: { [dataItemId]: { fieldId, fieldName, fieldDataName, fieldDataType, details[] } }
   *   }
   */
  load(fieldSimpleName, fieldDataName, fieldId) {
    return request.get('/data-item/load', {
      params: { fieldSimpleName, fieldDataName, fieldId }
    })
  }
}

export default {
  wikiMainApi,
  wikiDataItemApi,
  wikiRecordApi
}
