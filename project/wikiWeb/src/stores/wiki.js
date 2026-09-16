import { defineStore } from 'pinia'

/**
 * 前台 wiki 共享状态
 * 职责：持有当前项目上下文（项目信息 + 项目数据项列表），供 WikiLayout
 * 顶部公共布局（项目名展示 / 数据项下拉菜单）与项目内各页面复用，避免重复请求。
 *
 * 约束（docs/wiki/wiki访问.md）：
 *  - 前台只读，本 store 仅承载查询结果，不做任何修改操作
 *  - 项目上下文随路由参数 fieldSimpleName 变化而刷新（由 WikiLayout 驱动）
 */
export const useWikiStore = defineStore('wiki', {
  state: () => ({
    // 当前项目信息（WebWikiProjectVo）
    project: null,
    // 当前项目数据项列表（WebWikiDataItemVo[]）
    dataItems: [],
    // 已加载的项目简称（避免相同项目重复请求）
    simpleName: ''
  }),

  getters: {
    // 当前项目中文名
    projectName: (state) => state.project?.fieldName || '',
    // 当前是否处于首页（无项目上下文）
    isHome: (state) => !state.simpleName
  },

  actions: {
    /**
     * 设置项目上下文
     * @param {Object} context { simpleName, project, dataItems }
     */
    setContext({ simpleName, project, dataItems }) {
      this.simpleName = simpleName || ''
      this.project = project || null
      this.dataItems = Array.isArray(dataItems) ? dataItems : []
    },

    /**
     * 清空项目上下文（进入首页时调用）
     */
    clear() {
      this.simpleName = ''
      this.project = null
      this.dataItems = []
    }
  }
})
