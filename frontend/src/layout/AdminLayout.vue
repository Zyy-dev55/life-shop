<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <router-link to="/admin/dashboard" class="brand">
        <span class="brand-mark">悦</span>
        <div class="brand-text">
          <strong>悦家生活</strong>
          <em>运营管理后台</em>
        </div>
      </router-link>

      <el-menu :default-active="route.path" router class="menu" :collapse="false">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Grid /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
          <span v-if="pendingShip" class="menu-badge">{{ pendingShip }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/reviews">
          <el-icon><ChatDotRound /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/consults">
          <el-icon><ChatLineSquare /></el-icon>
          <span>聊天监管</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/merchants">
          <el-icon><Shop /></el-icon>
          <span>商家管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/withdraws">
          <el-icon><Money /></el-icon>
          <span>提现审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/after-sale">
          <el-icon><RefreshLeft /></el-icon>
          <span>售后监管</span>
        </el-menu-item>
        <el-menu-item index="/admin/sec-ops">
          <el-icon><Lock /></el-icon>
          <span>网络安全运维监控</span>
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
          <span class="eyebrow">ADMIN</span>
          <h1>{{ route.meta.title || '管理后台' }}</h1>
        </div>
        <div class="topbar-right">
          <span class="date">{{ today }}</span>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="32" :src="userStore.user && userStore.user.avatar">
                {{ userStore.nickname.slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ userStore.nickname }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { adminApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const pendingShip = ref(0)

const today = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

const loadBadge = async () => {
  try {
    const data = await adminApi.dashboard()
    pendingShip.value = data.pendingShipCount || 0
  } catch (e) {
    pendingShip.value = 0
  }
}

const onCommand = (command) => {
  if (command === 'shop') {
    router.push('/')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(loadBadge)
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f6f4f0;
}

.sidebar {
  width: 228px;
  flex-shrink: 0;
  background: #1e2320;
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
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #ff8a5b, var(--primary));
  color: #fff;
  font-weight: 700;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-text strong {
  color: #f4efe9;
  font-size: 16px;
  letter-spacing: 1.5px;
}

.brand-text em {
  font-style: normal;
  font-size: 11px;
  color: #7f8983;
  letter-spacing: 0.5px;
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
  color: #a7b0aa;
  background: transparent;
  font-size: 13.5px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(232, 97, 45, 0.9), rgba(199, 74, 28, 0.9));
  color: #fff;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(232, 97, 45, 0.28);
}

.menu-badge {
  margin-left: auto;
  background: #fff;
  color: var(--primary);
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
  color: #8e9892;
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
  background: rgba(255, 255, 255, 0.8);
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
  color: var(--primary);
  background: var(--primary-soft);
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
