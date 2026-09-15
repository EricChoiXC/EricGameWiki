<template>
  <div class="simple-selector">
    <!-- 触发输入框：展示已选名称，点击打开选择弹窗 -->
    <el-input
      :model-value="displayText"
      :placeholder="placeholder"
      readonly
      class="simple-selector__trigger"
      @click="handleOpen"
    >
      <template #suffix>
        <el-icon
          v-if="displayText"
          class="simple-selector__trigger-clear"
          @click.stop="handleClear"
        >
          <CircleClose />
        </el-icon>
        <el-icon v-else class="simple-selector__trigger-icon">
          <Search />
        </el-icon>
      </template>
    </el-input>

    <!-- 选择弹窗：查询条件 + 列表 + 分页 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      append-to-body
      class="simple-selector__dialog"
    >
      <div class="simple-selector__filter">
        <el-form inline @submit.prevent>
          <el-form-item v-for="item in filter" :key="item.name" :label="item.text">
            <el-input
              v-model="filterValues[item.name]"
              :placeholder="`请输入${item.text}`"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        :row-key="rowKey"
        height="320"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      >
        <el-table-column v-if="multi" type="selection" width="46" reserve-selection />
        <el-table-column v-else width="46" align="center">
          <template #default="{ row }">
            <el-radio
              :model-value="currentId"
              :label="String(row[ID_KEY])"
              @update:model-value="currentId = $event"
            >
              <span />
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column
          v-for="col in filter"
          :key="col.name"
          :prop="col.name"
          :label="col.text"
          min-width="140"
          show-overflow-tooltip
        />
      </el-table>

      <div class="simple-selector__pagination">
        <el-pagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :total="page.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="handleSizeChange"
        />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="hasSelection" @click="handleClear">清空</el-button>
        <el-button type="primary" :disabled="!hasSelection" @click="handleConfirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { CircleClose, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/api/request'
import { buildListQuery, like } from '@/utils/query'

/**
 * 普通选择器组件（Simple-Selector）
 * 契约来源：docs/common/前端公共组件.md 三.1
 * - 基于数据库来源数据（url 列表查询接口）进行单选/多选
 * - 通过 v-model:field-id / v-model:field-name 双向绑定选择结果
 * - 行数据按仓库数据库公共字段约定读取 fieldId / fieldName（AGENTS.md §7.4）
 */

/** 行数据中 id / 名称 字段名（对齐 field_id / field_name 公共字段的 JSON 小驼峰） */
const ID_KEY = 'fieldId'
const NAME_KEY = 'fieldName'

