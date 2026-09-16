<template>
  <div class="home-view">
    <!-- 筛选区：仅提供"关键字"筛选（docs/wiki/首页.md） -->
    <el-card class="home-view__card" shadow="never">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            placeholder="中文名称 / 英文名称 / 日文名称 / 简称"
            clearable
            style="width: 320px"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区：不分页显示查询结果 -->
    <el-card class="home-view__card" shadow="never">
      <el-table
        v-loading="loading"
        :data="projects"
        border
        @row-click="onRowClick"
      >
        <el-table-column prop="fieldName" label="中文名称" min-width="200" />
        <el-table-column prop="fieldEnName" label="英文名称" min-width="200" />
        <el-table-column prop="fieldJpName" label="日文名称" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { wikiMainApi } from '@/api/wiki'

const router = useRouter()

const keyword = ref('')
const projects = ref([])
const loading = ref(false)

// 关键字筛选：后端模糊匹配中文/英文/日文名称与简称，不分页
async function loadProjects() {
  loading.value = true
  try {
    const res = await wikiMainApi.list(keyword.value)
    projects.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  loadProjects()
}

// 行点击进入项目首页
function onRowClick(row) {
  if (row.fieldSimpleName) {
    router.push(`/wiki/${row.fieldSimpleName}`)
  }
}

onMounted(loadProjects)
</script>

<style lang="scss" scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);

  &__card {
    :deep(.el-card__header) {
      font-weight: var(--font-weight-bold);
    }
  }
}
</style>
