<template>
  <div class="auth-page">
    <div class="auth-visual">
      <router-link to="/" class="brand">
        <span class="brand-mark">悦</span>
        <span>悦家生活</span>
      </router-link>

      <div class="visual-text">
        <span class="kicker">YUEJIA LIFE STORE</span>
        <h1>把日常<br />过成喜欢的样子</h1>
        <p>锅碗瓢盆、收纳清洁、床品洗护——<br />挑对的，不挑贵的。</p>
      </div>

      <div class="visual-stats">
        <div><strong>18+</strong><span>精选好物</span></div>
        <div><strong>6</strong><span>生活场景</span></div>
        <div><strong>99</strong><span>元包邮</span></div>
      </div>
    </div>

    <div class="auth-form-side">
      <div class="form-card">
        <h2>欢迎回来</h2>
        <p class="sub">登录后可同步购物车与订单</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">登录</el-button>
        </el-form>

        <div class="demo-tip">
          <p class="tip-title">演示账号</p>
          <div class="demo-row">
            <span>买家：<b>zhangsan / 123456</b></span>
            <el-button text type="primary" size="small" @click="fill('zhangsan', '123456')">一键填入</el-button>
          </div>
          <div class="demo-row">
            <span>商家：<b>shop_kitchen / 123456</b></span>
            <el-button text type="primary" size="small" @click="fill('shop_kitchen', '123456')">一键填入</el-button>
          </div>
          <div class="demo-row">
            <span>管理员：<b>admin / admin123</b></span>
            <el-button text type="primary" size="small" @click="fill('admin', 'admin123')">一键填入</el-button>
          </div>
        </div>

        <div class="bottom">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
          <span class="sep">|</span>
          <router-link to="/">返回首页</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const fill = (username, password) => {
  form.username = username
  form.password = password
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const data = await userStore.login(form)
    cartStore.refresh()
    ElMessage.success(`欢迎回来，${data.user.nickname || data.user.username}`)
    const redirect = route.query.redirect
    if (redirect) {
      router.replace(String(redirect))
    } else if (data.user.role === 'ADMIN') {
      router.replace('/admin')
    } else if (data.user.role === 'MERCHANT') {
      router.replace('/merchant')
    } else {
      router.replace('/')
    }
  } catch (e) {
    // 错误信息已由请求拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: var(--bg);
}

.auth-visual {
  position: relative;
  background: linear-gradient(155deg, #2b3f36 0%, #1d2621 55%, #2c2018 100%);
  color: #f3ece3;
  padding: 44px 56px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.auth-visual::after {
  content: '';
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  right: -140px;
  bottom: -120px;
  background: radial-gradient(circle, rgba(232, 97, 45, 0.34), transparent 68%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  letter-spacing: 2px;
  font-weight: 600;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #ff8a5b, var(--primary));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.visual-text {
  margin-top: auto;
  position: relative;
  z-index: 1;
}

.kicker {
  font-size: 11px;
  letter-spacing: 3px;
  opacity: 0.55;
}

.visual-text h1 {
  font-size: 46px;
  line-height: 1.35;
  margin: 16px 0 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.visual-text p {
  font-size: 14px;
  line-height: 2;
  color: #b6c3bc;
}

.visual-stats {
  margin-top: 56px;
  display: flex;
  gap: 44px;
  position: relative;
  z-index: 1;
}

.visual-stats div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.visual-stats strong {
  font-size: 26px;
  color: #ffb28c;
}

.visual-stats span {
  font-size: 12px;
  color: #96a49d;
}

.auth-form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-card h2 {
  font-size: 28px;
  font-weight: 700;
}

.sub {
  margin: 8px 0 28px;
  color: var(--muted);
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 46px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 6px;
}

.demo-tip {
  margin-top: 24px;
  background: #fff;
  border: 1px dashed var(--line);
  border-radius: 12px;
  padding: 14px 16px;
}

.tip-title {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.demo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--ink-2);
  padding: 3px 0;
}

.demo-row b {
  color: var(--ink);
  font-weight: 600;
}

.bottom {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.bottom a {
  color: var(--primary);
  font-weight: 500;
}

.sep {
  margin: 0 8px;
  opacity: 0.4;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .auth-visual {
    display: none;
  }
}
</style>
