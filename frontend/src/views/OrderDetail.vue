<template>
  <div class="container order-detail-page" v-loading="loading">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
      <el-breadcrumb-item>订单详情</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="order.id">
      <div class="status-panel panel">
        <div class="status-left">
          <span class="status-icon">{{ statusIcon }}</span>
          <div>
            <h2>{{ ORDER_STATUS[order.status] }}</h2>
            <p class="status-desc">{{ statusDesc }}</p>
          </div>
        </div>
        <div class="status-right">
          <el-button v-if="order.status === 0" type="primary" size="large" round @click="pay">
            立即支付 ¥{{ money(order.payAmount) }}
          </el-button>
          <el-button v-if="order.status === 0" round @click="cancel">取消订单</el-button>
          <el-button v-if="order.status === 2 || order.status === 3" round @click="openLogistics">
            🚚 查看物流
          </el-button>
          <el-button v-if="order.status === 2" type="primary" size="large" round @click="confirm">
            确认收货
          </el-button>
        </div>
      </div>

      <div class="steps panel" v-if="order.status !== 4">
        <el-steps :active="stepActive" align-center finish-status="success">
          <el-step title="提交订单" :description="formatTime(order.createTime)" />
          <el-step title="买家付款" :description="formatTime(order.payTime)" />
          <el-step title="商家发货" :description="formatTime(order.shipTime)" />
          <el-step title="确认收货" :description="formatTime(order.finishTime)" />
        </el-steps>
      </div>

      <div class="layout">
        <div class="main">
          <section class="panel block">
            <div class="block-head"><h3>收货信息</h3></div>
            <div class="info-grid">
              <div><span class="label">收货人</span>{{ order.receiver }}</div>
              <div><span class="label">联系电话</span>{{ order.phone }}</div>
              <div class="full"><span class="label">收货地址</span>{{ order.address }}</div>
              <div class="full"><span class="label">买家留言</span>{{ order.remark || '无' }}</div>
            </div>
          </section>

          <section class="panel block">
            <div class="block-head"><h3>商品清单</h3></div>
            <div class="item" v-for="item in order.items" :key="item.id">
              <router-link :to="`/product/${item.productId}`" class="item-link">
                <img :src="item.productImage" @error="onImgError" />
                <div>
                  <p class="item-name">{{ item.productName }}</p>
                  <span class="item-meta">¥{{ money(item.price) }} × {{ item.quantity }}</span>
                </div>
              </router-link>
              <span class="price item-total">¥{{ money(item.totalPrice) }}</span>
              <div class="item-actions">
                <el-button
                  v-if="order.status === 3 && isPending(item)"
                  size="small"
                  type="primary"
                  plain
                  @click="openReview(item)"
                >
                  去评价
                </el-button>
                <span v-else-if="order.status === 3" class="rated-tip">已评价</span>
                <el-button
                  v-if="(order.status === 2 || order.status === 3) && !item.afterSaleId"
                  size="small"
                  type="warning"
                  plain
                  @click="openAfterSale(item)"
                >
                  申请售后
                </el-button>
                <el-tag v-else-if="item.afterSaleId" size="small" type="info">
                  {{ item.afterSaleStatusText || '售后中' }}
                </el-tag>
              </div>
            </div>
          </section>
        </div>

        <aside class="aside">
          <div class="panel summary">
            <h3>订单信息</h3>
            <div class="row"><span>订单编号</span><span class="mono">{{ order.orderNo }}</span></div>
            <div class="row" v-if="order.merchantName"><span>所属店铺</span><span>{{ order.merchantName }}</span></div>
            <div class="row"><span>下单时间</span><span>{{ formatTime(order.createTime) }}</span></div>
            <div class="row"><span>支付时间</span><span>{{ formatTime(order.payTime) }}</span></div>
            <div class="row"><span>发货时间</span><span>{{ formatTime(order.shipTime) }}</span></div>
            <div class="divider"></div>
            <div class="row"><span>商品总额</span><span>¥{{ money(order.totalAmount) }}</span></div>
            <div class="row"><span>运费</span><span>{{ Number(order.freight) === 0 ? '包邮' : '¥' + money(order.freight) }}</span></div>
            <div class="row total"><span>实付金额</span><span class="price">¥{{ money(order.payAmount) }}</span></div>
          </div>
        </aside>
      </div>
    </template>

    <ReviewDialog v-model="reviewVisible" :item="reviewItem" @success="onReviewed" />

    <AfterSaleDialog v-model="afterSaleVisible" :item="afterSaleItem" @success="onAfterSaleSuccess" />

    <PayDialog
      v-model="payVisible"
      :amount="order.payAmount || 0"
      :order-no="order.orderNo"
      :loading="paying"
      @confirm="doPay"
    />

    <!-- 物流轨迹弹窗 -->
    <el-dialog v-model="logisticsVisible" title="物流轨迹" width="520px" :close-on-click-modal="false">
      <div v-loading="logisticsLoading" class="logistics-box">
        <template v-if="logisticsList.length">
          <div class="logistics-summary">
            <span class="company">{{ logisticsList[0].company }}</span>
            <span class="tracking">运单号：{{ logisticsList[0].trackingNo }}</span>
          </div>
          <el-timeline class="logistics-timeline">
            <el-timeline-item
              v-for="(node, idx) in logisticsList"
              :key="node.id"
              :timestamp="formatTime(node.createTime)"
              :type="idx === logisticsList.length - 1 ? 'success' : 'primary'"
              :hollow="idx !== logisticsList.length - 1"
            >
              {{ node.content }}
            </el-timeline-item>
          </el-timeline>
        </template>
        <div v-else class="logistics-empty">暂无物流信息</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, reviewApi, afterSaleApi } from '@/api'
