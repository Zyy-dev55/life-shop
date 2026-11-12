<template>
  <div class="auth-page">
    <div class="auth-visual">
      <router-link to="/" class="brand">
        <span class="brand-mark">悦</span>
        <span>悦家生活</span>
      </router-link>
      <div class="visual-text">
        <span class="kicker">JOIN YUEJIA</span>
        <h1>注册即领<br />10 元新人券</h1>
        <p>好用的东西，值得被更多人看见。<br />30 天无忧退换，满 99 元包邮。</p>
      </div>
      <ul class="perks">
        <li>✅ 首单立减 10 元</li>
        <li>🚚 满 99 元免运费</li>
        <li>↩️ 30 天无理由退换</li>
      </ul>
    </div>

    <div class="auth-form-side">
      <div class="form-card">
        <h2>创建账号</h2>
        <p class="sub">几步即可开始你的悦家生活</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号（3-20 位）" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="Avatar" />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号（选填）" :prefix-icon="Iphone" maxlength="11" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码（6-20 位）"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">注册并登录</el-button>
        </el-form>

        <div class="bottom">
          已有账号？
          <router-link to="/login">直接登录</router-link>
          <span class="sep">|</span>
          <router-link to="/">返回首页</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Avatar, Iphone, Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度需为 3-20 位', trigger: 'blur' }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      nickname: form.nickname,
      phone: form.phone,
      password: form.password
    })
    cartStore.refresh()
    ElMessage.success('注册成功，已自动登录')
    router.replace('/')
  } catch (e) {
    // 拦截器已提示
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
  background: linear-gradient(155deg, #3a2a22 0%, #241a15 50%, #1e2622 100%);
  color: #f3ece3;
  padding: 44px 56px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.auth-visual::after {
  content: '';
  position: absolute;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  right: -120px;
  top: 18%;
  background: radial-gradient(circle, rgba(232, 97, 45, 0.28), transparent 70%);
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
  font-size: 44px;
  line-height: 1.35;
  margin: 16px 0 18px;
  font-weight: 700;
}

.visual-text p {
  font-size: 14px;
  line-height: 2;
  color: #c3b6ab;
}

.perks {
  margin-top: 36px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13.5px;
  color: #d9cdc2;
  position: relative;
  z-index: 1;
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
  margin: 8px 0 24px;
  color: var(--muted);
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 46px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 4px;
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
