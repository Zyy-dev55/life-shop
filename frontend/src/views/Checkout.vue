<template>
  <div class="container checkout-page" v-loading="loading">
    <div class="page-head">
      <h1>确认订单</h1>
      <p class="sub">请核对收货信息与商品清单</p>
    </div>

    <div class="layout">
      <div class="main">
        <!-- 收货地址 -->
        <section class="panel block">
          <div class="block-head">
            <h3>收货地址</h3>
            <el-button text type="primary" @click="openAddressDialog()">+ 新增地址</el-button>
          </div>

          <div v-if="addresses.length" class="address-grid">
            <button
              v-for="addr in addresses"
              :key="addr.id"
              :class="['address-card', { active: selectedAddressId === addr.id }]"
              @click="selectedAddressId = addr.id"
            >
              <div class="addr-top">
                <strong>{{ addr.receiver }}</strong>
                <span class="phone">{{ addr.phone }}</span>
                <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
              </div>
              <p class="addr-detail">
                {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}
              </p>
              <span class="check-dot" v-if="selectedAddressId === addr.id">✓</span>
            </button>
          </div>
          <el-empty v-else :image-size="80" description="还没有收货地址">
            <el-button type="primary" plain @click="openAddressDialog()">添加收货地址</el-button>
          </el-empty>
        </section>

        <!-- 商品清单 -->
        <section class="panel block">
          <div class="block-head">
            <h3>商品清单</h3>
            <span class="muted">共 {{ items.length }} 种商品 · {{ shopGroups.length }} 家店铺</span>
          </div>

          <div v-for="group in shopGroups" :key="group.merchantId" class="shop-group">
            <div class="shop-head">
              <el-icon><Shop /></el-icon>
              <span class="shop-name">{{ group.merchantName || '平台自营' }}</span>
              <span class="shop-freight">
                {{ group.freight === 0 ? '已包邮' : `运费 ¥${money(group.freight)}` }}
              </span>
            </div>
            <div class="item" v-for="item in group.items" :key="item.productId">
              <img :src="item.mainImage" @error="onImgError" />
              <div class="item-info">
                <p class="item-name">{{ item.name }}</p>
                <span class="item-unit">{{ item.unit }} / 件</span>
              </div>
              <span class="item-price">¥{{ money(item.price) }}</span>
              <span class="item-qty">× {{ item.quantity }}</span>
              <span class="item-subtotal price">¥{{ money(item.price * item.quantity) }}</span>
            </div>
          </div>

          <div class="remark-row">
            <span class="label">订单备注</span>
            <el-input
              v-model="remark"
              placeholder="选填，如：请放门口 / 工作日送达"
              maxlength="100"
              show-word-limit
            />
          </div>
        </section>
      </div>

      <!-- 结算侧栏 -->
      <aside class="aside">
        <div class="panel summary">
          <h3>订单金额</h3>
          <div class="line">
            <span>商品总额</span>
            <span>¥{{ money(totalAmount) }}</span>
          </div>
          <div class="line">
            <span>运费</span>
            <span v-if="freight === 0" class="free">包邮</span>
            <span v-else>¥{{ money(freight) }}</span>
          </div>
          <p v-if="freight > 0" class="freight-tip">
            再买 ¥{{ money(99 - totalAmount) }} 即可免运费
          </p>

          <!-- 优惠券 -->
          <div class="coupon-row">
            <span class="coupon-label">优惠券</span>
            <div class="coupon-select" @click="openCouponDialog">
              <template v-if="selectedCoupon">
                <span class="coupon-name">-¥{{ money(selectedCoupon.amount) }}</span>
                <span class="coupon-clear" @click.stop="clearCoupon">×</span>
              </template>
              <span v-else-if="usableCoupons.length" class="coupon-hint">
                {{ usableCoupons.length }} 张可用
              </span>
              <span v-else class="coupon-hint muted">暂无可用</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <div v-if="discount > 0" class="line discount-line">
            <span>优惠券抵扣</span>
            <span class="discount">-¥{{ money(discount) }}</span>
          </div>

          <div class="divider"></div>
          <div class="line total">
            <span>应付金额</span>
            <span class="price price-big">¥{{ money(payAmount) }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            :disabled="!items.length || !selectedAddressId"
            @click="submitOrder"
          >
            提交订单
          </el-button>
          <p class="tip">提交后可在「我的订单」中完成支付</p>
        </div>
      </aside>
    </div>

    <!-- 新增地址 -->
    <el-dialog v-model="addressDialogVisible" title="新增收货地址" width="480px">
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="addressForm.receiver" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区">
          <div class="region-row">
            <el-input v-model="addressForm.province" placeholder="省" />
            <el-input v-model="addressForm.city" placeholder="市" />
            <el-input v-model="addressForm.district" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="addressForm.detail" type="textarea" :rows="2" placeholder="街道、门牌号" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>

    <!-- 选择优惠券 -->
    <el-dialog v-model="couponDialogVisible" title="选择优惠券" width="460px" :close-on-click-modal="false">
      <div v-if="!usableCoupons.length" class="coupon-empty">
        暂无可用的优惠券，可先去领券中心领取
      </div>
      <div v-else class="coupon-list">
        <div
          v-for="c in usableCoupons"
          :key="c.id"
          class="coupon-option"
          :class="{ selected: selectedCoupon && selectedCoupon.id === c.id, unusable: c.usable === false }"
          @click="pickCoupon(c)"
        >
          <div class="co-left">
            <div class="co-amount">¥{{ money(c.amount) }}</div>
            <div class="co-cond">{{ c.type === 2 ? '无门槛' : `满 ${money(c.threshold)} 可用` }}</div>
          </div>
          <div class="co-right">
            <div class="co-name">{{ c.name }}</div>
            <div class="co-valid">有效期至 {{ formatTime(c.expireTime) }}</div>
          </div>
          <div class="co-radio">
            <el-icon v-if="selectedCoupon && selectedCoupon.id === c.id"><CircleCheckFilled /></el-icon>
            <el-icon v-else><CircleCheck /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="couponDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="couponDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, CircleCheck, CircleCheckFilled } from '@element-plus/icons-vue'
