<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 收货人 / 手机号"
          clearable
          style="width: 230px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="(label, value) in ORDER_STATUS" :key="value" :label="label" :value="Number(value)" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <span class="hint">共 {{ total }} 笔订单</span>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="订单号" min-width="190">
          <template #default="{ row }">
            <span class="mono">{{ row.orderNo }}</span>
            <span class="items-count">{{ row.items.length }} 件商品</span>
          </template>
        </el-table-column>
        <el-table-column label="买家" width="120">
          <template #default="{ row }">{{ row.username || '用户' + row.userId }}</template>
        </el-table-column>
        <el-table-column label="收货人" width="160">
          <template #default="{ row }">
            <div>{{ row.receiver }}</div>
            <span class="phone">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应付金额" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ money(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS_TYPE[row.status]" effect="light" round>
              {{ ORDER_STATUS[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
        <el-button
          v-if="row.status === 1"
          text
          type="success"
          size="small"
          @click="openShip(row)"
        >
          发货
        </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.pageSize"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="changePage"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="订单详情" size="520px">
      <div v-if="detail.id" class="detail">
        <div class="detail-head">
          <div>
            <span class="mono">{{ detail.orderNo }}</span>
            <p class="detail-time">下单于 {{ formatTime(detail.createTime) }}</p>
          </div>
          <el-tag :type="ORDER_STATUS_TYPE[detail.status]" effect="light" round>
            {{ ORDER_STATUS[detail.status] }}
          </el-tag>
        </div>

        <div class="block">
          <h4>收货信息</h4>
          <p>{{ detail.receiver }} · {{ detail.phone }}</p>
          <p class="muted">{{ detail.address }}</p>
          <p v-if="detail.remark" class="muted">备注：{{ detail.remark }}</p>
        </div>

        <div class="block">
          <h4>商品清单</h4>
          <div v-for="item in detail.items" :key="item.id" class="order-item">
            <img :src="item.productImage" @error="onImgError" />
            <div class="oi-info">
              <p>{{ item.productName }}</p>
              <span class="muted">¥{{ money(item.price) }} × {{ item.quantity }}</span>
            </div>
            <span class="price">¥{{ money(item.totalPrice) }}</span>
          </div>
        </div>

        <div class="block">
          <h4>金额明细</h4>
          <div class="row"><span>商品总额</span><span>¥{{ money(detail.totalAmount) }}</span></div>
          <div class="row"><span>运费</span><span>¥{{ money(detail.freight) }}</span></div>
          <div class="row total"><span>实付金额</span><span class="price">¥{{ money(detail.payAmount) }}</span></div>
        </div>

        <div class="block">
          <h4>时间节点</h4>
          <div class="row"><span>付款时间</span><span>{{ formatTime(detail.payTime) }}</span></div>
          <div class="row"><span>发货时间</span><span>{{ formatTime(detail.shipTime) }}</span></div>
          <div class="row"><span>完成时间</span><span>{{ formatTime(detail.finishTime) }}</span></div>
        </div>

        <el-button
          v-if="detail.status === 1"
          type="primary"
          size="large"
          class="ship-btn"
          @click="openShip(detail)"
        >
          确认发货
        </el-button>
      </div>
    </el-drawer>

    <!-- 发货弹窗：填写物流单号 -->
    <el-dialog v-model="shipVisible" title="确认发货" width="440px" :close-on-click-modal="false">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="90px">
        <el-form-item label="快递公司" prop="company">
          <el-select v-model="shipForm.company" placeholder="选择快递公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="邮政EMS" value="邮政EMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入快递单号" clearable />
        </el-form-item>
        <p class="ship-tip">发货后买家即可在订单详情中查看物流轨迹</p>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { money, onImgError, formatTime, ORDER_STATUS, ORDER_STATUS_TYPE } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const detail = ref({})

const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.orderPage({
      keyword: query.keyword || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNum = 1
  load()
}

const reset = () => {
  query.keyword = ''
  query.status = null
  query.pageNum = 1
  load()
}

const changePage = (page) => {
  query.pageNum = page
  load()
}

const openDetail = async (row) => {
  detail.value = await adminApi.orderDetail(row.id)
  drawerVisible.value = true
}

const shipVisible = ref(false)
const shipping = ref(false)
const shipFormRef = ref(null)
const shipForm = reactive({ company: '顺丰速运', trackingNo: '' })
const shipTarget = ref(null)

const shipRules = {
  company: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  trackingNo: [{ required: true, message: '请填写物流单号', trigger: 'blur' }]
}

const openShip = (row) => {
  shipTarget.value = row
  shipForm.company = '顺丰速运'
  shipForm.trackingNo = ''
  shipVisible.value = true
}

const submitShip = async () => {
  await shipFormRef.value.validate()
  shipping.value = true
  try {
    await adminApi.orderShip(shipTarget.value.id, {
      company: shipForm.company,
      trackingNo: shipForm.trackingNo
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    drawerVisible.value = false
    await load()
  } finally {
    shipping.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hint {
  font-size: 12px;
  color: var(--muted);
}

.table-card {
  padding: 8px 20px 20px;
}

.mono {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12.5px;
}

.items-count {
  display: block;
  font-size: 11px;
  color: var(--muted);
}

.phone {
  font-size: 11.5px;
  color: var(--muted);
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.detail-time {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 4px;
}

.block {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
}

.block h4 {
  font-size: 13px;
  margin-bottom: 10px;
  color: var(--ink);
}

.block p {
  font-size: 13px;
  line-height: 1.8;
}

.muted {
  color: var(--muted);
  font-size: 12.5px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}

.order-item:last-child {
  border-bottom: none;
}

.order-item img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f1ea;
}

.oi-info {
  flex: 1;
  min-width: 0;
}

.oi-info p {
  font-size: 12.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--ink-2);
  padding: 5px 0;
}

.row.total {
  border-top: 1px dashed var(--line);
  margin-top: 6px;
  padding-top: 10px;
  font-size: 13.5px;
  color: var(--ink);
}

.row.total .price {
  font-size: 19px;
}

.ship-btn {
  border-radius: 999px;
  height: 44px;
}

.ship-tip {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
  padding-left: 90px;
}
</style>
