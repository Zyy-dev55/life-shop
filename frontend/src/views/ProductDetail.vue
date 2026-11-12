<template>
  <div class="container detail-page" v-loading="loading">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/list', query: { categoryId: product.categoryId } }">
        {{ product.categoryName || '全部商品' }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="detail-main" v-if="product.id">
      <div class="gallery">
        <div class="main-img">
          <img :src="activeImage" :alt="product.name" @error="onImgError" />
        </div>
        <div class="thumbs">
          <button
            v-for="(img, index) in images"
            :key="index"
            :class="['thumb', { active: activeImage === img }]"
            @click="activeImage = img"
          >
            <img :src="img" @error="onImgError" />
          </button>
        </div>
      </div>

      <div class="info">
        <span class="tag-soft">{{ product.categoryName }}</span>
        <h1 class="title">{{ product.name }}</h1>
        <p class="subtitle">{{ product.subtitle }}</p>

        <div class="price-box">
          <div class="price-line">
            <span class="price-label">到手价</span>
            <span class="price price-big">
              <span class="price-symbol">¥</span>{{ money(product.price) }}
            </span>
            <span v-if="product.originalPrice" class="price-old">¥{{ money(product.originalPrice) }}</span>
            <span v-if="discount" class="save-tag">立省 ¥{{ discount }}</span>
          </div>
          <div class="shop-line" v-if="product.merchantName">
            <el-icon><Shop /></el-icon>
            <span>{{ product.merchantName }}</span>
            <em>本商品由该店铺发货并提供售后</em>
            <button class="consult-btn" @click="openConsult">
              <el-icon><ChatDotRound /></el-icon>
              咨询商家
            </button>
          </div>

          <div class="stat-line">
            <span>月销 {{ product.sales || 0 }} {{ product.unit }}</span>
            <span class="dot">·</span>
            <span>库存 {{ product.stock }} {{ product.unit }}</span>
            <span class="dot">·</span>
            <span class="link-rate" @click="scrollToReview">
              <el-rate :model-value="Number(product.rating || 5)" disabled size="small" />
              {{ product.rating || '5.0' }} 分 · {{ product.reviewCount || 0 }} 条评价
            </span>
          </div>
        </div>

        <div class="buy-row">
          <span class="label">数量</span>
          <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock, 1)" />
          <span class="stock-tip">最多可买 {{ product.stock }} {{ product.unit }}</span>
        </div>

        <div class="actions">
          <el-button size="large" class="btn-cart" :disabled="!product.stock" @click="addToCart">
            <el-icon><ShoppingCart /></el-icon>
            加入购物车
          </el-button>
          <el-button size="large" type="primary" class="btn-buy" :disabled="!product.stock" @click="buyNow">
            立即购买
          </el-button>
        </div>

        <div class="services">
          <div class="service"><el-icon><CircleCheck /></el-icon> 正品保障</div>
          <div class="service"><el-icon><Van /></el-icon> 48 小时内发货</div>
          <div class="service"><el-icon><RefreshLeft /></el-icon> 30 天无忧退换</div>
        </div>
      </div>
    </div>

    <div class="detail-body" v-if="product.id">
      <div class="detail-desc panel">
        <div class="desc-head">
          <h3>商品详情</h3>
        </div>
        <p class="desc-text">{{ product.detail }}</p>
        <div class="desc-imgs">
          <img v-for="(img, index) in images" :key="index" :src="img" @error="onImgError" />
        </div>
      </div>

      <aside class="side">
        <div class="panel side-card">
          <h4>买了还买</h4>
          <div class="mini-list">
            <router-link
              v-for="item in related"
              :key="item.id"
              :to="`/product/${item.id}`"
              class="mini-item"
            >
              <img :src="item.mainImage" @error="onImgError" />
              <div class="mini-info">
                <p class="mini-name">{{ item.name }}</p>
                <span class="price">¥{{ money(item.price) }}</span>
              </div>
            </router-link>
          </div>
        </div>
        <div class="panel side-card guarantee">
          <h4>购物保障</h4>
          <p>· 满 99 元免运费，未满收取 10 元</p>
          <p>· 7 天无理由退换，30 天质量问题包退</p>
          <p>· 客服在线时间 9:00 - 21:00</p>
          <el-button class="consult-side" @click="openConsult">
            <el-icon><ChatDotRound /></el-icon>
            联系店铺客服
          </el-button>
        </div>
      </aside>
    </div>

    <!-- 商品评价 -->
    <div class="panel review-panel" id="reviews" v-if="product.id">
      <div class="review-head">
        <h3>商品评价</h3>
        <span class="review-total">{{ reviewStats.total }} 条</span>
      </div>

      <div class="review-summary" v-if="reviewStats.total > 0">
        <div class="summary-score">
          <div class="score-num">{{ reviewStats.avgRating }}</div>
          <el-rate :model-value="Number(reviewStats.avgRating)" disabled />
          <p class="score-sub">好评率 {{ reviewStats.goodRate }}%</p>
          <p class="score-sub">带图 {{ reviewStats.withImage }} 条</p>
        </div>
        <div class="summary-bars">
          <button
            v-for="star in [5, 4, 3, 2, 1]"
            :key="star"
            :class="['bar-row', { active: reviewQuery.rating === star }]"
            @click="filterByRating(star)"
          >
            <span class="bar-label">{{ star }} 星</span>
            <span class="bar-track">
              <span class="bar-fill" :style="{ width: barWidth(star) }"></span>
            </span>
            <span class="bar-count">{{ reviewStats.starCount[star] || 0 }}</span>
          </button>
          <div class="bar-filters">
            <button :class="['chip', { active: !reviewQuery.rating && !reviewQuery.withImage }]" @click="resetFilter">全部评价</button>
            <button :class="['chip', { active: reviewQuery.withImage === 1 }]" @click="filterByImage">只看有图</button>
          </div>
        </div>
      </div>

      <div class="review-empty" v-if="reviewStats.total === 0">
        还没有评价，买到手后欢迎来写下第一条～
      </div>

      <div class="review-list" v-else v-loading="reviewLoading">
        <div class="review-item" v-for="item in reviews" :key="item.id">
          <div class="review-user">
            <div class="avatar">{{ (item.userName || '匿')[0] }}</div>
            <div>
              <p class="nick">{{ item.userName }}</p>
              <p class="time">{{ item.createTime }}</p>
            </div>
          </div>
          <div class="review-content">
            <el-rate :model-value="item.rating" disabled size="small" />
            <p class="text">{{ item.content || '此用户没有填写评价内容' }}</p>
            <div class="shots" v-if="item.images && item.images.length">
              <el-image
                v-for="(img, index) in item.images"
                :key="index"
                :src="img"
                :preview-src-list="item.images"
                :initial-index="index"
                fit="cover"
                class="shot"
                preview-teleported
              />
            </div>
            <div class="shop-reply" v-if="item.reply">
              <span class="reply-tag">商家回复</span>
              <span>{{ item.reply }}</span>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="reviewPage.total > reviewPage.pageSize"
          class="review-pager"
          layout="prev, pager, next"
          background
          :current-page="reviewPage.pageNum"
          :page-size="reviewPage.pageSize"
          :total="reviewPage.total"
          @current-change="(p) => loadReviews(p)"
        />
      </div>
    </div>

    <ConsultDialog
      v-model="showConsult"
      :merchant-id="product.merchantId"
      :product-id="product.id"
      :shop-name="product.merchantName"
      :shop-logo="product.merchantLogo"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi, productApi, reviewApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { money, onImgError, splitImages } from '@/utils/format'
