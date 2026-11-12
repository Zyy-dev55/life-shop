<template>
  <el-dialog
    :model-value="modelValue"
    title="收银台"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div class="pay-amount">
      <span class="label">需付款</span>
      <span class="num"><i>¥</i>{{ money(amount) }}</span>
      <p class="order-no" v-if="orderNo">订单号 {{ orderNo }}</p>
    </div>

    <p class="section-title">选择支付方式</p>
    <div class="methods">
      <button
        v-for="m in METHODS"
        :key="m.value"
        :class="['method', { active: method === m.value }]"
        @click="method = m.value"
      >
        <span class="icon" :style="{ background: m.color }">{{ m.icon }}</span>
        <span class="meta">
          <strong>{{ m.label }}</strong>
          <em>{{ m.desc }}</em>
        </span>
        <span class="check" v-if="method === m.value">✓</span>
      </button>
    </div>

    <p class="tip">演示环境不会产生真实扣款，点击「确认支付」直接把订单置为待发货。</p>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="confirm">确认支付</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { money } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  amount: { type: [Number, String], default: 0 },
  orderNo: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const METHODS = [
  { value: 'wechat', label: '微信支付', desc: '推荐使用，扫码或免密支付', icon: '微', color: '#09bb07' },
  { value: 'alipay', label: '支付宝', desc: '花呗可用，支持分期', icon: '支', color: '#1677ff' },
  { value: 'balance', label: '余额支付', desc: '当前余额 ¥999.00', icon: '余', color: '#e8734a' }
]

const method = ref('wechat')

watch(
  () => props.modelValue,
  (v) => {
    if (v) method.value = 'wechat'
  }
)

const confirm = () => emit('confirm', method.value)
</script>

<style scoped>
.pay-amount {
  text-align: center;
  padding: 8px 0 18px;
  border-bottom: 1px dashed var(--line);
}

.pay-amount .label {
  font-size: 12.5px;
  color: var(--muted);
}

.pay-amount .num {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: var(--primary-deep);
  margin-top: 4px;
}

.pay-amount .num i {
  font-size: 20px;
  font-style: normal;
  margin-right: 2px;
}

.order-no {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 6px;
}

.section-title {
  font-size: 13px;
  color: var(--ink-2);
  margin: 18px 0 10px;
}

.methods {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.method {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s;
}

.method:hover {
  border-color: #f0c9b2;
}

.method.active {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.method .icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 14px;
  flex-shrink: 0;
}

.method .meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.method .meta strong {
  font-size: 13.5px;
}

.method .meta em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--muted);
}

.method .check {
  color: var(--primary);
  font-weight: 700;
}

.tip {
  margin-top: 14px;
  font-size: 11.5px;
  color: var(--muted);
  line-height: 1.7;
}
</style>
