<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="昵称 / 账号 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="正常" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <span class="hint">共 {{ total }} 位会员</span>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="38" :src="row.avatar">{{ (row.nickname || row.username).slice(0, 1) }}</el-avatar>
              <div>
                <p class="user-name">{{ row.nickname || row.username }}</p>
                <span class="user-account">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="性别" width="90">
          <template #default="{ row }">{{ ['保密', '男', '女'][row.gender] || '保密' }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="账号状态" width="130">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态说明" min-width="140">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round>
              {{ row.status === 1 ? '正常使用' : '已禁用' }}
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

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="createVisible" title="新增用户" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="登录账号，3-20 位" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="可选，默认用账号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="可选" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
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
    const data = await adminApi.userPage({
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
    await ElMessageBox.confirm(`禁用后用户「${row.nickname || row.username}」将无法登录，确定吗？`, '提示', {
      type: 'warning'
    })
  }
  await adminApi.userStatus(row.id, status)
  row.status = status
  ElMessage.success(status === 1 ? '已启用' : '已禁用')
}

// 新增用户
const createVisible = ref(false)
const creating = ref(false)
const formRef = ref(null)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  gender: 0
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const openCreate = () => {
  Object.keys(form).forEach((k) => {
    form[k] = k === 'gender' ? 0 : ''
  })
  createVisible.value = true
}

const submitCreate = async () => {
  await formRef.value.validate()
  creating.value = true
  try {
    await adminApi.userCreate({ ...form })
    ElMessage.success('用户新增成功')
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

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hint {
  font-size: 12px;
  color: var(--muted);
}

.table-card {
  padding: 8px 20px 20px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 13.5px;
  font-weight: 500;
}

.user-account {
  font-size: 11.5px;
  color: var(--muted);
}
</style>
