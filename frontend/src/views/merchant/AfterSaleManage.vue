<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <span class="hint">本店共 {{ total }} 条售后申请</span>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="商品" min-width="220">
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

        <el-table-column label="申请原因" min-width="180">
          <template #default="{ row }">
            <p class="reason">{{ row.reason }}</p>
            <span v-if="row.description" class="desc">{{ row.description }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">
            <span class="time">{{ formatTime(row.applyTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="handle(row, 1)">同意</el-button>
              <el-button size="small" type="danger" plain @click="handle(row, 2)">拒绝</el-button>
            </template>
            <template v-else-if="row.status === 2">
              <el-button size="small" type="primary" @click="confirmRefund(row)">确认收货退款</el-button>
            </template>
            <span v-else class="done-tip">{{ row.status === 1 ? '等待买家寄回' : '—' }}</span>
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

    <!-- 处理弹窗 -->
    <el-dialog
      v-model="handleVisible"
      :title="handleAction === 1 ? '同意售后申请' : '拒绝售后申请'"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="handleAction === 1 ? '可填写退货地址等说明（选填）' : '请填写拒绝原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button :type="handleAction === 1 ? 'success' : 'danger'" :loading="submitting" @click="submitHandle">
          确认{{ handleAction === 1 ? '同意' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi } from '@/api'
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
    const data = await merchantApi.afterSalePage({
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

const handleVisible = ref(false)
const handleAction = ref(1)
const handleForm = reactive({ remark: '' })
const handleId = ref(null)
const submitting = ref(false)

const handle = (row, action) => {
  handleId.value = row.id
  handleAction.value = action
  handleForm.remark = ''
  handleVisible.value = true
}

const submitHandle = async () => {
  if (handleAction.value === 2 && !handleForm.remark) {
    ElMessage.warning('拒绝时请填写原因')
    return
  }
  submitting.value = true
  try {
    await merchantApi.afterSaleHandle(handleId.value, {
      action: handleAction.value,
      remark: handleForm.remark
    })
    ElMessage.success('处理成功')
    handleVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

const confirmRefund = (row) => {
  ElMessageBox.confirm(
    `确认已收到买家退回的商品，并同意退款 ¥${money(row.amount)} 吗？`,
    '确认退款',
    { type: 'warning' }
  )
    .then(async () => {
      await merchantApi.afterSaleConfirmRefund(row.id)
      ElMessage.success('已退款')
      load()
    })
    .catch(() => {})
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
.reason {
  margin: 0 0 4px;
}
.desc {
  font-size: 12px;
  color: var(--text-secondary);
}
.time {
  font-size: 13px;
  color: var(--text-secondary);
}
.done-tip {
  color: var(--text-secondary);
  font-size: 13px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