import { addressApi, cartApi, couponApi, orderApi, productApi } from '@/api'
import { useCartStore } from '@/store/cart'
import { money, onImgError, formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const loading = ref(false)
const submitting = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const items = ref([])
const remark = ref('')

const directProductId = route.query.productId ? Number(route.query.productId) : null
const directQuantity = Number(route.query.quantity) || 1

const addressDialogVisible = ref(false)
const addressForm = ref({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0
})

const totalAmount = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)
)

// 下单会按店铺拆单，运费也按店铺分别计算（每家店满 99 包邮）
const shopGroups = computed(() => {
  const map = new Map()
  for (const item of items.value) {
    const key = item.merchantId || 0
    if (!map.has(key)) {
      map.set(key, {
        merchantId: key,
        merchantName: item.merchantName,
        items: [],
        amount: 0,
        freight: 0
      })
    }
    const group = map.get(key)
    group.items.push(item)
    group.amount += Number(item.price) * item.quantity
  }
  map.forEach((group) => {
    group.freight = group.amount >= 99 ? 0 : 10
  })
  return [...map.values()]
})

const freight = computed(() => shopGroups.value.reduce((sum, group) => sum + group.freight, 0))

// 优惠券
const usableCoupons = ref([])
const selectedCoupon = ref(null)
const couponDialogVisible = ref(false)
const discount = computed(() => (selectedCoupon.value ? Number(selectedCoupon.value.amount) : 0))

const payAmount = computed(() => Math.max(totalAmount.value + freight.value - discount.value, 0))

const loadUsableCoupons = async () => {
  try {
    const list = await couponApi.usable(totalAmount.value)
    usableCoupons.value = list.filter((c) => c.usable !== false)
    // 若已选券失效则清除
    if (selectedCoupon.value && !usableCoupons.value.some((c) => c.id === selectedCoupon.value.id)) {
      selectedCoupon.value = null
    }
  } catch (e) {
    usableCoupons.value = []
  }
}

