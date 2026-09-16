import { createRouter, createWebHistory } from 'vue-router'

// 路由 history base 为 "/"（AGENTS.md 第 5 章）
// 前台 wiki 无登录校验（docs/wiki/wiki访问.md），不做登录守卫
const router = createRouter({
  history: createWebHistory('/'),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/WikiLayout.vue'),
      children: [
        // 首页（docs/wiki/首页.md）
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '首页' }
        },
        // 项目首页（docs/wiki/项目首页.md）
        {
          path: 'wiki/:simpleName',
          name: 'WikiMain',
          component: () => import('@/views/wiki/WikiMainView.vue'),
          meta: { title: '项目首页' }
        },
        // 数据项列表页（docs/wiki/图鉴类数据项页面.md / docs/wiki/文档类页面.md）
        {
          path: 'wiki/:simpleName/:dataName',
          name: 'WikiDataList',
          component: () => import('@/views/wiki/WikiDataListView.vue'),
          meta: { title: '数据项列表' }
        },
        // 数据项详情页（docs/wiki/图鉴类数据项页面.md / docs/wiki/文档类页面.md）
        {
          path: 'wiki/:simpleName/:dataName/:fieldId',
          name: 'WikiRecordDetail',
          component: () => import('@/views/wiki/WikiRecordDetailView.vue'),
          meta: { title: '数据项详情' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue')
    }
  ]
})

// 路由切换时同步浏览器标签标题
router.afterEach((to) => {
  const title = to.meta?.title
  document.title = title ? `${title} - EricGameWiki` : 'EricGameWiki'
})

export default router
