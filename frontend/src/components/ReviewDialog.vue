<template>
  <el-dialog
    :model-value="modelValue"
    title="发表评价"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @closed="reset"
  >
    <div class="review-product" v-if="item">
      <img :src="item.productImage" @error="onImgError" />
      <div>
        <p class="name">{{ item.productName }}</p>
        <p class="meta">订单号 {{ item.orderNo }}</p>
      </div>
    </div>

    <div class="form-row">
      <span class="label">商品评分</span>
      <el-rate v-model="form.rating" :texts="RATE_TEXTS" show-text />
    </div>

    <div class="form-row align-start">
      <span class="label">评价内容</span>
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="说说这件商品怎么样，给其他人一点参考～"
      />
    </div>

    <div class="form-row align-start">
      <span class="label">晒图</span>
      <div>
        <el-upload
          v-model:file-list="fileList"
          list-type="picture-card"
          :http-request="doUpload"
          :limit="3"
          accept="image/*"
          :on-exceed="() => ElMessage.warning('最多上传 3 张图片')"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <p class="tip">支持 jpg / png，单张不超过 5MB，最多 3 张</p>
      </div>
    </div>

    <div class="form-row">
      <span class="label">匿名</span>
      <el-switch v-model="form.anonymous" :active-value="1" :inactive-value="0" />
      <span class="tip inline">开启后其他人看到的昵称会显示为「匿名用户」</span>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { reviewApi, uploadApi } from '@/api'
import { onImgError } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])

const RATE_TEXTS = ['很不满意', '不满意', '一般', '满意', '非常满意']
const fileList = ref([])
const submitting = ref(false)
const form = reactive({ rating: 5, content: '', anonymous: 0 })

const doUpload = async (options) => {
  try {
    const res = await uploadApi.image(options.file)
    options.onSuccess(res)
  } catch (e) {
    options.onError(e)
    ElMessage.error(e.message || '图片上传失败')
  }
}

const reset = () => {
  form.rating = 5
  form.content = ''
  form.anonymous = 0
  fileList.value = []
}

const submit = async () => {
  if (!props.item) return
  if (!form.rating) {
    ElMessage.warning('请先选择评分')
    return
  }
  const images = fileList.value
    .map((f) => f.response?.url || f.url)
    .filter((u) => typeof u === 'string' && u.length > 0)

  submitting.value = true
  try {
    await reviewApi.submit({
      orderId: props.item.orderId,
      productId: props.item.productId,
      rating: form.rating,
      content: form.content.trim(),
      images,
      anonymous: form.anonymous
    })
    ElMessage.success('评价成功，感谢你的分享')
    emit('update:modelValue', false)
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.review-product {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #faf7f2;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 18px;
}

.review-product img {
  width: 56px;
  height: 56px;
  border-radius: 9px;
  object-fit: cover;
  background: #f2ece4;
}

.name {
  font-size: 13.5px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.form-row.align-start {
  align-items: flex-start;
}

.form-row .label {
  width: 68px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--ink-2);
  padding-top: 6px;
}

.form-row .el-textarea {
  flex: 1;
}

.tip {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 6px;
}

.tip.inline {
  margin-top: 0;
}
</style>
