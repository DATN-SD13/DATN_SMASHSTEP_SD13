import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  },
  '/uploads/products': {
    target: 'http://localhost:8080',
    changeOrigin: true
  },
  '/uploads/avatars': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}

export default defineConfig({
  plugins: [vue()],
  server: { host: '127.0.0.1', proxy: apiProxy },
  preview: { host: '127.0.0.1', proxy: apiProxy }
})
