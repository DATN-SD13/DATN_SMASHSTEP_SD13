import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
const proxy = { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true }, '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true } }
// Local screens should request the current build after a refresh.
const headers = { 'Cache-Control': 'no-store' }
export default defineConfig({ plugins: [vue()], server: { host: '127.0.0.1', port: 5174, strictPort: true, proxy, headers }, preview: { host: '127.0.0.1', port: 5174, strictPort: true, proxy, headers } })
