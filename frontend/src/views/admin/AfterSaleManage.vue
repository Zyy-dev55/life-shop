<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <span class="hint">全平台共 {{ total }} 条售后申请</span>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="店铺" width="150">
          <template #default="{ row }">
            <span class="shop-name">{{ row.merchantName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.productImage" @error="onImgError" />
              <div>
                <p class="p-name">{{ row.productName }}</p>
                <span class="order-no">{{ row.orderNo }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'warning' : 'danger'" size="small">{{ row.typeText }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="退款金额" width="110">
          <template #default="{ row }">
            <span class="amount">¥{{ money(row.amount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="退货物流" min-width="150">
          <template #default="{ row }">
            <span v-if="row.returnTrackingNo" class="return-no">
              {{ row.returnCompany || '' }} {{ row.returnTrackingNo }}
            </span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">
            <span class="time">{{ formatTime(row.applyTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="(p) => { pageNum = p; load() }"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api'
import { money, onImgError, formatTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

const query = reactive({ status: null })

const statusOptions = [
  { label: '待商家处理', value: 0 },
  { label: '待买家寄回', value: 1 },
  { label: '待商家收货', value: 2 },
  { label: '退款完成', value: 3 },
  { label: '已拒绝', value: 4 },
  { label: '已撤销', value: 5 }
]

const statusType = (s) => {
  return { 0: 'warning', 1: 'warning', 2: 'warning', 3: 'success', 4: 'danger', 5: 'info' }[s] || 'info'
}

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.afterSalePage({
      status: query.status === null ? undefined : query.status,
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  pageNum.value = 1
  load()
}
const reset = () => {
  query.status = null
  search()
}

onMounted(load)
</script>

<style scoped>
.manage-page {
  padding: 20px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
}
.hint {
  color: var(--text-secondary);
  font-size: 13px;
}
.table-card {
  padding: 8px 16px 16px;
}
.shop-name {
  font-weight: 500;
}
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.product-cell img {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  object-fit: cover;
}
.p-name {
  margin: 0 0 4px;
  font-weight: 500;
}
.order-no {
  font-size: 12px;
  color: var(--text-secondary);
}
.amount {
  color: #f56c6c;
  font-weight: 600;
}
.return-no {
  font-size: 12px;
  color: var(--text-secondary);
}
.muted {
  color: var(--text-secondary);
}
.time {
  font-size: 13px;
  color: var(--text-secondary);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
