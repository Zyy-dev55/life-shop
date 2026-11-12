<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="店铺名称 / 联系人 / 手机号"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="营业中" :value="1" />
          <el-option label="已停业" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增商家
      </el-button>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="店铺" min-width="220">
          <template #default="{ row }">
            <div class="shop-cell">
              <el-avatar :size="40" :src="row.logo || ''">{{ (row.name || '店').slice(0, 1) }}</el-avatar>
              <div>
                <p class="shop-name">{{ row.name }}</p>
                <span class="shop-account">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系人" width="120">
          <template #default="{ row }">{{ row.contact || '—' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="在售商品" width="100" align="center">
          <template #default="{ row }">{{ row.productCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="入驻时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="经营状态" width="150">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态说明" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round>
              {{ row.status === 1 ? '营业中' : '已停业' }}
            </el-tag>
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

    <!-- 新增商家弹窗 -->
    <el-dialog v-model="createVisible" title="新增商家" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商家账号" prop="username">
          <el-input v-model="form.username" placeholder="登录账号，3-20 位" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="例如：悦家厨房旗舰店" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="店主姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="经营地址" />
        </el-form-item>
        <el-form-item label="店铺简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="一句话介绍店铺" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">确定新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.merchantPage({
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

const toggleStatus = async (row, val) => {
  const status = val ? 1 : 0
  if (!val) {
    await ElMessageBox.confirm(
      `停业后店铺「${row.name}」的商品将不可售，商家账号也无法登录，确定吗？`,
      '提示',
      { type: 'warning' }
    )
  }
  await adminApi.merchantStatus(row.id, status)
  row.status = status
  ElMessage.success(status === 1 ? '已恢复营业' : '已停业')
}

// 新增
const createVisible = ref(false)
const creating = ref(false)
const formRef = ref(null)
const form = reactive({
  username: '',
  password: '',
  shopName: '',
  contact: '',
  phone: '',
  address: '',
  description: ''
})

const rules = {
  username: [{ required: true, message: '请输入商家账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }]
}

const openCreate = () => {
  Object.keys(form).forEach((k) => (form[k] = ''))
  createVisible.value = true
}

const submitCreate = async () => {
  await formRef.value.validate()
  creating.value = true
  try {
    await adminApi.merchantCreate({ ...form })
    ElMessage.success('商家新增成功')
    createVisible.value = false
    load()
  } finally {
    creating.value = false
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

.table-card {
  padding: 8px 20px 20px;
}

.shop-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shop-cell .el-avatar {
  background: linear-gradient(135deg, #f08a5d, var(--primary));
  color: #fff;
  flex-shrink: 0;
}

.shop-name {
  font-size: 13.5px;
  font-weight: 500;
}

.shop-account {
  font-size: 11.5px;
  color: var(--muted);
}
</style>
