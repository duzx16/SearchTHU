import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'
import Router from 'vue-router'
import Home from '@/views/Home'
import Search from '@/views/Search'
import AdvancedSearch from '@/views/AdvancedSearch'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

Vue.use(Router)
Vue.use(BootstrapVue)

export const router = new Router({
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home
    },
    {
      path: '/search/:query/:page',
      name: 'search',
      component: Search
    },
    {
      path: '/advanced_search',
      name: 'advanced-search',
      component: AdvancedSearch
    },
    {
      path: '*',
      redirect: '/'
    }
  ]
})
