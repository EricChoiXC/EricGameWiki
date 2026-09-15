<template>
  <div class="rich-editor">
    <Toolbar
      :editor="editorRef"
      :default-config="toolbarConfig"
      :mode="mode"
      class="rich-editor__toolbar"
    />
    <Editor
      v-model="content"
      :default-config="editorConfig"
      :mode="mode"
      class="rich-editor__body"
      style="height: 320px; overflow-y: hidden"
      @onCreated="handleCreated"
    />
  </div>
</template>

<script setup>
import { ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const props = defineProps({
  /** 富文本 HTML 字符串 */
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入内容'
  }
})

const emit = defineEmits(['update:modelValue'])

const mode = 'default'
const editorRef = shallowRef()

// 工具栏：移除视频与全屏，其余默认
const toolbarConfig = {
  excludeKeys: ['group-video', 'fullScreen']
}

const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {}
}

const content = ref(props.modelValue || '')

// 父组件值变化时回填（避免光标跳动）
watch(
  () => props.modelValue,
  (val) => {
    const next = val || ''
    if (next !== content.value) {
      content.value = next
    }
  }
)

// 内容变化时同步到 v-model
watch(content, (val) => {
  emit('update:modelValue', val)
})

function handleCreated(editor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
  }
  editorRef.value = null
})
</script>

<style lang="scss" scoped>
.rich-editor {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;

  &__toolbar {
    border-bottom: 1px solid var(--el-border-color);
  }

  &__body {
    background-color: var(--el-bg-color);
  }
}
</style>
