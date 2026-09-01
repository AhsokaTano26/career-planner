import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles/styles.css'
import './assets/styles/functional.css'
import './assets/styles/lists.css'
import './assets/styles/logout.css'
import './assets/styles/cqu-theme.css'
import { installCustomSelects } from './plugins/customSelect'

const app = createApp(App)
app.use(router)
app.mount('#app')
installCustomSelects()
