import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 把 Vite/vitest 的临时缓存固定在项目内，避免沙箱或受限环境下
  // 写系统临时目录触发 EPERM（CI 与 U 盘拷贝场景同样受益）
  cacheDir: 'node_modules/.vite-vitest',
  test: {
    environment: 'node',
    include: ['tests/**/*.spec.js'],
    reporters: 'default',
    cache: false,
    // 用例规模很小，顺序执行即可；也避免多 worker 并发写 vite-node 转换缓存
    fileParallelism: false
  }
})
