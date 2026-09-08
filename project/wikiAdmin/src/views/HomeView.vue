<template>
  <div class="home-view">
    <el-card class="home-view__card" shadow="never">
      <template #header>
        <h2 class="home-view__title">欢迎使用 EricGameWiki 管理后台</h2>
      </template>
      <p class="home-view__desc">
        公共样式规范遵循 docs/common/前端公共样式规范.md，技术栈为 Vue 3 + Vite + Element Plus。
      </p>
      <p class="home-view__desc">
        左侧菜单与顶部多标签页布局已就绪，可在此处接入业务模块。
      </p>
    </el-card>
  </div>
</template>

<script setup>
// 首页：固定首个标签，不可关闭（规范第 14 章）
</script>

<style lang="scss" scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);

  &__title {
    margin: 0;
    font-size: var(--font-size-h3);
    font-weight: var(--font-weight-bold);
    color: var(--color-text-primary);
  }

  &__desc {
    margin: var(--spacing-sm) 0;
    font-size: var(--font-size-body);
    color: var(--color-text-regular);
    line-height: var(--line-height-loose);
  }
}
</style>
