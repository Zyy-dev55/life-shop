<template>
  <div class="container after-sale-page">
    <div class="page-head">
      <div>
        <h1>我的售后</h1>
        <p class="sub">退款 / 退货退款进度一目了然</p>
      </div>
    </div>

    <div class="filter-row">
      <button
        v-for="f in filters"
        :key="f.value"
        :class="['chip', { active: status === f.value }]"
        @click="status = f.value; load()"
      >
        {{ f.label }}
      </button>
    </div>

    <div v-loading="loading">
      <div v-if="!list.length" class="panel empty">
        <el-empty description="暂无售后记录" />
      </div>

      <div v-else class="as-list">
        <div v-for="as in list" :key="as.id" class="as-card panel">
          <div class="as-head">
            <span class="as-type" :class="'type-' + as.type">{{ as.typeText }}</span>
            <span class="as-status" :class="'st-' + as.status">{{ as.statusText }}</span>
          </div>

          <div class="as-body">
            <img :src="as.productImage" @error="onImgError" class="as-img" />
            <div class="as-info">
              <p class="as-name">{{ as.productName }}</p>
              <p class="as-meta">
                <span>单价 ¥{{ money(as.price) }}</span>
                <span>× {{ as.quantity }}</span>
                <span class="as-amount">退款金额 ¥{{ money(as.amount) }}</span>
              </p>
              <p class="as-reason">
                原因：{{ as.reason }}
                <span v-if="as.description" class="as-desc">（{{ as.description }}）</span>
              </p>
              <p class="as-time">申请时间：{{ formatTime(as.applyTime) }}</p>
              <p v-if="as.returnTrackingNo" class="as-time">
                退货单号：{{ as.returnCompany || '' }} {{ as.returnTrackingNo }}
              </p>
              <p v-if="as.merchantRemark" class="as-remark">商家备注：{{ as.merchantRemark }}</p>
            </div>
          </div>

          <div class="as-actions">
            <template v-if="canFillReturn(as.status)">
              <el-button size="small" type="primary" @click="openReturn(as)">填写退货物流</el-button>
              <el-button size="small" plain @click="cancel(as)">撤销申请</el-button>
            </template>
            <template v-else-if="canCancel(as.status)">
              <el-button size="small" plain @click="cancel(as)">撤销申请</el-button>
            </template>
          </div>
        </div>
      </div>

      <div class="pager" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="(p) => { pageNum = p; load() }"
        />
      </div>
    </div>

    <!-- 填写退货物流弹窗 -->
    <el-dialog v-model="returnVisible" title="填写退货物流" width="460px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="快递公司">
          <el-select v-model="returnForm.company" placeholder="请选择快递公司" style="width: 100%">
            <el-option v-for="c in companies" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="returnForm.trackingNo" placeholder="请输入退货物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReturn">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { afterSaleApi } from '@/api'
import { money, onImgError, formatTime } from '@/utils/format'
import { afterSaleStatusText, afterSaleTypeText, canCancel, canFillReturn } from '@/utils/afterSale'

const loading = ref(false)
const list = ref([])
const status = ref(null)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

const filters = [
  { label: '全部', value: null },
  ...[0, 1, 2, 3, 4].map((code) => ({ label: afterSaleStatusText(code), value: code }))
]

const companies = ['顺丰速运', '圆通速递', '中通快递', '申通快递', '韵达快递', '京东物流', '邮政EMS']

const returnVisible = ref(false)
const returnForm = ref({ company: '', trackingNo: '' })
const returnId = ref(null)
const submitting = ref(false)

const load = async () => {
  loading.value = true
  try {
    const data = await afterSaleApi.my({
      status: status.value === null ? undefined : status.value,
      pageNum: pageNum.value,
      pageSize
    })
    // 后端 fillText 正常会带文案；缺失时用前端状态机兜底，避免页面出现空白标签
    list.value = (data.list || []).map((as) => ({
      ...as,
      typeText: as.typeText || afterSaleTypeText(as.type),
      statusText: as.statusText || afterSaleStatusText(as.status)
    }))
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const cancel = (as) => {
  ElMessageBox.confirm('确定撤销该售后申请吗？', '撤销申请', { type: 'warning' })
    .then(async () => {
      await afterSaleApi.cancel(as.id)
      ElMessage.success('已撤销')
      load()
    })
    .catch(() => {})
}

const openReturn = (as) => {
  returnId.value = as.id
  returnForm.value = { company: '顺丰速运', trackingNo: '' }
  returnVisible.value = true
}

const submitReturn = async () => {
  if (!returnForm.value.trackingNo) {
    ElMessage.warning('请填写退货物流单号')
    return
  }
  submitting.value = true
  try {
    await afterSaleApi.returnGoods(returnId.value, returnForm.value)
    ElMessage.success('已提交退货物流')
    returnVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.after-sale-page {
  padding-bottom: 40px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin: 24px 0 16px;
}
.page-head h1 {
  font-size: 24px;
  margin: 0 0 4px;
}
.page-head .sub {
  color: var(--text-secondary);
  margin: 0;
}
.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.chip {
  border: 1px solid var(--line);
  background: var(--bg);
  color: var(--text);
  padding: 6px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
}
.chip.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
.as-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.as-card {
  padding: 16px;
}
.as-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--line);
}
.as-type {
  font-weight: 600;
  font-size: 14px;
}
.type-1 { color: var(--primary); }
.type-2 { color: #e6a23c; }
.as-status { font-size: 13px; }
.st-0 { color: #e6a23c; }
.st-1 { color: #e6a23c; }
.st-2 { color: #e6a23c; }
.st-3 { color: #67c23a; }
.st-4 { color: #f56c6c; }
.st-5 { color: var(--text-secondary); }
.as-body {
  display: flex;
  gap: 14px;
}
.as-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.as-info { flex: 1; }
.as-name {
  font-weight: 600;
  margin: 0 0 6px;
}
.as-meta {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0 0 4px;
  display: flex;
  gap: 12px;
}
.as-amount { color: #f56c6c; font-weight: 600; }
.as-reason { font-size: 13px; margin: 0 0 4px; }
.as-desc { color: var(--text-secondary); }
.as-time { font-size: 12px; color: var(--text-secondary); margin: 0 0 2px; }
.as-remark { font-size: 13px; color: #e6a23c; margin: 4px 0 0; }
.as-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
