<template>
  <div class="container orders-page">
    <div class="page-head">
      <div>
        <h1>我的订单</h1>
        <p class="sub">查看订单状态、支付与物流进度</p>
      </div>
      <router-link to="/list" class="more-link">继续购物 →</router-link>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab', { active: activeStatus === tab.value }]"
        @click="changeTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tabBadge(tab)" class="badge">{{ tabBadge(tab) }}</span>
      </button>
    </div>

    <div v-loading="loading" class="order-list">
      <!-- 待评价专区 -->
      <div v-if="isPendingTab" class="pending-zone">
        <div v-if="!pendingList.length" class="panel pending-empty">
          <el-empty description="没有待评价的商品">
            <el-button type="primary" @click="$router.push('/list')">去逛逛</el-button>
          </el-empty>
        </div>
        <div v-else class="panel pending-list">
          <div v-for="item in pendingList" :key="`${item.orderId}-${item.productId}`" class="pending-item">
            <router-link :to="`/product/${item.productId}`" class="pending-main">
              <img :src="item.productImage" @error="onImgError" />
              <div class="pending-info">
                <p class="pending-name">{{ item.productName }}</p>
                <span class="pending-meta">¥{{ money(item.price) }} × {{ item.quantity }}</span>
              </div>
            </router-link>
            <div class="pending-side">
              <span class="pending-time">订单 {{ item.orderNo }}</span>
              <el-button type="primary" size="small" round @click="openPendingReview(item)">去评价</el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-for="order in list" :key="order.id" class="order-card panel">
        <div class="card-head">
          <div class="head-left">
            <span class="shop-tag" v-if="order.merchantName">
              <el-icon><Shop /></el-icon>
              {{ order.merchantName }}
            </span>
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="time">{{ formatTime(order.createTime) }}</span>
          </div>
          <el-tag :type="ORDER_STATUS_TYPE[order.status]" effect="light" round>
            {{ ORDER_STATUS[order.status] }}
          </el-tag>
        </div>

        <div class="card-body">
          <div class="goods-list">
            <div v-for="item in order.items" :key="item.id" class="goods-row">
              <router-link :to="`/product/${item.productId}`" class="goods-main">
                <img :src="item.productImage" @error="onImgError" />
                <div class="goods-info">
                  <p class="goods-name">{{ item.productName }}</p>
                  <span class="goods-meta">¥{{ money(item.price) }} × {{ item.quantity }}</span>
                </div>
              </router-link>
              <template v-if="order.status === 3">
                <el-button
                  v-if="isPending(order, item)"
                  size="small"
                  type="primary"
                  plain
                  class="rate-btn"
                  @click="openReview(order, item)"
                >
                  去评价
                </el-button>
                <span v-else class="rated-tip">已评价</span>
              </template>
            </div>
          </div>

          <div class="amount-col">
            <span class="label">实付金额</span>
            <span class="price amount">¥{{ money(order.payAmount) }}</span>
            <span class="freight">含运费 ¥{{ money(order.freight) }}</span>
          </div>

          <div class="action-col">
            <el-button size="small" @click="$router.push(`/order/${order.id}`)">订单详情</el-button>
            <el-button
              v-if="order.status === 0"
              size="small"
              type="primary"
              @click="pay(order)"
            >
              立即支付
            </el-button>
            <el-button v-if="order.status === 0" size="small" text @click="cancel(order)">取消订单</el-button>
            <el-button v-if="order.status === 2" size="small" type="primary" @click="confirm(order)">
              确认收货
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="!loading && !list.length" class="panel">
        <el-empty description="暂无相关订单">
          <el-button type="primary" @click="$router.push('/list')">去逛逛</el-button>
        </el-empty>
      </div>
    </div>

    <el-pagination
      v-if="total > pageSize"
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      background
      @current-change="changePage"
    />

    <ReviewDialog v-model="reviewVisible" :item="reviewItem" @success="onReviewed" />

    <PayDialog
      v-model="payVisible"
      :amount="payOrder.payAmount || 0"
      :order-no="payOrder.orderNo"
      :loading="paying"
      @confirm="doPay"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, reviewApi } from '@/api'
import ReviewDialog from '@/components/ReviewDialog.vue'
import PayDialog from '@/components/PayDialog.vue'
import { money, onImgError, formatTime, ORDER_STATUS, ORDER_STATUS_TYPE } from '@/utils/format'

const tabs = [
  { label: '全部', value: null },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '待评价', value: 'pending' },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 5
const activeStatus = ref(null)
const counts = ref({})

/* 评价相关 */
const pendingMap = ref({})
const pendingList = ref([])
const reviewVisible = ref(false)
const reviewItem = ref(null)

const isPendingTab = computed(() => activeStatus.value === 'pending')

const tabBadge = (tab) => {
  if (tab.value === 'pending') return pendingList.value.length
  return counts.value[tab.value]
}

const loadPending = async () => {
  const list = await reviewApi.pending()
  pendingList.value = list || []
  const map = {}
  list.forEach((item) => {
    map[`${item.orderId}-${item.productId}`] = item
  })
  pendingMap.value = map
}

