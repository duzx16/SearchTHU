import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'
import Router from 'vue-router'
import Search from '@/views/Search'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

Vue.use(Router)
Vue.use(BootstrapVue)

export const router = new Router({
  routes: [
    {
      path: '/',
      name: 'search',
      component: Search
    },
    {
      path: '/:query/:page',
      name: 'search',
      component: Search
    },    
    {
      path: '*',
      redirect: '/'
    }
  ]
})
