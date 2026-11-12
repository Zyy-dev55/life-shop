<template>
  <div class="container cart-page">
    <div class="page-head">
      <div>
        <h1>购物车</h1>
        <p class="sub">共 {{ list.length }} 种商品，满 99 元免运费</p>
      </div>
      <el-button v-if="list.length" text type="danger" @click="clearCart">清空购物车</el-button>
    </div>

    <div v-loading="loading">
      <template v-if="list.length">
        <div class="cart-panel panel">
          <div class="cart-head">
            <span class="col-check">
              <el-checkbox :model-value="allChecked" @change="toggleAll">全选</el-checkbox>
            </span>
            <span class="col-goods">商品</span>
            <span class="col-price">单价</span>
            <span class="col-qty">数量</span>
            <span class="col-total">小计</span>
            <span class="col-op">操作</span>
          </div>

          <div v-for="item in list" :key="item.id" class="cart-row">
            <span class="col-check">
              <el-checkbox
                :model-value="item.checked === 1"
                :disabled="item.status !== 1"
                @change="(val) => toggleItem(item, val)"
              />
            </span>

            <router-link :to="`/product/${item.productId}`" class="goods col-goods">
              <img :src="item.mainImage" @error="onImgError" />
              <div class="goods-info">
                <p class="goods-name">{{ item.name }}</p>
                <span class="goods-unit">规格：{{ item.unit }}</span>
                <span v-if="item.status !== 1" class="off-shelf">已下架</span>
              </div>
            </router-link>

            <span class="col-price price">¥{{ money(item.price) }}</span>

            <span class="col-qty">
              <el-input-number
                :model-value="item.quantity"
                :min="1"
                :max="Math.max(item.stock, 1)"
                size="small"
                @change="(val) => updateQty(item, val)"
              />
            </span>

            <span class="col-total price">¥{{ money(item.subtotal) }}</span>

            <span class="col-op">
              <el-button text type="danger" size="small" @click="removeItem(item)">删除</el-button>
            </span>
          </div>
        </div>

        <div class="summary-bar panel">
          <div class="left">
            <el-checkbox :model-value="allChecked" @change="toggleAll">全选</el-checkbox>
            <span class="selected">已选 {{ checkedItems.length }} 件</span>
            <el-button text type="danger" @click="removeChecked" :disabled="!checkedItems.length">
              删除选中
            </el-button>
          </div>
          <div class="right">
            <div class="amount">
              <span>合计（不含运费）：</span>
              <span class="price total-price"><span class="price-symbol">¥</span>{{ money(totalAmount) }}</span>
            </div>
            <el-button type="primary" size="large" class="checkout-btn" :disabled="!checkedItems.length" @click="checkout">
              去结算（{{ checkedItems.length }}）
            </el-button>
          </div>
        </div>
      </template>

      <div v-else-if="!loading" class="empty panel">
        <el-empty description="购物车还是空的，去挑点好物吧">
          <el-button type="primary" @click="$router.push('/list')">去逛逛</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartApi } from '@/api'
import { useCartStore } from '@/store/cart'
import { money, onImgError } from '@/utils/format'

const router = useRouter()
const cartStore = useCartStore()

const loading = ref(false)
const list = ref([])

const checkedItems = computed(() => list.value.filter((item) => item.checked === 1 && item.status === 1))
const allChecked = computed(() => list.value.length > 0 && list.value.every((item) => item.checked === 1))
const totalAmount = computed(() =>
  checkedItems.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)
)

const load = async () => {
  loading.value = true
  try {
    list.value = await cartApi.list()
  } finally {
    loading.value = false
  }
  cartStore.refresh()
}

const recalc = (item) => {
  item.subtotal = Number(item.price) * item.quantity
}

const toggleItem = async (item, checked) => {
  await cartApi.updateChecked(item.id, checked ? 1 : 0)
  item.checked = checked ? 1 : 0
}

const toggleAll = async (checked) => {
  await cartApi.checkAll(checked ? 1 : 0)
  list.value.forEach((item) => {
    if (item.status === 1) item.checked = checked ? 1 : 0
  })
}

const updateQty = async (item, quantity) => {
  if (!quantity) return
  await cartApi.updateQuantity({ id: item.id, quantity })
  item.quantity = quantity
  recalc(item)
}

const removeItem = async (item) => {
  await cartApi.remove(item.id)
  list.value = list.value.filter((row) => row.id !== item.id)
  cartStore.refresh()
  ElMessage.success('已删除')
}

const removeChecked = async () => {
  const ids = checkedItems.value.map((item) => item.id)
  if (!ids.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 件商品吗？`, '提示', { type: 'warning' })
  for (const id of ids) {
    await cartApi.remove(id)
  }
  await load()
  ElMessage.success('已删除')
}

const clearCart = async () => {
  await ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
  await cartApi.clear()
  await load()
  ElMessage.success('购物车已清空')
}

const checkout = () => {
  router.push('/checkout')
}

onMounted(load)
</script>

<style scoped>
.cart-page {
  padding-top: 28px;
  padding-bottom: 40px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
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

.cart-panel {
  overflow: hidden;
}

.cart-head {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  background: #fbf8f4;
  border-bottom: 1px solid var(--line);
  font-size: 12.5px;
  color: var(--muted);
}

.cart-row {
  display: flex;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid #f5efe7;
  transition: background 0.18s;
}

.cart-row:last-child {
  border-bottom: none;
}

.cart-row:hover {
  background: #fdfaf6;
}

.col-check {
  width: 60px;
  flex-shrink: 0;
}

.col-goods {
  flex: 1;
  min-width: 0;
}

.col-price,
.col-total {
  width: 110px;
  text-align: center;
  flex-shrink: 0;
}

.col-qty {
  width: 140px;
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}

.col-op {
  width: 70px;
  text-align: right;
  flex-shrink: 0;
}

.goods {
  display: flex;
  gap: 14px;
  align-items: center;
}

.goods img {
  width: 78px;
  height: 78px;
  border-radius: 10px;
  object-fit: cover;
  background: #f6f1ea;
  flex-shrink: 0;
}

.goods-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.goods-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 380px;
}

.goods-name:hover {
  color: var(--primary);
}

.goods-unit {
  font-size: 12px;
  color: var(--muted);
}

.off-shelf {
  font-size: 11px;
  color: #b0a79d;
  background: #f1ece6;
  padding: 1px 8px;
  border-radius: 4px;
  align-self: flex-start;
}

.summary-bar {
  position: sticky;
  bottom: 20px;
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 22px;
  box-shadow: var(--shadow);
}

.left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.selected {
  font-size: 13px;
  color: var(--muted);
}

.right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.amount {
  font-size: 13px;
  color: var(--ink-2);
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.total-price {
  font-size: 26px;
}

.checkout-btn {
  height: 44px;
  padding: 0 34px;
  border-radius: 999px;
  font-weight: 600;
}

.empty {
  padding: 20px;
}

@media (max-width: 900px) {
  .cart-head,
  .col-price {
    display: none;
  }
  .cart-row {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
