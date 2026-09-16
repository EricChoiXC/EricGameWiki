import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
// 通用约束遵循 docs/common/前端公共样式规范.md 与 AGENTS.md 第 5 章：
// - Vite base 为 "/"，前端页面通过裸根路径访问
// - Axios baseURL 为 "/api/v1/wiki"，仅 API 请求走该前缀
// - 沿用 Element Plus 默认主题，不通过 SCSS 主题变量覆盖
export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5181,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8086',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})
