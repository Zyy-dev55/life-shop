<template>
  <div class="container profile-page" v-loading="loading">
    <div class="page-head">
      <h1>个人中心</h1>
      <p class="sub">管理你的资料与账号安全</p>
    </div>

    <div class="layout">
      <aside class="side">
        <div class="panel user-card">
          <el-avatar :size="72" :src="user.avatar">{{ nickname.slice(0, 1) }}</el-avatar>
          <h3>{{ nickname }}</h3>
          <p class="account">@{{ user.username }}</p>
          <span class="role-tag">{{ user.role === 'ADMIN' ? '管理员' : '普通会员' }}</span>
          <div class="join">注册于 {{ formatTime(user.createTime) }}</div>
        </div>

        <div class="panel quick">
          <router-link to="/orders" class="quick-item">
            <span>待付款</span>
            <b>{{ counts['0'] || 0 }}</b>
          </router-link>
          <router-link to="/orders" class="quick-item">
            <span>待收货</span>
            <b>{{ counts['2'] || 0 }}</b>
          </router-link>
          <router-link to="/orders" class="quick-item">
            <span>已完成</span>
            <b>{{ counts['3'] || 0 }}</b>
          </router-link>
        </div>

        <div class="panel nav">
          <router-link to="/orders" class="nav-item">📦 我的订单</router-link>
          <router-link to="/after-sale" class="nav-item">🔄 我的售后</router-link>
          <router-link to="/coupons" class="nav-item">🎟️ 我的优惠券</router-link>
          <router-link to="/consult" class="nav-item">💬 我的咨询</router-link>
          <router-link to="/address" class="nav-item">📍 收货地址</router-link>
          <router-link to="/cart" class="nav-item">🛒 购物车</router-link>
          <router-link v-if="userStore.isAdmin" to="/admin" class="nav-item">⚙️ 管理后台</router-link>
        </div>
      </aside>

      <div class="main">
        <div class="panel content">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="个人资料" name="info">
              <el-form :model="infoForm" label-width="90px" class="form">
                <el-form-item label="头像地址">
                  <el-input v-model="infoForm.avatar" placeholder="图片 URL" />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="infoForm.nickname" maxlength="20" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="infoForm.phone" maxlength="11" />
                </el-form-item>
                <el-form-item label="性别">
                  <el-radio-group v-model="infoForm.gender">
                    <el-radio :value="0">保密</el-radio>
                    <el-radio :value="1">男</el-radio>
                    <el-radio :value="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveInfo">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px" class="form">
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="changing" @click="changePassword">确认修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane name="pending">
              <template #label>
                待评价
                <span v-if="pendingList.length" class="tab-badge">{{ pendingList.length }}</span>
              </template>
              <div v-if="!pendingList.length" class="empty-tip">没有待评价的商品，去订单里确认收货后就能评价啦</div>
              <div v-else class="pending-list">
                <div v-for="item in pendingList" :key="item.orderId + '-' + item.productId" class="pending-item">
                  <router-link :to="`/product/${item.productId}`">
                    <img :src="item.productImage" @error="onImgError" />
                  </router-link>
                  <div class="pending-info">
                    <p class="p-name">{{ item.productName }}</p>
                    <p class="p-meta">¥{{ money(item.price) }} × {{ item.quantity }}</p>
                    <p class="p-meta">完成于 {{ formatTime(item.finishTime) }}</p>
                  </div>
                  <el-button type="primary" plain size="small" @click="openReview(item)">去评价</el-button>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="我的评价" name="reviews">
              <div v-if="!myReviews.length" class="empty-tip">还没有发表过评价</div>
              <div v-else class="my-review-list">
                <div v-for="item in myReviews" :key="item.id" class="my-review">
                  <router-link :to="`/product/${item.productId}`" class="mr-product">
                    <img :src="item.productImage" @error="onImgError" />
                    <span>{{ item.productName }}</span>
                  </router-link>
                  <div class="mr-body">
                    <div class="mr-head">
                      <el-rate :model-value="item.rating" disabled size="small" />
                      <span class="mr-time">{{ item.createTime }}</span>
                    </div>
                    <p class="mr-text">{{ item.content || '（未填写内容）' }}</p>
                    <div class="mr-shots" v-if="item.images && item.images.length">
                      <el-image
                        v-for="(img, index) in item.images"
                        :key="index"
                        :src="img"
                        :preview-src-list="item.images"
                        :initial-index="index"
                        fit="cover"
                        class="mr-shot"
                        preview-teleported
                      />
                    </div>
                    <div class="shop-reply" v-if="item.reply">
                      <span class="reply-tag">商家回复</span>{{ item.reply }}
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <ReviewDialog v-model="reviewVisible" :item="reviewItem" @success="onReviewed" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi, reviewApi, userApi } from '@/api'
import ReviewDialog from '@/components/ReviewDialog.vue'
import { useUserStore } from '@/store/user'
import { formatTime, money, onImgError } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const changing = ref(false)
const activeTab = ref('info')
const user = ref({})
const counts = ref({})

