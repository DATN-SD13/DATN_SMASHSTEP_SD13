import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
const proxy = { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true }, '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true } }
export default defineConfig({ plugins: [vue()], server: { host: '127.0.0.1', port: 5174, strictPort: true, proxy }, preview: { host: '127.0.0.1', port: 5174, strictPort: true, proxy } })
