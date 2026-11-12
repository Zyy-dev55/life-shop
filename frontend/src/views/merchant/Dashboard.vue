<template>
  <div class="dashboard" v-loading="loading">
    <div class="hero panel">
      <div class="hero-text">
        <span class="tag">今日经营</span>
        <h2>{{ data.shopName || '我的店铺' }}</h2>
        <p>
          今天成交 <b>{{ data.todayOrderCount }}</b> 笔，销售额
          <b>¥{{ amount(data.todaySales) }}</b>
          <template v-if="data.pendingShipCount > 0">
            ，还有 <b class="warn">{{ data.pendingShipCount }}</b> 笔订单等你发货
          </template>
          <template v-else>，暂无待发货订单</template>
          <template v-if="data.consultUnread > 0">
            ，<router-link to="/merchant/consults" class="warn-link">{{ data.consultUnread }} 条客户咨询</router-link>等你回复
          </template>
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-metric">
          <span>累计销售额</span>
          <strong>¥{{ amount(data.totalSales) }}</strong>
        </div>
        <div class="hero-metric">
          <span>店铺评分</span>
          <strong>{{ data.avgRating || '—' }}<i v-if="data.avgRating">/5</i></strong>
        </div>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat panel">
        <div class="stat-icon orange"><el-icon><Money /></el-icon></div>
        <div>
          <span class="stat-label">今日销售额</span>
          <strong class="stat-value">¥{{ amount(data.todaySales) }}</strong>
        </div>
      </div>
      <div class="stat panel">
        <div class="stat-icon blue"><el-icon><Tickets /></el-icon></div>
        <div>
          <span class="stat-label">今日订单</span>
          <strong class="stat-value">{{ data.todayOrderCount }}<i>笔</i></strong>
        </div>
      </div>
      <div class="stat panel">
        <div class="stat-icon amber"><el-icon><Van /></el-icon></div>
        <div>
          <span class="stat-label">待发货</span>
          <strong class="stat-value">{{ data.pendingShipCount }}<i>笔</i></strong>
        </div>
      </div>
      <div class="stat panel">
        <div class="stat-icon green"><el-icon><ChatDotRound /></el-icon></div>
        <div>
          <span class="stat-label">待回复评价</span>
          <strong class="stat-value">{{ data.unrepliedCount }}<i>条</i></strong>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <div class="panel chart-card">
        <div class="card-head">
          <h3>近 7 日销售趋势</h3>
          <span class="hint">按下单时间统计，金额为已支付订单</span>
        </div>
        <div ref="chartRef" class="chart"></div>
      </div>

      <div class="panel top-card">
        <div class="card-head">
          <h3>热销商品 Top 5</h3>
          <router-link to="/merchant/products" class="link">管理商品 →</router-link>
        </div>
        <div v-if="!data.topProducts.length" class="empty">还没有成交数据，商品卖出去后这里会出现排名</div>
        <ul class="top-list">
          <li v-for="(item, index) in data.topProducts" :key="item.productId">
            <span class="rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
            <img :src="item.mainImage" @error="onImgError" />
            <div class="top-info">
              <p class="top-name">{{ item.name }}</p>
              <span class="top-meta">卖出 {{ item.quantity }} 件 · ¥{{ amount(item.amount) }}</span>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <div class="stat-grid small">
      <div class="mini panel">
        <span>商品总数</span>
        <strong>{{ data.productCount }}</strong>
      </div>
      <div class="mini panel">
        <span>在售中</span>
        <strong>{{ data.onSaleCount }}</strong>
      </div>
      <div class="mini panel">
        <span>库存预警（&lt;10）</span>
        <strong :class="{ warn: data.lowStockCount > 0 }">{{ data.lowStockCount }}</strong>
      </div>
      <div class="mini panel">
        <span>累计订单</span>
        <strong>{{ data.orderCount }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { merchantApi } from '@/api'
import { onImgError } from '@/utils/format'

const loading = ref(false)
const chartRef = ref(null)
let chart = null

const data = reactive({
  shopName: '',
  productCount: 0,
  onSaleCount: 0,
  lowStockCount: 0,
  orderCount: 0,
  pendingShipCount: 0,
  todayOrderCount: 0,
  todaySales: 0,
  totalSales: 0,
  unrepliedCount: 0,
  consultUnread: 0,
  avgRating: 0,
  trend: [],
  topProducts: []
})

const amount = (value) =>
  Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const dates = data.trend.map((item) => item.date)
  const sales = data.trend.map((item) => Number(item.sales || 0))
  const orders = data.trend.map((item) => Number(item.orders || 0))

  chart.setOption({
    grid: { left: 8, right: 12, top: 34, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#23201d',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      data: ['销售额', '订单数'],
      right: 0,
      top: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 11, color: '#948b81' }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#ece4da' } },
      axisTick: { show: false },
      axisLabel: { color: '#948b81', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: '#f4ede5', type: 'dashed' } },
        axisLabel: { color: '#948b81', fontSize: 11 }
      },
      {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { color: '#948b81', fontSize: 11 }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        data: sales,
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        itemStyle: { color: '#1f7a5c', borderColor: '#fff', borderWidth: 2 },
        lineStyle: { width: 3, color: '#1f7a5c' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(31, 122, 92, 0.24)' },
            { offset: 1, color: 'rgba(31, 122, 92, 0.01)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: orders,
        barWidth: 12,
        itemStyle: { color: 'rgba(232, 97, 45, 0.55)', borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

const load = async () => {
  loading.value = true
  try {
    const res = await merchantApi.dashboard()
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
  gap: 16px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 26px;
  background: linear-gradient(135deg, #1f7a5c 0%, #175f48 62%, #14503d 100%);
  border: none;
  color: #fff;
  flex-wrap: wrap;
}

.hero .tag {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 2px;
  background: rgba(255, 255, 255, 0.16);
  padding: 3px 9px;
  border-radius: 999px;
  margin-bottom: 10px;
}

.hero h2 {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
}

.hero p {
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.7;
}

.hero p b {
  color: #fff;
  font-size: 16px;
  margin: 0 2px;
}

.hero p b.warn {
  color: #ffd08a;
}

.hero p .warn-link {
  color: #ffd08a;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.hero-side {
  display: flex;
  gap: 34px;
}

.hero-metric {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-metric span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.hero-metric strong {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.hero-metric i {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  flex-shrink: 0;
}

.stat-icon.orange {
  background: #fff1ea;
  color: #e8612d;
}

.stat-icon.blue {
  background: #eaf1fb;
  color: #3a6fb0;
}

.stat-icon.amber {
  background: #fdf3e2;
  color: #c28824;
}

.stat-icon.green {
  background: #e8f5ef;
  color: #1f7a5c;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.stat-value i {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 3px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 16px;
}

.chart-card,
.top-card {
  padding: 18px 20px 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-head h3 {
  font-size: 14.5px;
  font-weight: 600;
}

.hint {
  font-size: 11.5px;
  color: var(--muted);
}

.link {
  font-size: 12px;
  color: #1f7a5c;
}

.chart {
  height: 258px;
}

.top-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-list li {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rank {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #f2efe9;
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank-1 {
  background: #1f7a5c;
  color: #fff;
}

.rank-2 {
  background: #4a9a7d;
  color: #fff;
}

.rank-3 {
  background: #8cc4ae;
  color: #fff;
}

.top-list img {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f1ea;
  flex-shrink: 0;
}

.top-info {
  min-width: 0;
  flex: 1;
}

.top-name {
  font-size: 12.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-meta {
  font-size: 11px;
  color: var(--muted);
}

.empty {
  padding: 34px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--muted);
}

.stat-grid.small {
  gap: 16px;
}

.mini {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.mini span {
  font-size: 12.5px;
  color: var(--muted);
}

.mini strong {
  font-size: 18px;
  font-weight: 700;
}

.mini strong.warn {
  color: var(--primary);
}

@media (max-width: 1180px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
