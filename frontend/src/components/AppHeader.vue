<template>
  <div class="site-header">
    <div class="topbar">
      <div class="container topbar-inner">
        <span class="slogan">🚚 满 99 元包邮 · 30 天无忧退换 · 正品保障</span>
        <div class="topbar-right">
          <template v-if="!userStore.isLogin">
            <router-link class="top-link" to="/login">登录</router-link>
            <span class="divider">|</span>
            <router-link class="top-link" to="/register">注册</router-link>
          </template>
          <template v-else>
            <span>你好，{{ userStore.nickname }}</span>
            <span class="divider">|</span>
            <router-link v-if="userStore.isAdmin" class="top-link" to="/admin">管理后台</router-link>
            <span v-if="userStore.isAdmin" class="divider">|</span>
            <router-link v-if="userStore.isMerchant" class="top-link" to="/merchant">商家中心</router-link>
            <span v-if="userStore.isMerchant" class="divider">|</span>
            <a class="top-link" href="javascript:;" @click="handleLogout">退出</a>
          </template>
        </div>
      </div>
    </div>

    <div class="container header-main">
      <router-link to="/" class="brand">
        <span class="brand-mark">悦</span>
        <span class="brand-text">
          <strong>悦家生活</strong>
          <em>让日常更舒服一点</em>
        </span>
      </router-link>

      <nav class="nav">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/list" class="nav-item">全部商品</router-link>
        <router-link to="/orders" class="nav-item">我的订单</router-link>
        <router-link v-if="userStore.isLogin" to="/after-sale" class="nav-item">售后</router-link>
        <router-link v-if="userStore.isLogin" to="/coupons" class="nav-item">优惠券</router-link>
        <router-link v-if="userStore.isLogin" to="/consult" class="nav-item">
          我的咨询
          <span v-if="consultUnread" class="nav-badge">{{ consultUnread }}</span>
        </router-link>
      </nav>

      <div class="search">
        <el-input
          v-model="keyword"
          placeholder="搜索锅具 / 收纳 / 清洁好物"
          clearable
          @keyup.enter="doSearch"
        >
          <template #append>
            <el-button @click="doSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <div class="actions">
        <router-link to="/cart" class="cart-btn">
          <el-badge :value="cartStore.count" :hidden="!cartStore.count" :max="99">
            <el-icon :size="20"><ShoppingCart /></el-icon>
          </el-badge>
          <span>购物车</span>
        </router-link>

        <el-dropdown v-if="userStore.isLogin" trigger="hover" @command="onCommand">
          <span class="user-chip">
            <el-avatar :size="28" :src="userStore.user && userStore.user.avatar">
              {{ userStore.nickname.slice(0, 1) }}
            </el-avatar>
            <span class="user-name">{{ userStore.nickname }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="orders">我的订单</el-dropdown-item>
              <el-dropdown-item command="after-sale">我的售后</el-dropdown-item>
              <el-dropdown-item command="coupons">我的优惠券</el-dropdown-item>
              <el-dropdown-item command="consult">
                我的咨询
                <span v-if="consultUnread" class="mini-badge">{{ consultUnread }}</span>
              </el-dropdown-item>
              <el-dropdown-item command="address">收货地址</el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
              <el-dropdown-item v-if="userStore.isMerchant" command="merchant" divided>商家中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <router-link v-else to="/login" class="login-btn">登录 / 注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { consultApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const keyword = ref('')
const consultUnread = ref(0)

const doSearch = () => {
  router.push({ path: '/list', query: keyword.value ? { keyword: keyword.value } : {} })
}

const loadConsultUnread = async () => {
  if (!userStore.isLogin) {
    consultUnread.value = 0
    return
  }
  try {
    consultUnread.value = await consultApi.unread()
  } catch (e) {
    consultUnread.value = 0
  }
}

const onCommand = (command) => {
  if (command === 'profile') router.push('/profile')
  else if (command === 'orders') router.push('/orders')
  else if (command === 'after-sale') router.push('/after-sale')
  else if (command === 'coupons') router.push('/coupons')
  else if (command === 'consult') router.push('/consult')
  else if (command === 'address') router.push('/address')
  else if (command === 'admin') router.push('/admin')
  else if (command === 'merchant') router.push('/merchant')
  else if (command === 'logout') handleLogout()
}

const handleLogout = () => {
  userStore.logout()
  cartStore.reset()
  consultUnread.value = 0
  ElMessage.success('已退出登录')
  router.push('/')
}

watch(
  () => userStore.isLogin,
  () => loadConsultUnread()
)

onMounted(() => {
  cartStore.refresh()
  if (userStore.isLogin) {
    userStore.fetchInfo().catch(() => {})
    loadConsultUnread()
  }
})
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}

.topbar {
  background: var(--ink);
  color: #d8cfc6;
  font-size: 12px;
}

.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 34px;
}

.slogan {
  letter-spacing: 0.3px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-link {
  color: #d8cfc6;
  transition: color 0.2s;
}

.top-link:hover {
  color: #fff;
}

.divider {
  opacity: 0.35;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 76px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff8a5b, var(--primary));
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(232, 97, 45, 0.28);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-text strong {
  font-size: 19px;
  letter-spacing: 2px;
}

.brand-text em {
  font-style: normal;
  font-size: 11px;
  color: var(--muted);
  letter-spacing: 0.6px;
  margin-top: 3px;
}

.nav {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.nav-item {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--ink-2);
  transition: all 0.18s;
}

.nav-badge {
  display: inline-block;
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  margin-left: 2px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  line-height: 15px;
  text-align: center;
  vertical-align: 2px;
}

.mini-badge {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  margin-left: 6px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.nav-item:hover {
  color: var(--primary);
  background: var(--primary-soft);
}

.nav-item.router-link-exact-active {
  color: var(--primary);
  font-weight: 600;
}

.search {
  flex: 1;
  max-width: 380px;
  margin-left: auto;
}

.search :deep(.el-input-group__append) {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
  padding: 0 16px;
}

.search :deep(.el-input-group__append .el-button) {
  color: #fff;
  margin: 0;
}

.actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.cart-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.cart-btn:hover {
  background: #ffe3d6;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.login-btn {
  font-size: 13px;
  color: var(--primary);
  border: 1px solid var(--primary);
  border-radius: 999px;
  padding: 6px 14px;
  transition: all 0.2s;
}

.login-btn:hover {
  background: var(--primary);
  color: #fff;
}

@media (max-width: 1080px) {
  .nav {
    display: none;
  }
  .search {
    max-width: none;
  }
}
</style>
