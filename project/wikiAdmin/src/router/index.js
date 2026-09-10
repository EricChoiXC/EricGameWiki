import { createRouter, createWebHistory } from 'vue-router'

// 路由 history base 为 "/"（AGENTS.md 第 5 章）
const router = createRouter({
  history: createWebHistory('/'),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/HomeView.vue'),
          meta: {
            title: '首页',
            closable: false, // 规范第 14 章：首页不可关闭
            keepAlive: true
          }
        },
        // -----------------------------------------------------------------
        // 用户与权限模块（docs/admin/用户和权限管理.md）
        // -----------------------------------------------------------------
        // 用户
        {
          path: 'admin/org/user',
          name: 'OrgUserList',
          component: () => import('@/views/org/user/UserListView.vue'),
          meta: { title: '用户', menuGroup: '用户与权限', keepAlive: true }
        },
        {
          path: 'admin/org/user/create',
          name: 'OrgUserCreate',
          component: () => import('@/views/org/user/UserEditView.vue'),
          meta: { title: '新建用户' }
        },
        {
          path: 'admin/org/user/edit/:id',
          name: 'OrgUserEdit',
          component: () => import('@/views/org/user/UserEditView.vue'),
          meta: { title: '编辑用户' }
        },
        {
          path: 'admin/org/user/detail/:id',
          name: 'OrgUserDetail',
          component: () => import('@/views/org/user/UserDetailView.vue'),
          meta: { title: '用户详情' }
        },
        // 角色
        {
          path: 'admin/org/role',
          name: 'OrgRoleList',
          component: () => import('@/views/org/role/RoleListView.vue'),
          meta: { title: '角色', menuGroup: '用户与权限', keepAlive: true }
        },
        {
          path: 'admin/org/role/create',
          name: 'OrgRoleCreate',
          component: () => import('@/views/org/role/RoleEditView.vue'),
          meta: { title: '新建角色' }
        },
        {
          path: 'admin/org/role/edit/:id',
          name: 'OrgRoleEdit',
          component: () => import('@/views/org/role/RoleEditView.vue'),
          meta: { title: '编辑角色' }
        },
        {
          path: 'admin/org/role/detail/:id',
          name: 'OrgRoleDetail',
          component: () => import('@/views/org/role/RoleDetailView.vue'),
          meta: { title: '角色详情' }
        },
        // 权限
        {
          path: 'admin/org/permission',
          name: 'OrgPermissionList',
          component: () => import('@/views/org/permission/PermissionListView.vue'),
          meta: { title: '权限', menuGroup: '用户与权限', keepAlive: true }
        },
        // 登录记录
        {
          path: 'admin/org/login-log',
          name: 'OrgLoginLogList',
          component: () => import('@/views/org/login-log/LoginLogListView.vue'),
          meta: { title: '登录记录', menuGroup: '用户与权限', keepAlive: true }
        },
        // 系统配置
        {
          path: 'admin/org/config',
          name: 'OrgSystemConfig',
          component: () => import('@/views/org/config/SystemConfigView.vue'),
          meta: { title: '系统配置', menuGroup: '用户与权限', keepAlive: true }
        },
        // -----------------------------------------------------------------
        // 附件模块（docs/admin/附件机制.md）
        // -----------------------------------------------------------------
        // 附件列表
        {
          path: 'admin/attachment',
          name: 'AttachmentList',
          component: () => import('@/views/attachment/AttachmentListView.vue'),
          meta: { title: '附件列表', menuGroup: '附件', keepAlive: true }
        },
        // 附件系统配置
        {
          path: 'admin/attachment/config',
          name: 'AttachmentConfig',
          component: () => import('@/views/attachment/AttachmentConfigView.vue'),
          meta: { title: '系统配置', menuGroup: '附件', keepAlive: true }
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

export default router