const openCouponDialog = () => {
  if (!usableCoupons.value.length) {
    ElMessage.info('暂无可用的优惠券')
    return
  }
  couponDialogVisible.value = true
}

const pickCoupon = (c) => {
  if (c.usable === false) return
  selectedCoupon.value = c
  couponDialogVisible.value = false
}

const clearCoupon = () => {
  selectedCoupon.value = null
}

const loadAddresses = async () => {
  addresses.value = await addressApi.list()
  const defaultAddr = addresses.value.find((item) => item.isDefault === 1) || addresses.value[0]
  if (defaultAddr) selectedAddressId.value = defaultAddr.id
}

const loadItems = async () => {
  if (directProductId) {
    const product = await productApi.detail(directProductId)
    items.value = [
      {
        productId: product.id,
        merchantId: product.merchantId,
        merchantName: product.merchantName,
        name: product.name,
        mainImage: product.mainImage,
        price: product.price,
        unit: product.unit,
        quantity: directQuantity
      }
    ]
    await loadUsableCoupons()
    return
  }
  const cartList = await cartApi.list()
  items.value = cartList
    .filter((item) => item.checked === 1 && item.status === 1)
    .map((item) => ({
      productId: item.productId,
      merchantId: item.merchantId,
      merchantName: item.merchantName,
      name: item.name,
      mainImage: item.mainImage,
      price: item.price,
      unit: item.unit,
      quantity: item.quantity
    }))
  if (!items.value.length) {
    ElMessage.warning('请先在购物车勾选要结算的商品')
    router.replace('/cart')
  }
  await loadUsableCoupons()
}