const pwdFormRef = ref()

/* 评价 */
const pendingList = ref([])
const myReviews = ref([])
const reviewVisible = ref(false)
const reviewItem = ref(null)

const loadPending = async () => {
  pendingList.value = await reviewApi.pending()
}

const loadMyReviews = async () => {
  const data = await reviewApi.my({ pageNum: 1, pageSize: 50 })
  myReviews.value = data.list || []
}

const openReview = (item) => {
  reviewItem.value = item
  reviewVisible.value = true
}

const onReviewed = () => {
  loadPending()
  loadMyReviews()
}

const infoForm = reactive({ nickname: '', phone: '', avatar: '', gender: 0 })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const nickname = computed(() => user.value.nickname || user.value.username || '用户')

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const load = async () => {
  loading.value = true
  try {
    user.value = await userStore.fetchInfo()
    infoForm.nickname = user.value.nickname || ''
    infoForm.phone = user.value.phone || ''
    infoForm.avatar = user.value.avatar || ''
    infoForm.gender = user.value.gender ?? 0
    counts.value = await orderApi.counts()
  } finally {
    loading.value = false
  }
}

const saveInfo = async () => {
  saving.value = true
  try {
    user.value = await userApi.update({ ...infoForm })
    await userStore.fetchInfo()
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }
  changing.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    changing.value = false
  }
}

onMounted(async () => {
  await load()
  loadPending()
  loadMyReviews()
})
</script>

<style scoped>
.profile-page {
  padding-top: 28px;
  padding-bottom: 40px;
}

.page-head {
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 26px;
}

.sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

/* ---------------- 评价相关 ---------------- */
.tab-badge {
  display: inline-block;
  margin-left: 4px;
  background: var(--primary);
  color: #fff;
  font-size: 10.5px;
  border-radius: 999px;
  padding: 0 6px;
  line-height: 16px;
  vertical-align: 1px;
}

.empty-tip {
  padding: 36px 0;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

.pending-list,
.my-review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 6px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fdfbf8;
}

.pending-item img {
  width: 62px;
  height: 62px;
  border-radius: 9px;
  object-fit: cover;
  background: #f2ece4;
}

.pending-info {
  flex: 1;
  min-width: 0;
}

.p-name {
  font-size: 13.5px;
  font-weight: 600;
}

.p-meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 3px;
}

.my-review {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
}

.mr-product {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-2);
}

.mr-product img {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  object-fit: cover;
}

.mr-body {
  margin-top: 10px;
}

.mr-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mr-time {
  font-size: 11.5px;
  color: var(--muted);
}

.mr-text {
  margin-top: 8px;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--ink-2);
}

.mr-shots {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.mr-shot {
  width: 72px;
  height: 72px;
  border-radius: 9px;
  cursor: zoom-in;
  background: #f6f1ea;
}

.shop-reply {
  margin-top: 10px;
  background: #f7f9f6;
  border-left: 3px solid var(--green);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 1.8;
}

.reply-tag {
  color: var(--green);
  font-weight: 600;
  margin-right: 8px;
}

.layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 18px;
  align-items: start;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.user-card {
  padding: 26px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.user-card h3 {
  font-size: 17px;
  margin-top: 6px;
}

.account {
  font-size: 12.5px;
  color: var(--muted);
}

.role-tag {
  margin-top: 4px;
  font-size: 11px;
  color: var(--primary);
  background: var(--primary-soft);
  border-radius: 999px;
  padding: 2px 10px;
}

.join {
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--muted);
}

.quick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 14px 8px;
  text-align: center;
}

.quick-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: var(--muted);
  padding: 4px 0;
}

.quick-item b {
  font-size: 18px;
  color: var(--ink);
}

.quick-item:hover b {
  color: var(--primary);
}

.nav {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13.5px;
  color: var(--ink-2);
  transition: all 0.18s;
}

.nav-item:hover {
  background: var(--primary-soft);
  color: var(--primary);
}

.content {
  padding: 10px 26px 26px;
}

.form {
  max-width: 460px;
  padding-top: 12px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
