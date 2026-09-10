<template>
  <div class="system-config">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="system-config__header">
          <span>附件系统配置</span>
          <el-button
            v-if="hasAttachmentPermission"
            type="primary"
            :loading="saving"
            @click="onSave"
          >
            保存
          </el-button>
        </div>
      </template>

      <el-form label-width="140px" style="max-width: 720px">
        <el-form-item
          v-for="item in ATTACHMENT_CONFIG_ITEMS"
          :key="item.key"
          :label="item.label"
        >
          <div class="system-config__item">
            <!-- 数值类型 -->
            <el-input-number
              v-if="item.type === 'number'"
              v-model="configValues[item.key]"
              :min="item.min ?? 0"
              controls-position="right"
            />
            <!-- 文本类型 -->
            <el-input
              v-else
              v-model="configValues[item.key]"
              :placeholder="`请输入${item.label}`"
              style="max-width: 320px"
            />
            <p class="system-config__desc">{{ item.description }}</p>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi } from '@/api/org'
import { useUserStore } from '@/stores/user'
import { ATTACHMENT_CONFIG_ITEMS, PERMISSION_CODE } from '@/utils/constants'

const userStore = useUserStore()
const hasAttachmentPermission = userStore.hasPermission(PERMISSION_CODE.ATTACHMENT_ADMIN)

const loading = ref(false)
const saving = ref(false)

// 配置项值集合，key 为配置项 key
const configValues = reactive({})

// 初始化默认值
ATTACHMENT_CONFIG_ITEMS.forEach((item) => {
  configValues[item.key] = item.defaultValue
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await configApi.init()
    const list = res.list || []
    list.forEach((item) => {
      if (item.fieldCode && configValues.hasOwnProperty(item.fieldCode)) {
        const meta = ATTACHMENT_CONFIG_ITEMS.find((c) => c.key === item.fieldCode)
        let val = item.fieldValue
        if (meta?.type === 'number') {
          val = val == null || val === '' ? meta.defaultValue : Number(val)
        }
        configValues[item.fieldCode] = val
      }
    })
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const list = ATTACHMENT_CONFIG_ITEMS.map((item) => {
      let val = configValues[item.key]
      if (item.type === 'number') {
        val = String(val ?? 0)
      } else {
        val = val == null ? '' : String(val)
      }
      return { fieldKey: item.key, fieldValue: val }
    })
    await configApi.saveBatch(list)
    ElMessage.success('保存成功')
    loadConfig()
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style lang="scss" scoped>
.system-config {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__item {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  &__desc {
    margin: 0;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    line-height: var(--line-height-base);
  }
}
</style>
