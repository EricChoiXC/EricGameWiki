<template>
  <div class="wiki-layout">
    <!-- 顶部公共布局（docs/wiki/wiki访问.md：固定占 80px 高） -->
    <header class="wiki-layout__header">
      <div class="wiki-layout__header-left">
        <template v-if="isHome">
          <el-icon class="wiki-layout__logo" :size="22"><Reading /></el-icon>
          <span class="wiki-layout__brand">WIKI</span>
        </template>
        <template v-else>
          <el-icon class="wiki-layout__logo" :size="22"><Reading /></el-icon>
          <span class="wiki-layout__brand">{{ wikiStore.projectName }}</span>
        </template>
      </div>

      <!-- 中间：项目内页面显示项目数据项下拉菜单 -->
      <div class="wiki-layout__header-center">
        <el-dropdown v-if="!isHome && dataItems.length > 0" @command="onDataItemCommand">
          <span class="wiki-layout__nav-trigger">
            {{ wikiStore.projectName }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="item in dataItems"
                :key="item.fieldId"
                :command="item.fieldDataName"
              >
                {{ item.fieldName }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 右侧：首页按钮 -->
      <div class="wiki-layout__header-right">
        <el-button link @click="goHome">首页</el-button>
      </div>
    </header>

    <!-- 中间：页面内容，可滚动 -->
    <main class="wiki-layout__body">
      <RouterView />
    </main>

    <!-- 底部公共布局（docs/wiki/wiki访问.md：固定占 80px 高，中间显示网站注册信息） -->
    <footer class="wiki-layout__footer">
      <span class="wiki-layout__copyright">EricGameWiki 版权所有</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Reading } from '@element-plus/icons-vue'
import { useWikiStore } from '@/stores/wiki'
import { wikiMainApi, wikiDataItemApi } from '@/api/wiki'

const route = useRoute()
const router = useRouter()
const wikiStore = useWikiStore()

const isHome = computed(() => route.path === '/home')

// 数据项下拉项由 store 提供（WikiLayout 已加载项目上下文）
const dataItems = computed(() => wikiStore.dataItems)

// 项目上下文随 fieldSimpleName 变化加载；相同项目内切换页面不重复请求
watch(
  () => route.params.simpleName,
  async (simpleName) => {
    if (!simpleName) {
      wikiStore.clear()
      return
    }
    if (wikiStore.simpleName === simpleName && wikiStore.project) {
      return
    }
    try {
      const [projectRes, itemsRes] = await Promise.all([
        wikiMainApi.load(simpleName),
        wikiDataItemApi.list(simpleName)
      ])
      wikiStore.setContext({
        simpleName,
        project: projectRes.data,
        dataItems: itemsRes.list
      })
    } catch {
      // 项目不存在或已停用：清空上下文，避免残留上一项目信息，由页面自身处理
      wikiStore.clear()
    }
  },
  { immediate: true }
)

// 数据项下拉跳转
function onDataItemCommand(fieldDataName) {
  const simpleName = route.params.simpleName
  if (simpleName && fieldDataName) {
    router.push(`/wiki/${simpleName}/${fieldDataName}`)
  }
}

// 返回首页
function goHome() {
  router.push('/home')
}
</script>

<style lang="scss" scoped>
.wiki-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background-color: var(--color-bg-page);

  &__header {
    flex: 0 0 80px;
    display: flex;
    align-items: center;
    padding: 0 var(--spacing-xl);
    background-color: var(--color-bg-primary);
    border-bottom: 1px solid var(--color-border-lighter);
    box-shadow: var(--shadow-sm);
    z-index: var(--z-index-sticky);
  }

  &__header-left,
  &__header-center,
  &__header-right {
    flex: 1;
    display: flex;
    align-items: center;
  }

  &__header-left {
    justify-content: flex-start;
    gap: var(--spacing-sm);
  }

  &__header-center {
    justify-content: center;
  }

  &__header-right {
    justify-content: flex-end;
  }

  &__logo {
    color: var(--color-primary);
  }

  &__brand {
    font-size: var(--font-size-h3);
    font-weight: var(--font-weight-bold);
    color: var(--color-text-primary);
  }

  &__nav-trigger {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-xs);
    font-size: var(--font-size-body);
    color: var(--color-text-regular);
    cursor: pointer;
    outline: none;
  }

  &__body {
    flex: 1 1 auto;
    overflow: auto;
    padding: var(--spacing-xl);
  }

  &__footer {
    flex: 0 0 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--color-bg-primary);
    border-top: 1px solid var(--color-border-lighter);
  }

  &__copyright {
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
  }
}
</style>
