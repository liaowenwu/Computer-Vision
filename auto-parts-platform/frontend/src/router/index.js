import { createRouter, createWebHistory } from 'vue-router'
import ProductView from '../views/ProductView.vue'
import SupplierView from '../views/SupplierView.vue'
import SyncView from '../views/SyncView.vue'

const routes = [
  { path: '/', redirect: '/products' },
  { path: '/products', component: ProductView },
  { path: '/suppliers', component: SupplierView },
  { path: '/sync', component: SyncView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
