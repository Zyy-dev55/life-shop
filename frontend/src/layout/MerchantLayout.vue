<template>
  <div class="merchant-layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">{{ initial }}</span>
        <div class="brand-text">
          <strong>{{ shop.name || '我的店铺' }}</strong>
          <em>商家中心</em>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/merchant/dashboard">
          <el-icon><TrendCharts /></el-icon>
          <span>经营概览</span>
        </el-menu-item>
        <el-menu-item index="/merchant/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
          <span v-if="pendingShip" class="menu-badge">{{ pendingShip }}</span>
        </el-menu-item>
        <el-menu-item index="/merchant/reviews">
          <el-icon><ChatDotRound /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/consults">
          <el-icon><Service /></el-icon>
          <span>客户咨询</span>
          <span v-if="consultUnread" class="menu-badge">{{ consultUnread }}</span>
        </el-menu-item>
        <el-menu-item index="/merchant/after-sale">
          <el-icon><RefreshLeft /></el-icon>
          <span>售后处理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/wallet">
          <el-icon><Wallet /></el-icon>
          <span>钱包与提现</span>
        </el-menu-item>
        <el-menu-item index="/merchant/setting">
          <el-icon><Shop /></el-icon>
          <span>店铺设置</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <router-link to="/" class="back-link">
          <el-icon><Back /></el-icon>
          返回商城前台
        </router-link>
      </div>
    </aside>

    <div class="body">
      <header class="topbar">
        <div class="crumb">
          <span class="eyebrow">MERCHANT</span>
          <h1>{{ route.meta.title || '商家中心' }}</h1>
        </div>
        <div class="topbar-right">
          <span class="date">{{ today }}</span>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="32" :src="shop.logo || ''">
                {{ initial }}
              </el-avatar>
              <span class="user-name">{{ shop.contact || userStore.nickname }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="setting">店铺设置</el-dropdown-item>
                <el-dropdown-item command="shop">返回商城</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, provide, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { merchantApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const shop = ref({})
const pendingShip = ref(0)
const consultUnread = ref(0)
let badgeTimer = null

const initial = computed(() => (shop.value.name || '店').slice(0, 1))

const today = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

const loadShop = async () => {
  try {
    shop.value = await merchantApi.profile()
  } catch (e) {
    // 非商家账号会被拦在路由守卫，这里兜底不阻断页面
  }
}

const loadBadge = async () => {
  try {
    pendingShip.value = await merchantApi.orderPendingCount()
  } catch (e) {
    pendingShip.value = 0
  }
}

/** 待回复的咨询条数（侧边栏角标） */
const loadConsultBadge = async () => {
  try {
    consultUnread.value = await merchantApi.consultUnread()
  } catch (e) {
    consultUnread.value = 0
  }
}

/** 子页面改了店铺资料后刷新侧边栏 */
provide('refreshShop', () => {
  loadShop()
  loadBadge()
})

/** 咨询页已读后刷新角标 */
provide('refreshConsultBadge', loadConsultBadge)

const onCommand = (command) => {
  if (command === 'shop') {
    router.push('/')
  } else if (command === 'setting') {
    router.push('/merchant/setting')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(() => {
  loadShop()
  loadBadge()
  loadConsultBadge()
  badgeTimer = setInterval(loadConsultBadge, 30000)
})

onBeforeUnmount(() => {
  if (badgeTimer) clearInterval(badgeTimer)
})
</script>

<style scoped>
.merchant-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f5f0;
}

.sidebar {
  width: 228px;
  flex-shrink: 0;
  background: #1b2b24;
  display: flex;
  flex-direction: column;
  padding: 20px 14px 16px;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 22px;
  min-width: 0;
}

.brand-mark {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 11px;
  background: linear-gradient(135deg, #3fae86, #1f7a5c);
  color: #fff;
  font-weight: 700;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}

.brand-text strong {
  color: #f2f7f4;
  font-size: 14.5px;
  letter-spacing: 0.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-text em {
  font-style: normal;
  font-size: 11px;
  color: #7f9a8d;
  letter-spacing: 1px;
}

.menu {
  border-right: none;
  background: transparent;
  flex: 1;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
  border-radius: 9px;
  color: #a5bcb0;
  background: transparent;
  font-size: 13.5px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(63, 174, 134, 0.92), rgba(31, 122, 92, 0.92));
  color: #fff;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(31, 122, 92, 0.32);
}

.menu-badge {
  margin-left: auto;
  background: #fff;
  color: #1f7a5c;
  font-size: 11px;
  border-radius: 999px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  font-weight: 700;
}

.sidebar-footer {
  padding: 12px 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #88a294;
  transition: color 0.2s;
}

.back-link:hover {
  color: #fff;
}

.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 20;
}

.crumb {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.eyebrow {
  font-size: 10px;
  letter-spacing: 2px;
  color: #1f7a5c;
  background: #e8f5ef;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: 700;
}

.crumb h1 {
  font-size: 19px;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.date {
  font-size: 12.5px;
  color: var(--muted);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 13px;
}

.arrow {
  font-size: 12px;
  color: var(--muted);
}

.content {
  padding: 24px 28px 40px;
}
</style>
