<template>
  <div class="container coupon-page">
    <div class="page-head">
      <div>
        <h1>优惠券</h1>
        <p class="sub">领券享优惠，下单自动抵扣</p>
      </div>
    </div>

    <div class="tabs">
      <button :class="['tab', { active: activeTab === 'center' }]" @click="activeTab = 'center'">领券中心</button>
      <button :class="['tab', { active: activeTab === 'mine' }]" @click="activeTab = 'mine'">我的优惠券</button>
    </div>

    <!-- 领券中心 -->
    <div v-if="activeTab === 'center'" v-loading="loading">
      <div v-if="!coupons.length" class="panel empty">暂时没有可领取的优惠券</div>
      <div v-else class="coupon-grid">
        <div v-for="c in coupons" :key="c.id" class="coupon-card" :class="{ got: c.receivedByMe }">
          <div class="coupon-left">
            <div class="amount">
              <span class="symbol">¥</span>
              <span class="num">{{ money(c.amount) }}</span>
            </div>
            <p class="cond">{{ c.type === 2 ? '无门槛' : `满 ${money(c.threshold)} 可用` }}</p>
          </div>
          <div class="coupon-right">
            <div class="name">{{ c.name }}</div>
            <p class="desc">{{ c.description }}</p>
            <p class="valid">领取后 {{ c.validDays }} 天内有效</p>
            <el-button
              size="small"
              :type="c.receivedByMe ? 'info' : 'primary'"
              :disabled="c.receivedByMe"
              round
              @click="receive(c)"
            >
              {{ c.receivedByMe ? '已领取' : '立即领取' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的优惠券 -->
    <div v-else v-loading="loading">
      <div class="filter-row">
        <button
          v-for="f in filters"
          :key="f.value"
          :class="['chip', { active: status === f.value }]"
          @click="status = f.value; loadMine()"
        >
          {{ f.label }}
        </button>
      </div>
      <div v-if="!myCoupons.length" class="panel empty">
        <el-empty description="还没有优惠券，去领券中心看看吧">
          <el-button type="primary" @click="activeTab = 'center'">去领券</el-button>
        </el-empty>
      </div>
      <div v-else class="coupon-grid">
        <div
          v-for="c in myCoupons"
          :key="c.id"
          class="coupon-card mine"
          :class="{ disabled: c.status !== 0 }"
        >
          <div class="coupon-left">
            <div class="amount">
              <span class="symbol">¥</span>
              <span class="num">{{ money(c.amount) }}</span>
            </div>
            <p class="cond">{{ c.type === 2 ? '无门槛' : `满 ${money(c.threshold)} 可用` }}</p>
          </div>
          <div class="coupon-right">
            <div class="name">{{ c.name }}</div>
            <p class="desc">{{ c.type === 2 ? '全场无门槛可用' : `满 ${money(c.threshold)} 元可用` }}</p>
            <p class="valid">
              有效期至 {{ formatTime(c.expireTime) }}
            </p>
            <span v-if="c.status === 0" class="status-tag usable">可用</span>
            <span v-else-if="c.status === 1" class="status-tag used">已使用</span>
            <span v-else class="status-tag expired">已过期</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { couponApi } from '@/api'
import { money, formatTime } from '@/utils/format'

const activeTab = ref('center')
const loading = ref(false)
const coupons = ref([])
const myCoupons = ref([])
const status = ref(undefined)

const filters = [
  { label: '全部', value: undefined },
  { label: '可用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已过期', value: 2 }
]

const loadCenter = async () => {
  loading.value = true
  try {
    coupons.value = await couponApi.center()
  } finally {
    loading.value = false
  }
}

const loadMine = async () => {
  loading.value = true
  try {
    myCoupons.value = await couponApi.my(status.value)
  } finally {
    loading.value = false
  }
}

const receive = async (c) => {
  await couponApi.receive(c.id)
  ElMessage.success(`已领取「${c.name}」`)
  await loadCenter()
  if (myCoupons.value.length || status.value !== undefined) loadMine()
}

onMounted(() => {
  loadCenter()
  loadMine()
})
</script>

<style scoped>
.coupon-page {
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
  margin-bottom: 20px;
}

.tab {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  padding: 9px 22px;
  border-radius: 999px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.18s;
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

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.coupon-card {
  display: flex;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  transition: all 0.2s;
}

.coupon-card:hover {
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.06);
}

.coupon-card.got {
  opacity: 0.6;
}

.coupon-card.mine.disabled {
  opacity: 0.55;
  filter: grayscale(0.4);
}

.coupon-left {
  width: 140px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #ff8a5b, var(--primary));
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 12px;
}

.coupon-card.mine.disabled .coupon-left {
  background: #b9b3ad;
}

.amount {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.amount .symbol {
  font-size: 18px;
  font-weight: 600;
}

.amount .num {
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}

.cond {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.92;
}

.coupon-right {
  flex: 1;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.name {
  font-size: 15px;
  font-weight: 600;
}

.desc {
  font-size: 12px;
  color: var(--muted);
}

.valid {
  font-size: 11.5px;
  color: var(--muted);
  margin-bottom: 6px;
}

.coupon-right .el-button {
  align-self: flex-start;
  margin-top: auto;
}

.status-tag {
  align-self: flex-start;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 999px;
  margin-top: auto;
}

.status-tag.usable {
  color: #1f7a5c;
  background: #eef8f3;
}

.status-tag.used {
  color: var(--muted);
  background: #f2f0ee;
}

.status-tag.expired {
  color: #a07d6b;
  background: #f6efe9;
}

.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.chip {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
}

.chip.active {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary);
  font-weight: 500;
}

.empty {
  padding: 40px;
  text-align: center;
  color: var(--muted);
}

@media (max-width: 700px) {
  .coupon-grid {
    grid-template-columns: 1fr;
  }
}
</style>
