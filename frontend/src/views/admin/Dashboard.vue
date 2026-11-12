<template>
  <div class="dashboard" v-loading="loading">
    <div class="stat-grid">
      <div class="stat-card primary">
        <div class="stat-top">
          <span class="stat-label">累计销售额</span>
          <span class="stat-icon">💰</span>
        </div>
        <strong class="stat-value">¥{{ amount(data.totalSales) }}</strong>
        <span class="stat-foot">今日 ¥{{ amount(data.todaySales) }}</span>
      </div>

      <div class="stat-card">
        <div class="stat-top">
          <span class="stat-label">订单总数</span>
          <span class="stat-icon">🧾</span>
        </div>
        <strong class="stat-value">{{ data.orderCount || 0 }}</strong>
        <span class="stat-foot">今日新增 {{ data.todayOrderCount || 0 }} 单</span>
      </div>

      <div class="stat-card">
        <div class="stat-top">
          <span class="stat-label">注册用户</span>
          <span class="stat-icon">👥</span>
        </div>
        <strong class="stat-value">{{ data.userCount || 0 }}</strong>
        <span class="stat-foot">累计注册会员</span>
      </div>

      <div class="stat-card">
        <div class="stat-top">
          <span class="stat-label">在售商品</span>
          <span class="stat-icon">📦</span>
        </div>
        <strong class="stat-value">{{ data.productCount || 0 }}</strong>
        <span class="stat-foot">待发货订单 {{ data.pendingShipCount || 0 }} 单</span>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel chart-card">
        <div class="card-head">
          <div>
            <h3>近 7 天销售趋势</h3>
            <p class="hint">按已付款订单（待发货 / 待收货 / 已完成）统计</p>
          </div>
          <router-link to="/admin/orders" class="link">查看订单 →</router-link>
        </div>
        <div ref="chartRef" class="chart"></div>
      </div>

      <div class="panel status-card">
        <div class="card-head">
          <h3>订单状态分布</h3>
        </div>
        <div class="status-list">
          <div v-for="item in data.statusDistribution" :key="item.status" class="status-row">
            <div class="status-info">
              <span>{{ item.name }}</span>
              <b>{{ item.count }}</b>
            </div>
            <div class="bar">
              <div
                class="bar-inner"
                :class="'s' + item.status"
                :style="{ width: barWidth(item.count) }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="panel hot-card">
      <div class="card-head">
        <div>
          <h3>热销商品 TOP 5</h3>
          <p class="hint">按累计销量排序</p>
        </div>
        <router-link to="/admin/products" class="link">商品管理 →</router-link>
      </div>

      <div class="hot-list">
        <div v-for="(item, index) in data.hotProducts" :key="item.id" class="hot-item">
          <span class="rank" :class="'r' + (index + 1)">{{ index + 1 }}</span>
          <img :src="item.mainImage" @error="onImgError" />
          <div class="hot-info">
            <p class="hot-name">{{ item.name }}</p>
            <span class="hot-meta">{{ item.categoryName || '—' }} · 库存 {{ item.stock }}</span>
          </div>
          <div class="hot-sales">
            <strong>{{ item.sales }}</strong>
            <span>销量</span>
          </div>
          <span class="price">¥{{ money(item.price) }}</span>
        </div>
        <el-empty v-if="!loading && !data.hotProducts.length" description="暂无商品数据" :image-size="70" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'
import { money, onImgError } from '@/utils/format'

const loading = ref(false)
const chartRef = ref(null)
let chart = null

const data = reactive({
  totalSales: 0,
  todaySales: 0,
  orderCount: 0,
  todayOrderCount: 0,
  userCount: 0,
  productCount: 0,
  pendingShipCount: 0,
  salesTrend: [],
  statusDistribution: [],
  hotProducts: []
})

const amount = (value) =>
  Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const barWidth = (count) => {
  const max = Math.max(...data.statusDistribution.map((item) => Number(item.count || 0)), 1)
  return Math.max((Number(count || 0) / max) * 100, count > 0 ? 6 : 0) + '%'
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const dates = data.salesTrend.map((item) => item.date)
  const amounts = data.salesTrend.map((item) => Number(item.amount || 0))

  chart.setOption({
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#23201d',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (params) => {
        const p = params[0]
        return `${p.axisValue}<br/>销售额：¥${Number(p.value).toFixed(2)}`
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#ece4da' } },
      axisTick: { show: false },
      axisLabel: { color: '#948b81', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f4ede5', type: 'dashed' } },
      axisLabel: { color: '#948b81', fontSize: 11 }
    },
    series: [
      {
        type: 'line',
        data: amounts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        itemStyle: { color: '#e8612d', borderColor: '#fff', borderWidth: 2 },
        lineStyle: { width: 3, color: '#e8612d' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(232, 97, 45, 0.26)' },
            { offset: 1, color: 'rgba(232, 97, 45, 0.01)' }
          ])
        }
      }
    ]
  })
}

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.dashboard()
    Object.assign(data, res)
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

const handleResize = () => chart && chart.resize()

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.stat-card.primary {
  background: linear-gradient(140deg, #2c3f36, #1c2621);
  border-color: transparent;
  color: #f4efe6;
}

.stat-card.primary .stat-label,
.stat-card.primary .stat-foot {
  color: #a9b7b0;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 12.5px;
  color: var(--muted);
}

.stat-icon {
  font-size: 17px;
  opacity: 0.75;
}

.stat-value {
  font-size: 27px;
  font-weight: 700;
  letter-spacing: 0.3px;
  font-family: 'DIN Alternate', 'PingFang SC', sans-serif;
}

.stat-foot {
  font-size: 11.5px;
  color: var(--muted);
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
}

.chart-card,
.status-card,
.hot-card {
  padding: 20px 22px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-head h3 {
  font-size: 15px;
}

.hint {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 3px;
}

.link {
  font-size: 12px;
  color: var(--primary);
}

.chart {
  height: 264px;
  width: 100%;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 8px;
}

.status-info {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--ink-2);
  margin-bottom: 6px;
}

.status-info b {
  color: var(--ink);
}

.bar {
  height: 7px;
  background: #f4ede5;
  border-radius: 999px;
  overflow: hidden;
}

.bar-inner {
  height: 100%;
  border-radius: 999px;
  background: var(--primary);
  transition: width 0.5s ease;
}

.bar-inner.s0 { background: #e9a23b; }
.bar-inner.s1 { background: var(--primary); }
.bar-inner.s2 { background: #4a86c5; }
.bar-inner.s3 { background: var(--green); }
.bar-inner.s4 { background: #c3bdb5; }

.hot-list {
  display: flex;
  flex-direction: column;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
}

.hot-item:last-child {
  border-bottom: none;
}

.rank {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: #f4ede5;
  color: var(--ink-2);
  font-size: 11.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank.r1 { background: var(--primary); color: #fff; }
.rank.r2 { background: #f0b27a; color: #fff; }
.rank.r3 { background: #e3c9a8; color: #7a5a32; }

.hot-item img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f1ea;
}

.hot-info {
  flex: 1;
  min-width: 0;
}

.hot-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-meta {
  font-size: 11.5px;
  color: var(--muted);
}

.hot-sales {
  text-align: center;
  width: 60px;
}

.hot-sales strong {
  display: block;
  font-size: 14px;
}

.hot-sales span {
  font-size: 10.5px;
  color: var(--muted);
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
