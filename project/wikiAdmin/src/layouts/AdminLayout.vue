<template>
  <div class="admin-layout">
    <!-- 顶部行（规范第 14 章：至多 120px 或 10% 页面高度） -->
    <!-- 左侧：系统 logo（默认系统名称图标）+ 系统名称；右侧：用户头像 + 操作（修改密码、登出） -->
    <header class="admin-layout__header">
      <div class="admin-layout__header-left">
        <el-icon class="admin-layout__header-logo" :size="24"><Platform /></el-icon>
        <span class="admin-layout__header-title">EricGameWiki 管理后台</span>
      </div>
      <div class="admin-layout__header-right">
        <el-dropdown>
          <span class="admin-layout__header-user">
            <el-avatar :size="28" class="admin-layout__header-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span>{{ userStore.nickname }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onChangePassword">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="onLogout">登出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 修改密码弹窗（docs/admin/用户和权限管理.md 业务逻辑第 5 条） -->
      <ChangePasswordDialog v-model:visible="passwordDialogVisible" />
    </header>

    <div class="admin-layout__body">
      <!-- 左侧菜单（规范第 14 章：至多 360px 或 20% 页面宽度） -->
      <aside
        class="admin-layout__sidebar"
        :class="{ 'admin-layout__sidebar--collapsed': appStore.sidebarCollapsed }"
      >
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          router
        >
          <el-menu-item index="/home" @click="onMenuClick('/home', '首页')">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <!-- 用户与权限（docs/admin/用户和权限管理.md 菜单栏） -->
          <el-sub-menu index="admin-org">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>用户与权限</span>
            </template>
            <el-menu-item index="/admin/org/user" @click="onMenuClick('/admin/org/user', '用户')">
              <el-icon><User /></el-icon>
              <template #title>用户</template>
            </el-menu-item>
            <el-menu-item index="/admin/org/role" @click="onMenuClick('/admin/org/role', '角色')">
              <el-icon><Avatar /></el-icon>
              <template #title>角色</template>
            </el-menu-item>
            <el-menu-item index="/admin/org/permission" @click="onMenuClick('/admin/org/permission', '权限')">
              <el-icon><Key /></el-icon>
              <template #title>权限</template>
            </el-menu-item>
            <el-menu-item index="/admin/org/login-log" @click="onMenuClick('/admin/org/login-log', '登录记录')">
              <el-icon><Document /></el-icon>
              <template #title>登录记录</template>
            </el-menu-item>
            <el-menu-item index="/admin/org/config" @click="onMenuClick('/admin/org/config', '系统配置')">
              <el-icon><Setting /></el-icon>
              <template #title>系统配置</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- 附件（docs/admin/附件机制.md 菜单栏） -->
          <el-sub-menu index="admin-attachment">
            <template #title>
              <el-icon><Paperclip /></el-icon>
              <span>附件</span>
            </template>
            <el-menu-item index="/admin/attachment" @click="onMenuClick('/admin/attachment', '附件列表')">
              <el-icon><Files /></el-icon>
              <template #title>附件列表</template>
            </el-menu-item>
            <el-menu-item index="/admin/attachment/config" @click="onMenuClick('/admin/attachment/config', '系统配置')">
              <el-icon><Setting /></el-icon>
              <template #title>系统配置</template>
            </el-menu-item>
          </el-sub-menu>

          <!-- wiki 系统（docs/admin/wiki/wiki系统.md 菜单栏） -->
          <el-sub-menu index="admin-wiki">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>wiki</span>
            </template>
            <el-menu-item index="/admin/wiki" @click="onMenuClick('/admin/wiki', '项目列表')">
              <el-icon><Notebook /></el-icon>
              <template #title>项目列表</template>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </aside>

      <!-- 右侧内容区：标签页 + 视图 -->
      <main class="admin-layout__content">
        <!-- 标签页行（规范第 14 章） -->
        <div class="admin-tabs">
          <div ref="tabsScrollRef" class="admin-tabs__scroll">
            <el-tabs
              v-model="appStore.activeTabId"
              type="card"
              closable
              @tab-click="onTabClick"
              @tab-remove="onTabRemove"
            >
              <el-tab-pane
                v-for="tab in appStore.tabs"
                :key="tab.id"
                :name="tab.id"
                :label="tab.title"
                :closable="tab.closable !== false"
              />
            </el-tabs>
          </div>

          <div class="admin-tabs__actions">
            <el-button
              link
              :icon="ArrowLeft"
              title="向左滚动"
              @click="scrollTabs(-200)"
            />
            <el-button
              link
              :icon="ArrowRight"
              title="向右滚动"
              @click="scrollTabs(200)"
            />
            <el-dropdown trigger="click" @command="onCommand">
              <el-button link :icon="MoreFilled" title="所有标签页" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="tab in appStore.tabs"
                    :key="tab.id"
                    :command="tab.id"
                  >
                    {{ tab.title }}
                  </el-dropdown-item>
                  <el-dropdown-item divided command="closeOthers">
                    关闭其他
                  </el-dropdown-item>
                  <el-dropdown-item command="closeAll">
                    关闭所有
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 实际视图内容 -->
        <div class="admin-layout__view">
          <RouterView v-slot="{ Component }">
            <KeepAlive>
              <component :is="Component" />
            </KeepAlive>
          </RouterView>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  Avatar,
  Collection,
  Document,
  Files,
  HomeFilled,
  Key,
  MoreFilled,
  Notebook,
  Paperclip,
  Platform,
  Setting,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/org'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const tabsScrollRef = ref(null)
