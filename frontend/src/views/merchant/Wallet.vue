<template>
  <div class="wallet-page">
    <!-- 钱包概览 -->
    <div class="wallet-cards">
      <div class="panel card primary">
        <div class="card-label">可提现余额（元）</div>
        <div class="card-value">{{ money(wallet.balance) }}</div>
        <div class="card-desc">订单确认收货后货款自动入账</div>
        <el-button type="primary" round class="withdraw-btn" @click="openApply">发起提现</el-button>
      </div>
      <div class="panel card">
        <div class="card-label">未入账金额（元）</div>
        <div class="card-value muted">{{ money(wallet.unsettled) }}</div>
        <div class="card-desc">已支付未完成的订单货款</div>
      </div>
      <div class="panel card">
        <div class="card-label">审核中（元）</div>
        <div class="card-value warn">{{ money(wallet.pendingWithdraw) }}</div>
        <div class="card-desc">已提交、待平台审核</div>
      </div>
      <div class="panel card">
        <div class="card-label">累计已提现（元）</div>
        <div class="card-value green">{{ money(wallet.totalWithdrawn) }}</div>
        <div class="card-desc">历史成功打款总额</div>
      </div>
    </div>

    <!-- 提现记录 -->
    <div class="panel table-card">
      <div class="table-head">
        <h3>提现记录</h3>
        <div class="filters">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
            <el-option label="待审核" :value="0" />
            <el-option label="已打款" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
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
        <el-table-column label="收款账号" min-width="180">
          <template #default="{ row }">
            <span class="account">{{ maskAccount(row.accountNo) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" round>{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <span class="remark">{{ row.auditRemark || (row.status === 1 ? '已打款至您的收款账户' : '—') }}</span>
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

    <!-- 发起提现弹窗 -->
    <el-dialog v-model="applyVisible" title="发起提现" width="480px" :close-on-click-modal="false">
      <div class="balance-tip">
        当前可提现余额 <b>¥{{ money(wallet.balance) }}</b>
      </div>
      <el-form :model="form" label-width="90px">
        <el-form-item label="提现方式" required>
          <el-radio-group v-model="form.method">
            <el-radio-button :value="1">银行卡</el-radio-button>
            <el-radio-button :value="2">微信</el-radio-button>
            <el-radio-button :value="3">支付宝</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.method === 1" label="持卡人">
          <el-input v-model="form.accountName" placeholder="请输入持卡人姓名" maxlength="32" />
        </el-form-item>
        <el-form-item :label="form.method === 1 ? '银行卡号' : '收款账号'">
          <el-input
            v-model="form.accountNo"
            :placeholder="form.method === 1 ? '请输入银行卡号' : '请输入' + methodText(form.method) + '账号'"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="提现金额" required>
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :max="Number(wallet.balance) || 0"
            :precision="2"
            :step="100"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api'
import { money, formatTime } from '@/utils/format'

const wallet = ref({})
const list = ref([])
const total = ref(0)
const loading = ref(false)
const applyVisible = ref(false)
const submitting = ref(false)

const query = reactive({ status: undefined, pageNum: 1, pageSize: 10 })
const form = reactive({ method: 1, accountName: '', accountNo: '', amount: 0 })

const methodText = (m) => ({ 1: '银行卡', 2: '微信', 3: '支付宝' }[m] || '未知')
const statusText = (s) => ({ 0: '待审核', 1: '已打款', 2: '已驳回' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

const maskAccount = (no) => {
  if (!no) return '—'
  if (no.length <= 8) return no
  return no.slice(0, 4) + ' **** ' + no.slice(-4)
}

const loadWallet = async () => {
  wallet.value = await merchantApi.wallet()
}

const load = async () => {
  loading.value = true
  try {
    const data = await merchantApi.withdrawPage({
      status: query.status,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const changePage = (page) => {
  query.pageNum = page
  load()
}

const openApply = () => {
  if (!Number(wallet.value.balance)) {
    ElMessage.warning('暂无可提现余额')
    return
  }
  form.method = 1
  form.accountName = ''
  form.accountNo = ''
  form.amount = 0
  applyVisible.value = true
}

const submitApply = async () => {
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入提现金额')
    return
  }
  if (!form.accountNo.trim()) {
    ElMessage.warning(form.method === 1 ? '请输入银行卡号' : '请输入收款账号')
    return
  }
  if (form.method === 1 && !form.accountName.trim()) {
    ElMessage.warning('请输入持卡人姓名')
    return
  }
  submitting.value = true
  try {
    await merchantApi.withdrawApply({
      amount: form.amount,
      method: form.method,
      accountNo: form.accountNo.trim(),
      accountName: form.accountName.trim()
    })
    ElMessage.success('提现申请已提交，等待平台审核')
    applyVisible.value = false
    await Promise.all([loadWallet(), load()])
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadWallet()
  load()
})
</script>

<style scoped>
.wallet-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wallet-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card.primary {
  background: linear-gradient(135deg, #fff6f0, #fdf3ec);
  border-color: #f8e2d5;
}

.card-label {
  font-size: 12.5px;
  color: var(--muted);
}

.card-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--primary);
}

.card-value.muted {
  color: var(--ink-2);
}

.card-value.warn {
  color: #e6a23c;
}

.card-value.green {
  color: #1f7a5c;
}

.card-desc {
  font-size: 11.5px;
  color: var(--muted);
}

.withdraw-btn {
  margin-top: 6px;
  align-self: flex-start;
}

.table-card {
  padding: 20px 22px;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.table-head h3 {
  font-size: 16px;
}

.amount {
  font-weight: 600;
  color: var(--primary);
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

.remark {
  font-size: 12.5px;
  color: var(--ink-2);
}

.balance-tip {
  background: var(--primary-soft);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 18px;
  font-size: 13px;
  color: var(--ink-2);
}

.balance-tip b {
  color: var(--primary);
  font-size: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .wallet-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
