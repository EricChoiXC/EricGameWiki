<template>
  <div class="wiki-main">
    <!-- 筛选区：关键字 + 数据项类型（docs/wiki/项目首页.md） -->
    <el-card class="wiki-main__card" shadow="never">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            placeholder="数据项名称 / 数据项简称"
            clearable
            style="width: 280px"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </el-form-item>
        <el-form-item label="数据项类型">
          <el-select
            v-model="fieldDataType"
            placeholder="全部"
            clearable
            style="width: 160px"
            @change="onSearch"
          >
            <el-option
              v-for="opt in WIKI_DATA_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区：不分页显示查询结果 -->
    <el-card class="wiki-main__card" shadow="never">
      <el-table
        v-loading="loading"
        :data="dataItems"
        border
        @row-click="onRowClick"
      >
        <el-table-column prop="fieldName" label="数据项名称" min-width="200" />
        <el-table-column prop="fieldDataName" label="数据项简称" min-width="200" />
        <el-table-column label="数据项类型" min-width="120">
          <template #default="{ row }">
            {{ dataTypeLabel(row.fieldDataType) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { wikiDataItemApi } from '@/api/wiki'
import { WIKI_DATA_TYPE_OPTIONS } from '@/utils/constants'

const route = useRoute()
const router = useRouter()

const simpleName = computed(() => route.params.simpleName || '')

const keyword = ref('')
const fieldDataType = ref('')
const dataItems = ref([])
const loading = ref(false)

function dataTypeLabel(type) {
  return WIKI_DATA_TYPE_OPTIONS.find((opt) => opt.value === type)?.label || type || '-'
}

// 关键字 + 数据项类型筛选，不分页
async function loadDataItems() {
  loading.value = true
  try {
    const res = await wikiDataItemApi.list(simpleName.value, {
      keyword: keyword.value,
      fieldDataType: fieldDataType.value
    })
    dataItems.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  loadDataItems()
}

// 行点击进入数据项列表页
function onRowClick(row) {
  if (row.fieldDataName) {
    router.push(`/wiki/${simpleName.value}/${row.fieldDataName}`)
  }
}

onMounted(loadDataItems)
</script>

<style lang="scss" scoped>
.wiki-main {
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
