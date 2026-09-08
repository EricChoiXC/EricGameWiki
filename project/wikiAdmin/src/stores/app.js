import { defineStore } from 'pinia'

/**
 * 应用全局状态
 * 规范章节：docs/common/前端公共样式规范.md 第 14 章 admin 主页面多标签页
 *
 * 约束：
 *  - 固定第一个标签为「首页」且不可被关闭
 *  - 标签行右侧提供左右滚动按钮与三点下拉菜单
 *  - 三点下拉菜单底部提供「关闭其他」「关闭所有」快捷关闭选项
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    // 当前侧边栏折叠状态
    sidebarCollapsed: false,
    // 已打开的标签页列表
    tabs: [],
    // 当前激活的标签页 id（路由 path）
    activeTabId: ''
  }),

  getters: {
    // 首页标签（始终位于列表首位）
    homeTab: (state) => state.tabs.find((tab) => tab.closable === false),
    // 标签数量
    tabCount: (state) => state.tabs.length
  },

  actions: {
    /**
     * 初始化首页标签
     * 应用启动时调用一次，确保首位永远是「首页」
     */
    initHomeTab() {
      if (!this.tabs.some((t) => t.closable === false)) {
        this.tabs.unshift({
          id: '/home',
          title: '首页',
          path: '/home',
          closable: false
        })
      }
      if (!this.activeTabId) {
        this.activeTabId = '/home'
      }
    },

    /**
     * 添加标签页
     * 若同 id 已存在则仅激活，不重复添加
     * @param {{ id: string, title: string, path: string, closable?: boolean }} tab
     */
    addTab(tab) {
      this.initHomeTab()
      const exists = this.tabs.find((t) => t.id === tab.id)
      if (!exists) {
        this.tabs.push({
          closable: true,
          ...tab
        })
      }
      this.activeTabId = tab.id
    },

    /**
     * 关闭指定标签页
     * - 首页标签不可关闭
     * - 关闭当前激活标签时，自动激活相邻标签（优先右侧，其次左侧）
     * @param {string} tabId
     */
    closeTab(tabId) {
      const idx = this.tabs.findIndex((t) => t.id === tabId)
      if (idx === -1) return
      if (this.tabs[idx].closable === false) return

      this.tabs.splice(idx, 1)

      if (this.activeTabId === tabId) {
        const next = this.tabs[idx] || this.tabs[idx - 1]
        this.activeTabId = next ? next.id : '/home'
      }
    },

    /**
     * 关闭其他标签页（保留首页 + 当前激活）
     */
    closeOthers() {
      this.tabs = this.tabs.filter(
        (t) => t.closable === false || t.id === this.activeTabId
      )
    },

    /**
     * 关闭所有可关闭标签页，激活首页
     */
    closeAll() {
      this.tabs = this.tabs.filter((t) => t.closable === false)
      this.activeTabId = '/home'
    },

    /**
     * 设置当前激活标签
     * @param {string} tabId
     */
    setActiveTab(tabId) {
      this.activeTabId = tabId
    },

    /**
     * 设置侧边栏折叠状态
     * @param {boolean} collapsed
     */
    setSidebarCollapsed(collapsed) {
      this.sidebarCollapsed = collapsed
    },

    /**
     * 切换侧边栏折叠状态
     */
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    }
  }
})