const openAddressDialog = () => {
  addressForm.value = { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
  addressDialogVisible.value = true
}

const saveAddress = async () => {
  if (!addressForm.value.receiver || !addressForm.value.phone || !addressForm.value.detail) {
    ElMessage.warning('请填写收货人、手机号与详细地址')
    return
  }
  const saved = await addressApi.save({ ...addressForm.value })
  addressDialogVisible.value = false
  await loadAddresses()
  if (saved && saved.id) selectedAddressId.value = saved.id
  ElMessage.success('地址已保存')
}

const submitOrder = async () => {
  submitting.value = true
  try {
    const payload = {
      addressId: selectedAddressId.value,
      remark: remark.value,
      userCouponId: selectedCoupon.value ? selectedCoupon.value.id : null,
      items: directProductId
        ? [{ productId: directProductId, quantity: directQuantity }]
        : []
    }
    const orders = await orderApi.submit(payload)
    cartStore.refresh()
    const total = orders.reduce((sum, order) => sum + Number(order.payAmount), 0)
    const split = orders.length > 1 ? `（已按 ${orders.length} 家店铺拆成 ${orders.length} 个订单）` : ''
    try {
      await ElMessageBox.confirm(
        `订单提交成功，应付金额 ¥${money(total)}${split}，是否立即支付？`,
        '提交成功',
        { confirmButtonText: '立即支付', cancelButtonText: '稍后支付', type: 'success' }
      )
      // 拆单后每个订单单独付款（真实场景可合并支付）
      for (const order of orders) {
        await orderApi.pay(order.id)
      }
      ElMessage.success('支付成功')
      router.replace(orders.length > 1 ? '/orders' : `/order/${orders[0].id}`)
    } catch (e) {
      router.replace('/orders')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadAddresses(), loadItems()])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.checkout-page {
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

.layout {
  display: grid;
  grid-template-columns: 1fr 330px;
  gap: 20px;
  align-items: start;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.block {
  padding: 22px 24px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.block-head h3 {
  font-size: 16px;
}

.muted {
  font-size: 12.5px;
  color: var(--muted);
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.address-card {
  position: relative;
  text-align: left;
  background: #fbf8f4;
  border: 1.5px solid transparent;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.18s;
  font: inherit;
}

.address-card:hover {
  border-color: #f0d3c1;
}

.address-card.active {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.addr-top strong {
  font-size: 14px;
}

.phone {
  font-size: 12.5px;
  color: var(--ink-2);
}

.default-tag {
  font-size: 10.5px;
  color: var(--primary);
  border: 1px solid var(--primary);
  border-radius: 3px;
  padding: 0 4px;
}

.addr-detail {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--muted);
  line-height: 1.6;
}

.check-dot {
  position: absolute;
  right: 12px;
  bottom: 10px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shop-group + .shop-group {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.shop-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0 8px;
  font-size: 12.5px;
  color: var(--ink-2);
}

.shop-head .el-icon {
  color: #1f7a5c;
  font-size: 14px;
}

.shop-name {
  font-weight: 600;
}

.shop-freight {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--muted);
}

.item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
}

.item:last-of-type {
  border-bottom: none;
}

.item img {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
  background: #f6f1ea;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-unit {
  font-size: 12px;
  color: var(--muted);
}

.item-price {
  width: 90px;
  text-align: right;
  font-size: 13px;
  color: var(--ink-2);
}

.item-qty {
  width: 60px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.item-subtotal {
  width: 100px;
  text-align: right;
  font-size: 15px;
}

.remark-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed var(--line);
}

.remark-row .label {
  font-size: 13px;
  color: var(--muted);
  flex-shrink: 0;
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
  margin-bottom: 18px;
}

.line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--ink-2);
  padding: 7px 0;
}

.free {
  color: var(--green);
  font-weight: 600;
}

.freight-tip {
  font-size: 11.5px;
  color: var(--primary);
  background: var(--primary-soft);
  padding: 6px 10px;
  border-radius: 6px;
  margin-top: 4px;
}

.divider {
  height: 1px;
  background: var(--line);
  margin: 14px 0;
}

.coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  margin-top: 6px;
  border-top: 1px dashed var(--line);
}

.coupon-label {
  font-size: 13px;
  color: var(--ink-2);
  flex-shrink: 0;
}

.coupon-select {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--primary);
  font-size: 13px;
}

.coupon-select .coupon-name {
  font-weight: 600;
}

.coupon-select .coupon-hint {
  color: var(--muted);
}

.coupon-select .coupon-hint.muted {
  color: #b9b3ad;
}

.coupon-select .coupon-clear {
  color: var(--muted);
  font-size: 16px;
  line-height: 1;
  padding: 0 2px;
}

.coupon-select .coupon-clear:hover {
  color: var(--primary);
}

.coupon-select .arrow {
  font-size: 12px;
  color: var(--muted);
}

.discount-line .discount {
  color: var(--primary);
  font-weight: 600;
}

.coupon-empty {
  text-align: center;
  padding: 30px;
  color: var(--muted);
  font-size: 13px;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
}

.coupon-option {
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.18s;
}

.coupon-option:hover {
  border-color: #f0c5a8;
}

.coupon-option.selected {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.coupon-option.unusable {
  opacity: 0.5;
  cursor: not-allowed;
}

.co-left {
  width: 92px;
  flex-shrink: 0;
  text-align: center;
  padding: 8px 0;
  background: linear-gradient(135deg, #ff8a5b, var(--primary));
  border-radius: 10px;
  color: #fff;
}

.co-amount {
  font-size: 20px;
  font-weight: 700;
}

.co-cond {
  font-size: 11px;
  opacity: 0.9;
  margin-top: 2px;
}

.co-right {
  flex: 1;
  min-width: 0;
}

.co-name {
  font-size: 14px;
  font-weight: 600;
}

.co-valid {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 4px;
}

.co-radio {
  font-size: 20px;
  color: var(--primary);
  flex-shrink: 0;
}

.total {
  align-items: baseline;
  font-size: 13.5px;
  color: var(--ink);
}

.price-big {
  font-size: 26px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 999px;
  margin-top: 16px;
  font-weight: 600;
  font-size: 15px;
}

.tip {
  margin-top: 10px;
  font-size: 11.5px;
  color: var(--muted);
  text-align: center;
}

.region-row {
  display: flex;
  gap: 8px;
  flex-direction: column;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .address-grid {
    grid-template-columns: 1fr;
  }
  .aside {
    position: static;
  }
}
</style>
