<template>
  <div class="product-card">
    <router-link :to="`/product/${product.id}`" class="thumb">
      <img :src="product.mainImage" :alt="product.name" @error="onImgError" />
      <span v-if="discount" class="discount">省 {{ discount }} 元</span>
      <button class="quick-add" @click.prevent.stop="addToCart">
        <el-icon><Plus /></el-icon>
        加入购物车
      </button>
    </router-link>

    <div class="info">
      <router-link :to="`/product/${product.id}`" class="name">{{ product.name }}</router-link>
      <p class="subtitle">{{ product.subtitle || '生活好物 · 品质之选' }}</p>
      <div class="meta">
        <span class="price">
          <span class="price-symbol">¥</span>{{ money(product.price) }}
        </span>
        <span v-if="product.originalPrice" class="price-old">¥{{ money(product.originalPrice) }}</span>
        <span class="rate-mini">★ {{ product.rating || '5.0' }}</span>
        <span class="sales">已售 {{ product.sales || 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { money, onImgError } from '@/utils/format'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const discount = computed(() => {
  const { price, originalPrice } = props.product
  if (!originalPrice || Number(originalPrice) <= Number(price)) return 0
  return Math.round(Number(originalPrice) - Number(price))
})

const addToCart = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  await cartApi.add({ productId: props.product.id, quantity: 1 })
  cartStore.refresh()
  ElMessage.success('已加入购物车')
}
</script>

<style scoped>
.product-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow);
  border-color: #f2d9c9;
}

.thumb {
  position: relative;
  display: block;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: #f6f1ea;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}

.product-card:hover .thumb img {
  transform: scale(1.06);
}

.discount {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(35, 32, 29, 0.78);
  color: #fff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  letter-spacing: 0.4px;
}

.quick-add {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(8px);
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}

.product-card:hover .quick-add {
  opacity: 1;
  transform: translateY(0);
}

.quick-add:hover {
  background: var(--primary);
  color: #fff;
}

.info {
  padding: 14px 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.name {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 41px;
}

.name:hover {
  color: var(--primary);
}

.subtitle {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  display: flex;
  align-items: baseline;
  margin-top: 4px;
}

.price {
  font-size: 19px;
}

.sales {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--muted);
}

.rate-mini {
  font-size: 11.5px;
  color: #e8a33d;
  margin-left: 6px;
  font-weight: 600;
}
</style>
