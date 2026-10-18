import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles/theme.css'
import './assets/styles/storefront.css'

createApp(App).use(createPinia()).use(router).mount('#app')
