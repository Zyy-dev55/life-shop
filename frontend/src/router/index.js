import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/UserLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'list', name: 'list', component: () => import('@/views/ProductList.vue'), meta: { title: '全部商品' } },
      { path: 'product/:id', name: 'product', component: () => import('@/views/ProductDetail.vue'), meta: { title: '商品详情' } },
      { path: 'cart', name: 'cart', component: () => import('@/views/Cart.vue'), meta: { title: '购物车', auth: true } },
      { path: 'checkout', name: 'checkout', component: () => import('@/views/Checkout.vue'), meta: { title: '确认订单', auth: true } },
      { path: 'orders', name: 'orders', component: () => import('@/views/OrderList.vue'), meta: { title: '我的订单', auth: true } },
      { path: 'consult', name: 'consult', component: () => import('@/views/Consult.vue'), meta: { title: '我的咨询', auth: true } },
      { path: 'order/:id', name: 'orderDetail', component: () => import('@/views/OrderDetail.vue'), meta: { title: '订单详情', auth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心', auth: true } },
      { path: 'address', name: 'address', component: () => import('@/views/Address.vue'), meta: { title: '收货地址', auth: true } },
      { path: 'coupons', name: 'coupons', component: () => import('@/views/CouponCenter.vue'), meta: { title: '优惠券', auth: true } },
      { path: 'after-sale', name: 'afterSale', component: () => import('@/views/AfterSale.vue'), meta: { title: '我的售后', auth: true } }
    ]
  },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { title: '注册' } },
  {
    path: '/merchant',
    component: () => import('@/layout/MerchantLayout.vue'),
    meta: { merchant: true },
    children: [
      { path: '', redirect: '/merchant/dashboard' },
      { path: 'dashboard', name: 'merchantDashboard', component: () => import('@/views/merchant/Dashboard.vue'), meta: { title: '经营概览' } },
      { path: 'products', name: 'merchantProducts', component: () => import('@/views/merchant/ProductManage.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'merchantOrders', component: () => import('@/views/merchant/OrderManage.vue'), meta: { title: '订单管理' } },
      { path: 'reviews', name: 'merchantReviews', component: () => import('@/views/merchant/ReviewManage.vue'), meta: { title: '评价管理' } },
      { path: 'consults', name: 'merchantConsults', component: () => import('@/views/merchant/ConsultManage.vue'), meta: { title: '客户咨询' } },
      { path: 'wallet', name: 'merchantWallet', component: () => import('@/views/merchant/Wallet.vue'), meta: { title: '钱包与提现' } },
      { path: 'after-sale', name: 'merchantAfterSale', component: () => import('@/views/merchant/AfterSaleManage.vue'), meta: { title: '售后处理' } },
      { path: 'setting', name: 'merchantSetting', component: () => import('@/views/merchant/ShopSetting.vue'), meta: { title: '店铺设置' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'adminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'products', name: 'adminProducts', component: () => import('@/views/admin/ProductManage.vue'), meta: { title: '商品管理' } },
      { path: 'categories', name: 'adminCategories', component: () => import('@/views/admin/CategoryManage.vue'), meta: { title: '分类管理' } },
      { path: 'orders', name: 'adminOrders', component: () => import('@/views/admin/OrderManage.vue'), meta: { title: '订单管理' } },
      { path: 'reviews', name: 'adminReviews', component: () => import('@/views/admin/ReviewManage.vue'), meta: { title: '评价管理' } },
      { path: 'consults', name: 'adminConsults', component: () => import('@/views/admin/ConsultManage.vue'), meta: { title: '聊天监管' } },
      { path: 'users', name: 'adminUsers', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'merchants', name: 'adminMerchants', component: () => import('@/views/admin/MerchantManage.vue'), meta: { title: '商家管理' } },
      { path: 'withdraws', name: 'adminWithdraws', component: () => import('@/views/admin/WithdrawManage.vue'), meta: { title: '提现审核' } },
      { path: 'after-sale', name: 'adminAfterSale', component: () => import('@/views/admin/AfterSaleManage.vue'), meta: { title: '售后监管' } },
      { path: 'sec-ops', name: 'adminSecOps', component: () => import('@/views/admin/SecOps.vue'), meta: { title: '网络安全运维监控' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · 悦家生活` : '悦家生活 · 生活用品商城'
  const userStore = useUserStore()

  if (to.meta.auth && !userStore.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.matched.some((record) => record.meta.merchant)) {
    if (!userStore.isLogin) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    if (!userStore.isMerchant) {
      ElMessage.warning('该页面仅商家账号可访问')
      next({ path: '/' })
      return
    }
  }

  if (to.matched.some((record) => record.meta.admin)) {
    if (!userStore.isLogin) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    if (!userStore.isAdmin) {
      next({ path: '/' })
      return
    }
  }

  next()
})

export default router
