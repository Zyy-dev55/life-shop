<template>
  <div class="container address-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>收货地址</h1>
        <p class="sub">最多保存多个地址，可设置默认收货地址</p>
      </div>
      <el-button type="primary" round @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增地址
      </el-button>
    </div>

    <div v-if="list.length" class="grid">
      <div v-for="addr in list" :key="addr.id" class="card panel">
        <div class="card-top">
          <strong>{{ addr.receiver }}</strong>
          <span class="phone">{{ addr.phone }}</span>
          <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
        </div>
        <p class="detail">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</p>
        <div class="card-actions">
          <el-button text size="small" @click="openDialog(addr)">编辑</el-button>
          <el-button v-if="addr.isDefault !== 1" text size="small" @click="setDefault(addr)">设为默认</el-button>
          <el-button text size="small" type="danger" @click="remove(addr)">删除</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="panel empty">
      <el-empty description="还没有收货地址，添加一个吧">
        <el-button type="primary" @click="openDialog()">新增地址</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑地址' : '新增地址'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="form.receiver" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省 / 市 / 区">
          <div class="region-row">
            <el-input v-model="form.province" placeholder="省" />
            <el-input v-model="form.city" placeholder="市" />
            <el-input v-model="form.district" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="街道、门牌号" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addressApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return {
    id: null,
    receiver: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: 0
  }
}

const load = async () => {
  loading.value = true
  try {
    list.value = await addressApi.list()
  } finally {
    loading.value = false
  }
}

const openDialog = (addr) => {
  form.value = addr ? { ...addr } : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.receiver || !form.value.phone || !form.value.detail) {
    ElMessage.warning('请填写收货人、手机号与详细地址')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await addressApi.update(form.value)
    } else {
      await addressApi.save(form.value)
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

const setDefault = async (addr) => {
  await addressApi.setDefault(addr.id)
  ElMessage.success('已设为默认地址')
  await load()
}

const remove = async (addr) => {
  await ElMessageBox.confirm('确定删除该地址吗？', '提示', { type: 'warning' })
  await addressApi.remove(addr.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.address-page {
  padding-top: 28px;
  padding-bottom: 40px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 26px;
}

.sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s;
}

.card:hover {
  box-shadow: var(--shadow);
  border-color: #f0d3c1;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-top strong {
  font-size: 15px;
}

.phone {
  font-size: 13px;
  color: var(--ink-2);
}

.default-tag {
  font-size: 10.5px;
  color: var(--primary);
  border: 1px solid var(--primary);
  border-radius: 3px;
  padding: 0 5px;
}

.detail {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.7;
  min-height: 44px;
}

.card-actions {
  display: flex;
  gap: 4px;
  border-top: 1px dashed var(--line);
  padding-top: 10px;
}

.region-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty {
  padding: 20px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