import ConsultDialog from '@/components/ConsultDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(false)
const product = ref({})
const activeImage = ref('')
const quantity = ref(1)
const related = ref([])

/* 评价 */
const reviewStats = ref({ total: 0, avgRating: '5.0', goodRate: 0, withImage: 0, starCount: {} })
const reviews = ref([])
const reviewLoading = ref(false)
const reviewPage = ref({ total: 0, pageNum: 1, pageSize: 5 })
const reviewQuery = ref({ rating: 0, withImage: 0 })

const barWidth = (star) => {
  const total = reviewStats.value.total || 0
  const count = reviewStats.value.starCount[star] || 0
  return total ? Math.round((count / total) * 100) + '%' : '0%'
}

const loadReviews = async (pageNum = 1) => {
  if (!product.value.id) return
  reviewLoading.value = true
  try {
    const data = await reviewApi.productReviews(product.value.id, {
      pageNum,
      pageSize: reviewPage.value.pageSize,
      rating: reviewQuery.value.rating || undefined,
      withImage: reviewQuery.value.withImage || undefined
    })
    reviewStats.value = data.stats
    reviews.value = data.page.list
    reviewPage.value = {
      total: data.page.total,
      pageNum: data.page.pageNum,
      pageSize: data.page.pageSize
    }
  } finally {
    reviewLoading.value = false
  }
}

const filterByRating = (star) => {
  reviewQuery.value.rating = reviewQuery.value.rating === star ? 0 : star
  reviewQuery.value.withImage = 0
  loadReviews(1)
}

