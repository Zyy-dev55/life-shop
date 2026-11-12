<template>
  <div class="container list-page">
    <div class="page-head">
      <div>
        <h1>{{ title }}</h1>
        <p class="sub">
          <template v-if="keyword">搜索「{{ keyword }}」· </template>
          共 {{ total }} 件商品
        </p>
      </div>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>全部商品</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="filter panel">
      <div class="filter-row">
        <span class="label">分类</span>
        <div class="chips">
          <button :class="['chip', { active: !categoryId }]" @click="changeCategory(null)">全部</button>
          <button
            v-for="cat in categories"
            :key="cat.id"
            :class="['chip', { active: categoryId === cat.id }]"
            @click="changeCategory(cat.id)"
          >
            {{ cat.icon }} {{ cat.name }}
          </button>
        </div>
      </div>
      <div class="filter-row">
        <span class="label">排序</span>
        <div class="chips">
          <button :class="['chip', { active: sort === 'default' }]" @click="changeSort('default')">综合</button>
          <button :class="['chip', { active: sort === 'sales' }]" @click="changeSort('sales')">销量优先</button>
          <button :class="['chip', { active: sort === 'priceAsc' }]" @click="changeSort('priceAsc')">价格从低到高</button>
          <button :class="['chip', { active: sort === 'priceDesc' }]" @click="changeSort('priceDesc')">价格从高到低</button>
        </div>
      </div>
    </div>

    <div class="goods-grid" v-loading="loading">
      <ProductCard v-for="item in list" :key="item.id" :product="item" />
    </div>

    <el-empty v-if="!loading && !list.length" description="没有找到符合条件的商品，换个条件试试">
      <el-button type="primary" plain @click="resetAll">查看全部商品</el-button>
    </el-empty>

    <el-pagination
      v-if="total > pageSize"
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next, jumper"
      background
      @current-change="changePage"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { productApi } from '@/api'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)

const keyword = ref('')
const categoryId = ref(null)
const sort = ref('default')
const pageNum = ref(1)
const pageSize = 12

const title = computed(() => {
  if (categoryId.value) {
    const cat = categories.value.find((c) => c.id === categoryId.value)
    return cat ? cat.name : '全部商品'
  }
  return '全部商品'
})

const syncQuery = (patch) => {
  const query = { ...route.query, ...patch }
  Object.keys(query).forEach((key) => {
    if (!query[key] || query[key] === 'default') delete query[key]
  })
  router.push({ path: '/list', query })
}

const changeCategory = (id) => syncQuery({ categoryId: id, page: null })
const changeSort = (value) => syncQuery({ sort: value, page: null })
const changePage = (page) => {
  syncQuery({ page })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
const resetAll = () => router.push({ path: '/list' })

const load = async () => {
  loading.value = true
  try {
    const data = await productApi.page({
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      sort: sort.value,
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query,
  (query) => {
    keyword.value = query.keyword || ''
    categoryId.value = query.categoryId ? Number(query.categoryId) : null
    sort.value = query.sort || 'default'
    pageNum.value = Number(query.page) || 1
    load()
  },
  { immediate: true, deep: true }
)

productApi.categories().then((data) => {
  categories.value = data || []
})
</script>

<style scoped>
.list-page {
  padding-top: 28px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 26px;
  font-weight: 700;
}

.sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.filter {
  padding: 6px 22px;
  margin-bottom: 22px;
}

.filter-row {
  display: flex;
  gap: 18px;
  padding: 14px 0;
  align-items: flex-start;
}

.filter-row + .filter-row {
  border-top: 1px dashed var(--line);
}

.label {
  flex-shrink: 0;
  width: 40px;
  font-size: 13px;
  color: var(--muted);
  line-height: 30px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  border: 1px solid transparent;
  background: #f7f3ed;
  color: var(--ink-2);
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.chip:hover {
  color: var(--primary);
  background: var(--primary-soft);
}

.chip.active {
  background: var(--ink);
  color: #fff;
  border-color: var(--ink);
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  min-height: 200px;
}

@media (max-width: 1000px) {
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
