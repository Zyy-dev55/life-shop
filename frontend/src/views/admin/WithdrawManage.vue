<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="店铺名称 / 收款账号"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="待审核" :value="0" />
          <el-option label="已打款" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <span class="hint">共 {{ total }} 条提现申请</span>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="店铺" min-width="180">
          <template #default="{ row }">
            <p class="shop-name">{{ row.merchantName }}</p>
            <span class="shop-id">ID: {{ row.merchantId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提现金额" width="130">
          <template #default="{ row }">
            <span class="amount">¥{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提现方式" width="110">
          <template #default="{ row }">
            <span class="method-tag" :class="'m' + row.method">{{ methodText(row.method) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收款账号" min-width="170">
          <template #default="{ row }">
            <span class="account">{{ row.accountNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="持卡人" width="100">
          <template #default="{ row }">{{ row.accountName || '—' }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" round>{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="audit(row, 1)">通过</el-button>
              <el-button size="small" type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <span v-else class="done-tip">{{ row.status === 1 ? '已打款' : '已驳回' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.pageSize"
        class="pager"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="changePage"
      />
    </div>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回提现" width="440px" :close-on-click-modal="false">
      <p class="reject-tip">
        将驳回 <b>{{ current && current.merchantName }}</b> 的 ¥{{ current ? money(current.amount) : '' }} 提现申请，资金会退回商家余额。
      </p>
      <el-input
        v-model="rejectRemark"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="请输入驳回原因（必填）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { money, formatTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const rejectVisible = ref(false)
const current = ref(null)
const rejectRemark = ref('')

const query = reactive({ status: undefined, keyword: '', pageNum: 1, pageSize: 10 })

const methodText = (m) => ({ 1: '银行卡', 2: '微信', 3: '支付宝' }[m] || '未知')
const statusText = (s) => ({ 0: '待审核', 1: '已打款', 2: '已驳回' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.withdrawPage({
      status: query.status,
      keyword: query.keyword,
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
  query.status = undefined
  query.keyword = ''
  query.pageNum = 1
  load()
}

const changePage = (page) => {
  query.pageNum = page
  load()
}

const audit = async (row, status) => {
  const action = status === 1 ? '通过并打款' : '驳回'
  await ElMessageBox.confirm(
    `确认${action}「${row.merchantName}」的 ¥${money(row.amount)} 提现申请吗？`,
    '审核确认',
    { type: status === 1 ? 'success' : 'warning' }
  )
  submitting.value = true
  try {
    await adminApi.withdrawAudit(row.id, { status, remark: '' })
    ElMessage.success(status === 1 ? '已通过并打款' : '已驳回')
    load()
  } finally {
    submitting.value = false
  }
}

const openReject = (row) => {
  current.value = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await adminApi.withdrawAudit(current.value.id, { status: 2, remark: rejectRemark.value.trim() })
    ElMessage.success('已驳回，资金已退回商家余额')
    rejectVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.amount {
  font-weight: 600;
  color: var(--primary);
}

.shop-name {
  font-weight: 500;
}

.shop-id {
  font-size: 11.5px;
  color: var(--muted);
}

.method-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
}

.method-tag.m1 {
  color: #1f7a5c;
  background: #eef8f3;
}

.method-tag.m2 {
  color: #2e9e44;
  background: #e9f7ed;
}

.method-tag.m3 {
  color: #1677ff;
  background: #e8f1ff;
}

.account {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
}

.done-tip {
  font-size: 12.5px;
  color: var(--muted);
}

.reject-tip {
  font-size: 13px;
  color: var(--ink-2);
  margin-bottom: 14px;
  line-height: 1.6;
}

.reject-tip b {
  color: var(--ink);
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