const filterByImage = () => {
  reviewQuery.value.rating = 0
  reviewQuery.value.withImage = reviewQuery.value.withImage ? 0 : 1
  loadReviews(1)
}

const resetFilter = () => {
  reviewQuery.value = { rating: 0, withImage: 0 }
  loadReviews(1)
}

const scrollToReview = () => {
  document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const images = computed(() => splitImages(product.value.images, product.value.mainImage))
const discount = computed(() => {
  const { price, originalPrice } = product.value
  if (!originalPrice || Number(originalPrice) <= Number(price || 0)) return 0
  return (Number(originalPrice) - Number(price)).toFixed(2)
})

const loadDetail = async (id) => {
  loading.value = true
  try {
    product.value = await productApi.detail(id)
    activeImage.value = product.value.mainImage
    quantity.value = 1
    related.value = (await productApi.hot(4)).filter((item) => item.id !== Number(id))
    reviewQuery.value = { rating: 0, withImage: 0 }
    await loadReviews(1)
  } finally {
    loading.value = false
  }
}

const requireLogin = () => {
  if (userStore.isLogin) return true
  ElMessage.warning('请先登录')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

const addToCart = async () => {
  if (!requireLogin()) return
  await cartApi.add({ productId: product.value.id, quantity: quantity.value })
  cartStore.refresh()
  ElMessage.success('已加入购物车')
}

const buyNow = () => {
  if (!requireLogin()) return
  router.push({ path: '/checkout', query: { productId: product.value.id, quantity: quantity.value } })
}

/* 咨询商家 */
const showConsult = ref(false)
const openConsult = () => {
  if (!requireLogin()) return
  if (!product.value.merchantId) {
    ElMessage.warning('该商品暂未关联店铺')
    return
  }
  showConsult.value = true
}

watch(
  () => route.params.id,
  (id) => {
    if (id) loadDetail(id)
  },
  { immediate: true }
)
</script>

<style scoped>
.detail-page {
  padding-top: 24px;
  min-height: 60vh;
}

.crumb {
  margin-bottom: 18px;
}

.detail-main {
  display: grid;
  grid-template-columns: 480px 1fr;
  gap: 40px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 26px;
  box-shadow: var(--shadow-sm);
}

.main-img {
  aspect-ratio: 1 / 1;
  border-radius: 14px;
  overflow: hidden;
  background: #f6f1ea;
}

.main-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.thumb {
  width: 70px;
  height: 70px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: none;
  transition: border-color 0.2s;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb.active {
  border-color: var(--primary);
}

.info {
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.4;
  margin-top: 10px;
}

.subtitle {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13.5px;
}

.price-box {
  margin-top: 20px;
  background: linear-gradient(120deg, #fff6f1, #fdfbf7);
  border: 1px solid #f8e2d5;
  border-radius: 14px;
  padding: 18px 20px;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price-label {
  font-size: 12px;
  color: var(--muted);
}

.price-big {
  font-size: 38px;
  line-height: 1.1;
}

.save-tag {
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
}

.shop-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
  padding: 7px 12px;
  background: #f2f9f5;
  border-radius: 9px;
  font-size: 12.5px;
  color: #1f7a5c;
  width: fit-content;
}

.shop-line .el-icon {
  font-size: 13px;
}

.shop-line span {
  font-weight: 600;
}

.shop-line em {
  font-style: normal;
  color: var(--muted);
  font-size: 11.5px;
}

.consult-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 6px;
  padding: 4px 10px;
  border: 1px solid rgba(31, 122, 92, 0.35);
  background: #fff;
  color: #1f7a5c;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.consult-btn:hover {
  background: #1f7a5c;
  border-color: #1f7a5c;
  color: #fff;
}

.consult-side {
  width: 100%;
  margin-top: 12px;
  border-color: var(--primary);
  color: var(--primary);
}

.consult-side:hover {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
}

.stat-line {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ink-2);
}

.dot {
  margin: 0 8px;
  color: var(--line);
}

.buy-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 24px;
}

.buy-row .label {
  font-size: 13px;
  color: var(--muted);
}

.stock-tip {
  font-size: 12px;
  color: var(--muted);
}

.actions {
  display: flex;
  gap: 14px;
  margin-top: 26px;
}

.btn-cart,
.btn-buy {
  flex: 1;
  height: 48px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
}

.btn-cart {
  border: 1px solid var(--primary);
  color: var(--primary);
  background: #fff;
}

.btn-cart:hover {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
}

.btn-buy {
  box-shadow: 0 10px 22px rgba(232, 97, 45, 0.28);
}

.services {
  display: flex;
  gap: 22px;
  margin-top: auto;
  padding-top: 26px;
  border-top: 1px dashed var(--line);
  font-size: 12.5px;
  color: var(--ink-2);
}

.service {
  display: flex;
  align-items: center;
  gap: 5px;
}

.service .el-icon {
  color: var(--green);
}

.detail-body {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  margin-top: 20px;
  align-items: start;
}

.detail-desc {
  padding: 26px 28px;
}

.desc-head {
  border-bottom: 1px solid var(--line);
  padding-bottom: 14px;
  margin-bottom: 18px;
}

.desc-head h3 {
  font-size: 17px;
  position: relative;
  padding-left: 12px;
}

.desc-head h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 2px;
  background: var(--primary);
}

