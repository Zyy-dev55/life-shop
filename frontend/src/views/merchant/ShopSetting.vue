<template>
  <div class="setting" v-loading="loading">
    <div class="panel intro">
      <div class="shop-card">
        <div class="logo">
          <img v-if="form.logo" :src="form.logo" @error="onImgError" />
          <span v-else>{{ (form.name || '店').slice(0, 1) }}</span>
        </div>
        <div class="shop-meta">
          <h2>{{ form.name || '未命名店铺' }}</h2>
          <p>{{ form.description || '还没有店铺简介，写一句让买家记住你的话' }}</p>
          <div class="chips">
            <span class="chip">账号：{{ base.username || '—' }}</span>
            <span class="chip">在售 {{ base.productCount || 0 }} 件</span>
            <span class="chip" v-if="base.createTime">入驻于 {{ base.createTime.slice(0, 10) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel form-card">
      <div class="card-head">
        <h3>店铺资料</h3>
        <span class="hint">这些信息会展示在商品详情页的店铺信息里</span>
      </div>

      <el-form :model="form" label-width="92px" class="form">
        <el-form-item label="店铺名称" required>
          <el-input v-model="form.name" maxlength="64" placeholder="如：悦家厨房旗舰店" />
        </el-form-item>

        <el-form-item label="店铺 Logo">
          <div class="image-row">
            <el-input v-model="form.logo" placeholder="图片地址，或直接上传" />
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="uploadLogo"
              accept="image/*"
            >
              <el-button :loading="uploading">{{ uploading ? '上传中' : '上传' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="店铺简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="介绍你的主营品类、发货时效、售后承诺"
          />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="联系人">
            <el-input v-model="form.contact" maxlength="32" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" maxlength="20" />
          </el-form-item>
        </div>

        <el-form-item label="发货地址">
          <el-input v-model="form.address" maxlength="255" placeholder="仓库或办公地址" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
          <el-button @click="reset">还原</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel tips-card">
      <h3>经营提醒</h3>
      <ul>
        <li>商品发布后需保持库存充足，库存为 0 时买家无法下单。</li>
        <li>买家付款后请尽快发货，长期未发货会影响店铺评分。</li>
        <li>评价回复会展示在商品详情页，建议 24 小时内处理待回复评价。</li>
        <li>店铺资料修改后立即生效，无需等待审核。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi, uploadApi } from '@/api'
import { onImgError } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)

const base = ref({})
const form = reactive({
  name: '',
  logo: '',
  description: '',
  contact: '',
  phone: '',
  address: ''
})

const refreshShop = inject('refreshShop', () => {})

const fill = (data) => {
  base.value = data
  form.name = data.name || ''
  form.logo = data.logo || ''
  form.description = data.description || ''
  form.contact = data.contact || ''
  form.phone = data.phone || ''
  form.address = data.address || ''
}

const load = async () => {
  loading.value = true
  try {
    fill(await merchantApi.profile())
  } finally {
    loading.value = false
  }
}

const reset = () => fill(base.value)

const beforeUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  return true
}

const uploadLogo = async ({ file }) => {
  uploading.value = true
  try {
    const data = await uploadApi.image(file)
    form.logo = data.url
    ElMessage.success('图片已上传')
  } finally {
    uploading.value = false
  }
}

const save = async () => {
  if (!form.name.trim()) {
    ElMessage.warning('店铺名称不能为空')
    return
  }
  saving.value = true
  try {
    const data = await merchantApi.updateProfile({ ...form })
    fill(data)
    refreshShop()
    ElMessage.success('店铺资料已更新')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.setting {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 940px;
}

.intro {
  padding: 22px 24px;
}

.shop-card {
  display: flex;
  align-items: center;
  gap: 18px;
}

.logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, #3fae86, #1f7a5c);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shop-meta h2 {
  font-size: 19px;
  font-weight: 600;
  margin-bottom: 6px;
}

.shop-meta p {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.7;
  margin-bottom: 10px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  font-size: 11.5px;
  color: #1f7a5c;
  background: #eef8f3;
  border-radius: 999px;
  padding: 3px 10px;
}

.form-card {
  padding: 20px 24px 8px;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
}

.card-head h3 {
  font-size: 15px;
  font-weight: 600;
}

.hint {
  font-size: 11.5px;
  color: var(--muted);
}

.form {
  max-width: 660px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-row :deep(.el-form-item) {
  margin-bottom: 18px;
}

.image-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.tips-card {
  padding: 18px 24px 20px;
}

.tips-card h3 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.tips-card ul {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.tips-card li {
  position: relative;
  padding-left: 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 1.7;
}

.tips-card li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #3fae86;
}
</style>
