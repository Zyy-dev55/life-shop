<template>
  <div class="home">
    <!-- Hero -->
    <section class="container hero">
      <div class="hero-left" v-loading="loading">
        <el-carousel v-if="banners.length" height="392px" :interval="5000" arrow="hover">
          <el-carousel-item v-for="item in banners" :key="item.id">
            <router-link :to="item.link || '/list'" class="slide">
              <img :src="item.image" :alt="item.title" @error="onImgError" />
              <div class="slide-mask"></div>
              <div class="slide-text">
                <span class="kicker">本周精选 · WEEKLY PICKS</span>
                <h2>{{ item.title }}</h2>
                <span class="cta">立即逛逛 <i>→</i></span>
              </div>
            </router-link>
          </el-carousel-item>
        </el-carousel>
        <div v-else class="hero-placeholder">
          <h2>把日子过成喜欢的样子</h2>
          <p>厨房 · 清洁 · 收纳 · 卫浴 · 布艺 · 护理</p>
        </div>
      </div>

      <div class="hero-right">
        <div class="promo promo-main">
          <div class="promo-head">
            <span class="tag">新客专享</span>
            <span class="emoji">🎁</span>
          </div>
          <h3>首单立减 10 元</h3>
          <p>注册即领，全站无门槛使用</p>
          <router-link to="/register" class="promo-btn">立即领取</router-link>
        </div>
        <div class="promo promo-sub">
          <div>
            <h4>满 99 元包邮</h4>
            <p>16:00 前下单，当日发出</p>
          </div>
          <span class="emoji">📦</span>
        </div>
      </div>
    </section>

    <!-- 分类 -->
    <section class="container section">
      <div class="section-head">
        <div>
          <h2 class="section-title">按需逛逛</h2>
          <p class="section-sub">厨房、清洁、收纳、卫浴、布艺、护理，六大场景一站买齐</p>
        </div>
        <router-link class="more-link" to="/list">查看全部商品 →</router-link>
      </div>

      <div class="cat-grid" v-loading="loading">
        <router-link
          v-for="cat in categories"
          :key="cat.id"
          :to="`/list?categoryId=${cat.id}`"
          class="cat-card"
        >
          <span class="cat-icon">{{ cat.icon || '🛍️' }}</span>
          <span class="cat-name">{{ cat.name }}</span>
          <span class="cat-arrow">→</span>
        </router-link>
      </div>
    </section>

    <!-- 热销 -->
    <section class="container section">
      <div class="section-head">
        <div>
          <span class="tag-soft">大家都在买</span>
          <h2 class="section-title" style="margin-top: 8px">热销好物</h2>
          <p class="section-sub">销量榜前排，闭眼入不出错</p>
        </div>
        <router-link class="more-link" to="/list?sort=sales">查看销量榜 →</router-link>
      </div>
      <div class="goods-grid" v-loading="loading">
        <ProductCard v-for="item in hotProducts" :key="item.id" :product="item" />
      </div>
      <el-empty v-if="!loading && !hotProducts.length" description="暂无商品数据" />
    </section>

    <!-- 促销条 -->
    <section class="container section">
      <div class="banner-strip">
        <div class="strip-text">
          <span class="kicker">SEASONAL SALE</span>
          <h3>换季收纳季 · 满 199 减 30</h3>
          <p>真空压缩袋、折叠收纳箱、抽屉收纳盒，一次收好一整个衣柜</p>
        </div>
        <router-link to="/list?categoryId=3" class="strip-btn">去挑收纳好物</router-link>
      </div>
    </section>

    <!-- 新品 -->
    <section class="container section">
      <div class="section-head">
        <div>
          <h2 class="section-title">新品上架</h2>
          <p class="section-sub">每周更新，先看先得</p>
        </div>
        <router-link class="more-link" to="/list">全部商品 →</router-link>
      </div>
      <div class="goods-grid" v-loading="loading">
        <ProductCard v-for="item in newProducts" :key="item.id" :product="item" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ProductCard from '@/components/ProductCard.vue'
import { productApi } from '@/api'
import { onImgError } from '@/utils/format'