import ReviewDialog from '@/components/ReviewDialog.vue'
import AfterSaleDialog from '@/components/AfterSaleDialog.vue'
import PayDialog from '@/components/PayDialog.vue'
import { money, onImgError, formatTime, ORDER_STATUS } from '@/utils/format'

const route = useRoute()
const loading = ref(false)
const order = ref({})

const statusIcon = computed(() => {
  return { 0: '💳', 1: '📦', 2: '🚚', 3: '✅', 4: '🚫' }[order.value.status] || '📄'
})

const statusDesc = computed(() => {
  const map = {
    0: '订单已提交，请尽快完成支付',
    1: '已付款，商家正在备货，请耐心等待',
    2: '商品已发出，收到货后请确认收货',
    3: '订单已完成，感谢你的支持',
    4: '订单已取消，库存已释放'
  }
  return map[order.value.status] || ''
})

const stepActive = computed(() => {
  const map = { 0: 1, 1: 2, 2: 3, 3: 4 }
  return map[order.value.status] || 1
})

const pendingMap = ref({})
const reviewVisible = ref(false)
const reviewItem = ref(null)

const loadPending = async () => {
  const list = await reviewApi.pending()
  const map = {}
  list.forEach((item) => {
    map[`${item.orderId}-${item.productId}`] = item
  })
  pendingMap.value = map
}

const isPending = (item) => !!pendingMap.value[`${order.value.id}-${item.productId}`]

const openReview = (item) => {
  reviewItem.value = pendingMap.value[`${order.value.id}-${item.productId}`] || {
    orderId: order.value.id,
    orderNo: order.value.orderNo,
    productId: item.productId,
    productName: item.productName,
    productImage: item.productImage
  }
  reviewVisible.value = true
}

const onReviewed = () => {
  loadPending()
}

// 售后
const afterSaleVisible = ref(false)
const afterSaleItem = ref(null)

const loadAfterSales = async () => {
  try {
    const data = await afterSaleApi.my({ pageNum: 1, pageSize: 100 })
    const records = data.list || []
    const map = {}
    records.forEach((as) => {
      if (!map[as.orderItemId]) map[as.orderItemId] = as
    })
    ;(order.value.items || []).forEach((item) => {
      const as = map[item.id]
      if (as) {
        item.afterSaleId = as.id
        item.afterSaleStatusText = as.statusText
      }
    })
  } catch (e) {
    // 忽略售后查询失败
  }
}