const props = defineProps({
  /** 关联的 id 值字段，必填（v-model:field-id 绑定选择结果） */
  fieldId: {
    type: String,
    default: ''
  },
  /** 关联的名称值字段，必填（v-model:field-name 绑定选择结果） */
  fieldName: {
    type: String,
    default: ''
  },
  /** 请求的列表查询接口 url，必填（POST /list 报文，见 utils/query.js buildListQuery） */
  url: {
    type: String,
    required: true
  },
  /** 可筛选项：[{ text: '查询的中文名', name: '查询的字段名称' }] */
  filter: {
    type: Array,
    default: () => [{ text: '名称', name: 'fieldName' }]
  },
  /** 是否多选，默认 false */
  multi: {
    type: Boolean,
    default: false
  },
  /** 多选的分隔符，默认 ';'，仅限符号 */
  septarator: {
    type: String,
    default: ';'
  },
  /** 点击确认选择后的回调函数 */
  callback: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['update:fieldId', 'update:fieldName'])

const dialogVisible = ref(false)
const loading = ref(false)
const tableRef = ref(null)
const tableData = ref([])
const currentId = ref('')
const selectedRows = ref([])
const selectedIds = ref(new Set())
const page = reactive({ pageNum: 1, pageSize: 15, total: 0 })
const filterValues = reactive({})

// id -> 名称 映射：跨页保留已选项名称，确认时按 id 还原名称
const nameMap = new Map()

const displayText = computed(() => props.fieldName || '')
const placeholder = computed(() => (props.multi ? '请选择（可多选）' : '请选择'))
const dialogTitle = computed(() => (props.multi ? '选择（可多选）' : '选择'))
const hasSelection = computed(() =>
  props.multi ? selectedIds.value.size > 0 : !!currentId.value
)

function rowKey(row) {
  return row[ID_KEY]
}

/** 打开弹窗：初始化查询条件与已选状态后加载列表 */
function handleOpen() {
  // 以已绑定的值初始化已选项
  currentId.value = props.multi ? '' : String(props.fieldId || '')
  selectedIds.value = new Set(
    String(props.fieldId || '')
      .split(props.septarator)
      .map((s) => s.trim())
      .filter(Boolean)
  )
  nameMap.clear()
  String(props.fieldName || '')
    .split(props.septarator)
    .forEach((name, index) => {
      const ids = [...selectedIds.value]
      if (ids[index]) nameMap.set(ids[index], name.trim())
    })
  dialogVisible.value = true
  loadList()
}

/** 加载列表：filter 中填写的条件按 like 拼接进 and 条件 */
async function loadList() {
  loading.value = true
  try {
    const conditions = props.filter.map((item) => like(item.name, filterValues[item.name] || ''))
    const res = await request.post(
      props.url,
      buildListQuery(conditions, {
        pageNum: page.pageNum,
        pageSize: page.pageSize,
        needPage: true,
        sortField: ID_KEY,
        sortOrder: 'desc'
      })
    )
    tableData.value = res.list || []
    page.total = res.query?.total ?? 0
    props.filter.forEach((item) => {
      tableData.value.forEach((row) => {
        if (row[item.name] != null && row[item.name] !== '') {
          nameMap.set(String(row[ID_KEY]), String(row[NAME_KEY] ?? row[item.name]))
        }
      })
    })
    await syncSelection()
  } catch {
    tableData.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

/** 多选：加载后按已选 id 回填勾选；单选：跨页按已选 id 回填 radio */
async function syncSelection() {
  await nextTick()
  if (!tableRef.value) return
  if (props.multi) {
    tableData.value.forEach((row) => {
      const selected = selectedIds.value.has(String(row[ID_KEY]))
      tableRef.value.toggleRowSelection(row, selected)
    })
  }
}

function handleSearch() {
  page.pageNum = 1
  loadList()
}

function handleReset() {
  props.filter.forEach((item) => {
    filterValues[item.name] = ''
  })
  handleSearch()
}

function handleSizeChange() {
  page.pageNum = 1
  loadList()
}

/** 单选：点击行或 radio 记录当前选中 */
function handleRowClick(row) {
  if (props.multi) return
  currentId.value = String(row[ID_KEY])
}

/** 多选：同步勾选结果 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = new Set(rows.map((row) => String(row[ID_KEY])))
  rows.forEach((row) => nameMap.set(String(row[ID_KEY]), String(row[NAME_KEY] ?? '')))
}

/** 确认：按 id 顺序组装名称，emit 更新并触发 callback */
function handleConfirm() {
  const ids = props.multi ? [...selectedIds.value] : currentId.value ? [currentId.value] : []
  if (ids.length === 0) return
  const names = ids.map((id) => {
    const hit = tableData.value.find((row) => String(row[ID_KEY]) === id)
    return String(hit ? hit[NAME_KEY] ?? '' : nameMap.get(id) ?? '')
  })
  const idText = ids.join(props.septarator)
  const nameText = names.join(props.septarator)
  emit('update:fieldId', idText)
  emit('update:fieldName', nameText)
  if (typeof props.callback === 'function') {
    props.callback({ fieldId: idText, fieldName: nameText, rows: selectedRows.value })
  }
  dialogVisible.value = false
}

/** 清空选择 */
function handleClear() {
  emit('update:fieldId', '')
  emit('update:fieldName', '')
  if (typeof props.callback === 'function') {
    props.callback({ fieldId: '', fieldName: '', rows: [] })
  }
  dialogVisible.value = false
}
</script>

<style lang="scss" scoped>
.simple-selector {
  width: 100%;

  &__trigger {
    cursor: pointer;

    :deep(.el-input__inner) {
      cursor: pointer;
    }
  }

  &__trigger-icon,
  &__trigger-clear {
    cursor: pointer;
  }

  &__filter {
    :deep(.el-form-item) {
      margin-bottom: var(--spacing-md);
    }
  }

  &__pagination {
    margin-top: var(--spacing-md);
    display: flex;
    justify-content: flex-end;
  }
}
</style>