.desc-text {
  font-size: 14px;
  color: var(--ink-2);
  line-height: 2;
}

.desc-imgs {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 22px;
}

.desc-imgs img {
  width: 100%;
  border-radius: 12px;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 130px;
}

.side-card {
  padding: 20px;
}

.side-card h4 {
  font-size: 14px;
  margin-bottom: 14px;
}

.mini-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mini-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.mini-item img {
  width: 56px;
  height: 56px;
  border-radius: 9px;
  object-fit: cover;
  background: #f6f1ea;
}

.mini-name {
  font-size: 12.5px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mini-info .price {
  font-size: 13px;
}

.guarantee p {
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 2;
}

/* ---------------- 评价区 ---------------- */
.link-rate {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--primary-deep);
}

.link-rate:hover {
  text-decoration: underline;
}

.review-panel {
  margin-top: 20px;
  padding: 26px 28px 30px;
}

.review-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 14px;
  margin-bottom: 20px;
}

.review-head h3 {
  font-size: 17px;
  position: relative;
  padding-left: 12px;
}

.review-head h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 2px;
  background: var(--primary);
}

.review-total {
  font-size: 13px;
  color: var(--muted);
}

.review-summary {
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 28px;
  padding: 18px 22px;
  background: linear-gradient(120deg, #fff8f3, #fdfbf7);
  border: 1px solid #f7e6d9;
  border-radius: 14px;
}

.summary-score {
  text-align: center;
  border-right: 1px dashed #f0ddce;
  padding-right: 20px;
}

.score-num {
  font-size: 40px;
  font-weight: 700;
  color: var(--primary-deep);
  line-height: 1.2;
}

.summary-score :deep(.el-rate) {
  justify-content: center;
  margin: 4px 0 6px;
}

.score-sub {
  font-size: 12.5px;
  color: var(--muted);
  line-height: 1.8;
}

.summary-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.bar-row {
  display: grid;
  grid-template-columns: 44px 1fr 40px;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  padding: 2px 6px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12.5px;
  color: var(--ink-2);
  text-align: left;
}

.bar-row:hover,
.bar-row.active {
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.bar-track {
  height: 7px;
  border-radius: 999px;
  background: #f1e6dc;
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #ffb27a, var(--primary));
  transition: width 0.4s ease;
}

.bar-count {
  color: var(--muted);
}

.bar-filters {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.chip {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  cursor: pointer;
}

.chip.active {
  border-color: var(--primary);
  color: var(--primary-deep);
  background: var(--primary-soft);
}

.review-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--muted);
  font-size: 13.5px;
}

.review-list {
  margin-top: 8px;
}

.review-item {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 18px;
  padding: 22px 4px;
  border-bottom: 1px solid var(--line);
}

.review-item:last-of-type {
  border-bottom: none;
}

.review-user {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--primary-soft);
  color: var(--primary-deep);
  font-weight: 600;
  flex-shrink: 0;
}

.nick {
  font-size: 13px;
  font-weight: 600;
}

.time {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 3px;
}

.review-content .text {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--ink-2);
}

.shots {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.shot {
  width: 92px;
  height: 92px;
  border-radius: 10px;
  cursor: zoom-in;
  background: #f6f1ea;
}

.shop-reply {
  margin-top: 12px;
  background: #f7f9f6;
  border-left: 3px solid var(--green);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 1.8;
}

.reply-tag {
  color: var(--green);
  font-weight: 600;
  margin-right: 8px;
}

.review-pager {
  margin-top: 22px;
  justify-content: center;
}

@media (max-width: 980px) {
  .review-summary {
    grid-template-columns: 1fr;
  }
  .summary-score {
    border-right: none;
    border-bottom: 1px dashed #f0ddce;
    padding: 0 0 14px;
  }
  .review-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .detail-main {
    grid-template-columns: 1fr;
  }
  .detail-body {
    grid-template-columns: 1fr;
  }
  .side {
    position: static;
  }
}
</style>