const openAfterSale = (item) => {
  afterSaleItem.value = item
  afterSaleVisible.value = true
}

const onAfterSaleSuccess = () => {
  load()
}

const load = async () => {
  loading.value = true
  try {
    order.value = await orderApi.detail(route.params.id)
    await loadPending()
    await loadAfterSales()
  } finally {
    loading.value = false
  }
}

const payVisible = ref(false)
const paying = ref(false)

const pay = () => {
  payVisible.value = true
}

const doPay = async () => {
  paying.value = true
  try {
    order.value = await orderApi.pay(order.value.id)
    payVisible.value = false
    ElMessage.success('支付成功，商家开始备货')
  } finally {
    paying.value = false
  }
}

const cancel = async () => {
  await ElMessageBox.confirm('确定取消该订单吗？', '取消订单', { type: 'warning' })
  await orderApi.cancel(order.value.id)
  ElMessage.success('订单已取消')
  await load()
}

const confirm = async () => {
  await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', { type: 'info' })
  order.value = await orderApi.confirm(order.value.id)
  ElMessage.success('已确认收货')
}

// 物流轨迹
const logisticsVisible = ref(false)
const logisticsLoading = ref(false)
const logisticsList = ref([])

const openLogistics = async () => {
  logisticsVisible.value = true
  logisticsLoading.value = true
  try {
    logisticsList.value = await orderApi.logistics(order.value.id)
  } finally {
    logisticsLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail-page {
  padding-top: 24px;
  padding-bottom: 40px;
  min-height: 60vh;
}

.crumb {
  margin-bottom: 18px;
}

.status-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  background: linear-gradient(120deg, #fff6f0, #fdfbf7);
  border-color: #f8e2d5;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-icon {
  font-size: 34px;
}

.status-left h2 {
  font-size: 21px;
}

.status-desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--ink-2);
}

.status-right {
  display: flex;
  gap: 10px;
}

.steps {
  margin-top: 16px;
  padding: 26px 20px 14px;
}

.layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 18px;
  margin-top: 16px;
  align-items: start;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.block {
  padding: 22px 26px;
}

.block-head {
  border-bottom: 1px solid var(--line);
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.block-head h3 {
  font-size: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 24px;
  font-size: 13.5px;
}

.info-grid .full {
  grid-column: 1 / -1;
}

.label {
  display: inline-block;
  width: 70px;
  color: var(--muted);
  font-size: 12.5px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}

.item:last-child {
  border-bottom: none;
}

.item-link {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.item-link img {
  width: 60px;
  height: 60px;
  border-radius: 9px;
  object-fit: cover;
  background: #f6f1ea;
}

.item-name {
  font-size: 13.5px;
}

.item-name:hover {
  color: var(--primary);
}

.item-meta {
  font-size: 12px;
  color: var(--muted);
}

.rated-tip {
  font-size: 12px;
  color: var(--muted);
  margin-left: 4px;
}

.item-total {
  font-size: 15px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
}

.aside {
  position: sticky;
  top: 130px;
}

.summary {
  padding: 22px;
}

.summary h3 {
  font-size: 16px;
  margin-bottom: 16px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12.5px;
  color: var(--ink-2);
  padding: 6px 0;
}

.row .mono {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.divider {
  height: 1px;
  background: var(--line);
  margin: 12px 0;
}

.total {
  font-size: 14px;
  color: var(--ink);
  align-items: baseline;
}

.total .price {
  font-size: 22px;
}

.logistics-box {
  min-height: 160px;
}

.logistics-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--primary-soft);
  border-radius: 10px;
  margin-bottom: 16px;
}

.logistics-summary .company {
  font-weight: 600;
  color: var(--primary);
}

.logistics-summary .tracking {
  font-size: 12.5px;
  color: var(--ink-2);
}

.logistics-timeline {
  padding-left: 4px;
}

.logistics-empty {
  text-align: center;
  color: var(--muted);
  padding: 48px 0;
  font-size: 13px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .aside {
    position: static;
  }
  .status-panel {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
