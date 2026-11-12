<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div>
        <h3>商品分类</h3>
        <p class="hint">分类会展示在前台首页与筛选栏，排序值越小越靠前</p>
      </div>
      <el-button type="primary" plain @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增分类
      </el-button>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="图标" width="80">
          <template #default="{ row }">
            <span class="icon-cell">{{ row.icon || '🛍️' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="分类名称" min-width="160" />
        <el-table-column prop="sort" label="排序值" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" round>
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="20" placeholder="如：厨房用品" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="可填 emoji，如 🍳" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
import { adminApi } from '@/api'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)

const emptyForm = () => ({ id: null, name: '', icon: '', sort: 0, status: 1 })
const form = ref(emptyForm())

const load = async () => {
  loading.value = true
  try {
    list.value = await adminApi.categoryList()
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  form.value = row ? { ...row } : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name || !form.value.name.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await adminApi.categoryUpdate(form.value)
    } else {
      await adminApi.categorySave(form.value)
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
  await adminApi.categoryDelete(row.id)
  ElMessage.success('已删除')
  await load()
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
  padding: 18px 22px;
  gap: 16px;
}

.toolbar h3 {
  font-size: 15px;
}

.hint {
  font-size: 11.5px;
  color: var(--muted);
  margin-top: 4px;
}

.table-card {
  padding: 8px 20px 20px;
}

.icon-cell {
  font-size: 22px;
}
</style>
