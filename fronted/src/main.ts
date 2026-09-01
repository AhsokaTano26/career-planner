import { createApp } from 'vue'
import App from './App.vue'
import './assets/styles/styles.css'
import './assets/styles/functional.css'
import './assets/styles/lists.css'
import './assets/styles/logout.css'
import './assets/styles/cqu-theme.css'
import { installCustomSelects } from './plugins/customSelect'

createApp(App).mount('#app')
installCustomSelects()
