<template>
  <el-dialog
    :model-value="modelValue"
    title="申请售后"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="as-dialog">
      <div class="product-row" v-if="item">
        <img :src="item.productImage" @error="onImgError" />
        <div>
          <p class="p-name">{{ item.productName }}</p>
          <p class="p-meta">¥{{ money(item.price) }} × {{ item.quantity }}</p>
        </div>
      </div>

      <el-form label-width="90px" class="as-form">
        <el-form-item label="售后类型">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">仅退款</el-radio>
            <el-radio :label="2">退货退款</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="申请原因">
          <el-select v-model="form.reason" placeholder="请选择申请原因" style="width: 100%">
            <el-option v-for="r in reasons" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>

        <el-form-item label="退款金额">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :max="maxAmount"
            :precision="2"
            style="width: 100%"
          />
          <span class="amount-tip">最多可退 ¥{{ money(maxAmount) }}</span>
        </el-form-item>

        <el-form-item label="问题描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请描述遇到的问题，便于商家快速处理"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { afterSaleApi } from '@/api'
import { money, onImgError } from '@/utils/format'

const props = defineProps({
  modelValue: Boolean,
  item: Object
})
const emit = defineEmits(['update:modelValue', 'success'])

const reasons = [
  '质量问题',
  '商品与描述不符',
  '收到商品破损',
  '少件/漏发',
  '不想要了',
  '拍错/买错',
  '其他'
]

const form = reactive({ type: 2, reason: '', amount: 0, description: '' })
const submitting = ref(false)

const maxAmount = computed(() => (props.item ? Number(props.item.totalPrice) || 0 : 0))

watch(
  () => props.item,
  (val) => {
    if (val) {
      form.type = 2
      form.reason = ''
      form.amount = Number(val.totalPrice) || 0
      form.description = ''
    }
  }
)

const submit = async () => {
  if (!props.item) return
  if (!form.reason) {
    ElMessage.warning('请选择申请原因')
    return
  }
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请填写退款金额')
    return
  }
  submitting.value = true
  try {
    await afterSaleApi.apply({
      orderItemId: props.item.id,
      type: form.type,
      reason: form.reason,
      amount: form.amount,
      description: form.description
    })
    ElMessage.success('售后申请已提交，等待商家处理')
    emit('update:modelValue', false)
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.product-row {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--bg);
  border-radius: 8px;
  margin-bottom: 16px;
}
.product-row img {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
}
.p-name {
  font-weight: 600;
  margin: 0 0 6px;
}
.p-meta {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0;
}
.as-form {
  margin-top: 4px;
}
.amount-tip {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 4px;
}
</style>