const passwordDialogVisible = ref(false)

// 初始化首页标签
appStore.initHomeTab()

// 密码已过期登录（/home?forceChangePwd=1）：自动弹出修改密码弹窗
// docs/admin/用户和权限管理.md 业务逻辑第 5 条
if (route.query.forceChangePwd === '1') {
  passwordDialogVisible.value = true
  ElMessage.warning('密码已过期，请先修改密码')
}

// 当前激活菜单
const activeMenu = computed(() => appStore.activeTabId)

// 路由变化时同步到标签页
watch(
  () => route.path,
  (path) => {
    appStore.addTab({
      id: path,
      title: route.meta?.title || path,
      path,
      closable: route.meta?.closable !== false
    })
  },
  { immediate: true }
)

// 点击菜单项 → 跳转
function onMenuClick(path, title) {
  router.push(path)
}

// 点击标签页 → 路由跳转
function onTabClick(pane) {
  const tab = appStore.tabs.find((t) => t.id === pane.paneName)
  if (tab && tab.path !== route.path) {
    router.push(tab.path)
  }
}

// 关闭标签页
function onTabRemove(name) {
  const closing = appStore.tabs.find((t) => t.id === name)
  appStore.closeTab(name)
  // 若关闭的是当前激活，跳转到新激活的标签
  const next = appStore.tabs.find((t) => t.id === appStore.activeTabId)
  if (next && next.path !== route.path) {
    router.push(next.path)
  }
  // eslint-disable-next-line no-unused-vars
  const _ = closing
}

// 三点下拉菜单：点击具体标签 / 关闭操作
function onCommand(command) {
  if (command === 'closeOthers') {
    appStore.closeOthers()
    return
  }
  if (command === 'closeAll') {
    appStore.closeAll()
    router.push('/home')
    return
  }
  // 否则视为标签 id，激活该标签
  const tab = appStore.tabs.find((t) => t.id === command)
  if (tab) {
    appStore.setActiveTab(tab.id)
    router.push(tab.path)
  }
}

// 横向滚动标签容器
function scrollTabs(delta) {
  const el = tabsScrollRef.value
  if (!el) return
  el.scrollLeft += delta
}

// 修改密码：弹出修改密码弹窗（docs/admin/用户和权限管理.md 业务逻辑第 5 条）
function onChangePassword() {
  passwordDialogVisible.value = true
}

// 登出：调用后端登出接口并清空本地登录态，回到登录页
function onLogout() {
  ElMessageBox.confirm('确定要登出吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      // 后端登出失败不阻塞本地登出（如 token 已过期）
      authApi.logout().catch(() => {})
      userStore.logout()
      appStore.closeAll()
      router.push('/login')
    })
    .catch(() => {})
}

// 确保激活标签滚动到可见区域
watch(
  () => appStore.activeTabId,
  () => {
    nextTick(() => {
      const container = tabsScrollRef.value
      if (!container) return
      const active = container.querySelector('.is-active')
      if (active) {
        active.scrollIntoView({ inline: 'nearest', block: 'nearest', behavior: 'smooth' })
      }
    })
  }
)
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background-color: var(--color-bg-page);

  &__header {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: min(120px, 10vh);
    padding: 0 var(--spacing-xl);
    background-color: var(--color-bg-primary);
    border-bottom: 1px solid var(--color-border-lighter);
    box-shadow: var(--shadow-sm);
    z-index: var(--z-index-sticky);
  }

  &__header-left,
  &__header-right {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
  }

  &__header-title {
    font-size: var(--font-size-h3);
    font-weight: var(--font-weight-bold);
    color: var(--color-text-primary);
  }

  &__header-logo {
    color: var(--color-primary);
  }

  &__header-avatar {
    background-color: var(--color-primary-light-9);
    color: var(--color-primary);
  }

  &__header-user {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-sm);
    cursor: pointer;
    color: var(--color-text-regular);
  }

  &__body {
    flex: 1 1 auto;
    display: flex;
    overflow: hidden;
  }

  &__sidebar {
    flex: 0 0 auto;
    width: min(360px, 20vw);
    background-color: var(--color-bg-primary);
    border-right: 1px solid var(--color-border-lighter);
    overflow-x: hidden;
    transition: width var(--transition-base);

    &--collapsed {
      width: 64px;
    }
  }

  &__content {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  &__view {
    flex: 1 1 auto;
    overflow: auto;
    padding: var(--spacing-lg);
  }
}

// 标签页行
.admin-tabs {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 0 var(--spacing-md);
  background-color: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-lighter);

  &__scroll {
    flex: 1 1 auto;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }

    // 让 el-tabs 横向不被截断
    :deep(.el-tabs) {
      --el-tabs-header-height: 40px;
    }

    :deep(.el-tabs__header) {
      margin: 0;
    }

    :deep(.el-tabs__nav-wrap::after) {
      display: none;
    }

    :deep(.el-tabs__item) {
      height: 32px;
      line-height: 32px;
      font-size: var(--font-size-body);
    }
  }

  &__actions {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    gap: var(--spacing-xs);
  }
}
</style>