const isPending = (order, item) => !!pendingMap.value[`${order.id}-${item.productId}`]

const openReview = (order, item) => {
  const pending = pendingMap.value[`${order.id}-${item.productId}`]
  reviewItem.value = pending || {
    orderId: order.id,
    orderNo: order.orderNo,
    productId: item.productId,
    productName: item.productName,
    productImage: item.productImage
  }
  reviewVisible.value = true
}

const openPendingReview = (item) => {
  reviewItem.value = item
  reviewVisible.value = true
}

const onReviewed = () => {
  loadPending()
  loadCounts()
  if (isPendingTab.value) load()
}

const loadCounts = async () => {
  counts.value = await orderApi.counts()
}

const load = async () => {
  loading.value = true
  try {
    // 待评价 tab 只展示待评价商品，不走订单分页
    if (isPendingTab.value) {
      await loadPending()
      list.value = []
      total.value = 0
      return
    }
    const data = await orderApi.page({
      status: activeStatus.value === null ? undefined : activeStatus.value,
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const changeTab = (value) => {
  activeStatus.value = value
  pageNum.value = 1
  load()
  loadCounts()
}

const changePage = (page) => {
  pageNum.value = page
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const payVisible = ref(false)
const paying = ref(false)
const payOrder = ref({})

const pay = (order) => {
  payOrder.value = order
  payVisible.value = true
}

const doPay = async () => {
  paying.value = true
  try {
    await orderApi.pay(payOrder.value.id)
    payVisible.value = false
    ElMessage.success('支付成功，商家开始备货')
    await Promise.all([load(), loadCounts()])
  } finally {
    paying.value = false
  }
}

const cancel = async (order) => {
  await ElMessageBox.confirm('取消后库存将释放，确定取消该订单吗？', '取消订单', { type: 'warning' })
  await orderApi.cancel(order.id)
  ElMessage.success('订单已取消')
  await Promise.all([load(), loadCounts()])
}

const confirm = async (order) => {
  await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', { type: 'info' })
  await orderApi.confirm(order.id)
  ElMessage.success('已确认收货，去给个评价吧')
  await Promise.all([load(), loadCounts(), loadPending()])
}

onMounted(() => {
  load()
  loadCounts()
  loadPending()
})
</script>

<style scoped>
.orders-page {
  padding-top: 28px;
  padding-bottom: 40px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-head h1 {
  font-size: 26px;
}

.sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.tab {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  padding: 8px 18px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab:hover {
  color: var(--primary);
  border-color: #f0d3c1;
}

.tab.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.badge {
  background: var(--primary);
  color: #fff;
  font-size: 10.5px;
  border-radius: 999px;
  padding: 0 6px;
  min-width: 16px;
  line-height: 16px;
  text-align: center;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
}

.order-card {
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fbf8f4;
  border-bottom: 1px solid var(--line);
}

.head-left {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12.5px;
  color: var(--ink-2);
}

.shop-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #1f7a5c;
  background: #eef8f3;
  border-radius: 999px;
  padding: 2px 9px;
}

.shop-tag .el-icon {
  font-size: 12px;
}

.order-no {
  font-weight: 500;
}

.time {
  color: var(--muted);
}

.card-body {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  gap: 20px;
}

.goods-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.goods-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-main {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.rate-btn {
  flex-shrink: 0;
}

.rated-tip {
  font-size: 12px;
  color: var(--muted);
  flex-shrink: 0;
}

.goods-row img {
  width: 62px;
  height: 62px;
  border-radius: 9px;
  object-fit: cover;
  background: #f6f1ea;
}

.goods-name {
  font-size: 13.5px;
  font-weight: 500;
}

.goods-name:hover {
  color: var(--primary);
}

.goods-meta {
  font-size: 12px;
  color: var(--muted);
}

.amount-col {
  width: 130px;
  text-align: center;
  border-left: 1px dashed var(--line);
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.label {
  font-size: 11.5px;
  color: var(--muted);
}

.amount {
  font-size: 19px;
}

.freight {
  font-size: 11px;
  color: var(--muted);
}

.action-col {
  width: 108px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-col .el-button {
  margin-left: 0;
  width: 100%;
}

.pending-zone {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pending-list {
  padding: 8px 20px;
}

.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px dashed var(--line);
}

.pending-item:last-child {
  border-bottom: none;
}

.pending-main {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.pending-main img {
  width: 66px;
  height: 66px;
  border-radius: 10px;
  object-fit: cover;
  background: #f6f1ea;
}

.pending-name {
  font-size: 14px;
  font-weight: 500;
}

.pending-name:hover {
  color: var(--primary);
}

.pending-meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
  display: block;
}

.pending-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.pending-time {
  font-size: 11.5px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .card-body {
    flex-wrap: wrap;
  }
  .amount-col {
    border-left: none;
    padding-left: 0;
    text-align: left;
  }
  .action-col {
    width: 100%;
    flex-direction: row;
  }
}
</style>