const loading = ref(true)
const banners = ref([])
const categories = ref([])
const hotProducts = ref([])
const newProducts = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const [bannerList, categoryList, hotList, newList] = await Promise.all([
      productApi.banners(),
      productApi.categories(),
      productApi.hot(8),
      productApi.newest(8)
    ])
    banners.value = bannerList || []
    categories.value = categoryList || []
    hotProducts.value = hotList || []
    newProducts.value = newList || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 18px;
  margin-top: 22px;
}

.hero-left {
  border-radius: 20px;
  overflow: hidden;
  background: #efe7dc;
  min-height: 392px;
  box-shadow: var(--shadow);
}

.slide {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
}

.slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slide-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, rgba(24, 20, 16, 0.78) 0%, rgba(24, 20, 16, 0.3) 55%, rgba(24, 20, 16, 0) 100%);
}

.slide-text {
  position: absolute;
  left: 46px;
  bottom: 56px;
  color: #fff;
  max-width: 62%;
}

.kicker {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 2.4px;
  opacity: 0.75;
  margin-bottom: 12px;
}

.slide-text h2 {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.5px;
}

.cta {
  display: inline-block;
  margin-top: 20px;
  padding: 9px 20px;
  border-radius: 999px;
  background: #fff;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.2s;
}

.cta i {
  font-style: normal;
  margin-left: 4px;
}

.slide:hover .cta {
  transform: translateX(3px);
}

.hero-placeholder {
  height: 392px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-2);
}

.hero-placeholder h2 {
  font-size: 28px;
}

.hero-placeholder p {
  color: var(--muted);
  font-size: 13px;
  letter-spacing: 1px;
}

.hero-right {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.promo {
  border-radius: 18px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.promo-main {
  flex: 1.35;
  background: linear-gradient(150deg, #2f4a3f, #1b2a24);
  color: #f4efe6;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.promo-main::after {
  content: '';
  position: absolute;
  right: -40px;
  bottom: -50px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(232, 97, 45, 0.18);
}

.promo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.promo-head .emoji {
  font-size: 24px;
}

.tag {
  display: inline-block;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 999px;
  letter-spacing: 0.5px;
  align-self: flex-start;
}

.promo-main h3 {
  font-size: 25px;
  margin-top: 6px;
  letter-spacing: 0.5px;
}

.promo-main p {
  font-size: 12.5px;
  color: #b9c7c0;
}

.promo-btn {
  margin-top: 12px;
  align-self: flex-start;
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  padding: 8px 18px;
  border-radius: 999px;
  font-weight: 600;
  position: relative;
  z-index: 1;
}

.promo-sub {
  flex: 1;
  background: #fff;
  border: 1px solid var(--line);
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-sm);
}

.promo-sub h4 {
  font-size: 16px;
}

.promo-sub p {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.promo-sub .emoji {
  font-size: 30px;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.cat-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 22px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.22s ease;
  box-shadow: var(--shadow-sm);
}

.cat-card:hover {
  transform: translateY(-3px);
  border-color: #f0d3c1;
  box-shadow: var(--shadow);
}

.cat-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: var(--primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.cat-name {
  font-size: 14px;
  font-weight: 600;
}

.cat-arrow {
  font-size: 12px;
  color: var(--muted);
  transition: transform 0.2s;
}

.cat-card:hover .cat-arrow {
  transform: translateX(3px);
  color: var(--primary);
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  min-height: 120px;
}

.banner-strip {
  border-radius: 20px;
  padding: 38px 44px;
  background: linear-gradient(115deg, #f4e7d8 0%, #f7f0e6 45%, #e9efe9 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  position: relative;
  overflow: hidden;
  border: 1px solid #efe3d4;
}

.banner-strip::before {
  content: '';
  position: absolute;
  right: 18%;
  top: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(232, 97, 45, 0.08);
}

.banner-strip .kicker {
  color: var(--primary);
  opacity: 1;
  font-weight: 700;
}

.banner-strip h3 {
  font-size: 27px;
  margin: 8px 0 8px;
  letter-spacing: 0.5px;
}

.banner-strip p {
  font-size: 13px;
  color: var(--ink-2);
}

.strip-btn {
  flex-shrink: 0;
  background: var(--ink);
  color: #fff;
  padding: 12px 26px;
  border-radius: 999px;
  font-size: 13.5px;
  font-weight: 600;
  transition: background 0.2s;
}

.strip-btn:hover {
  background: var(--primary);
}

@media (max-width: 1100px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .hero-right {
    flex-direction: row;
  }
  .cat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .banner-strip {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
