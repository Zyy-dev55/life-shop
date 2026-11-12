<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input v-model="query.keyword" placeholder="搜索商品名称" clearable style="width: 200px" @keyup.enter="search" />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="上架中" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" plain @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增商品
      </el-button>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="goods-cell">
              <img :src="row.mainImage" @error="onImgError" />
              <div class="goods-text">
                <p class="goods-name">{{ row.name }}</p>
                <span class="goods-sub">{{ row.subtitle || '—' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="105" />
        <el-table-column label="所属店铺" width="150">
          <template #default="{ row }">
            <span class="shop-name">{{ row.merchantName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="130">
          <template #default="{ row }">
            <span class="price">¥{{ money(row.price) }}</span>
            <span v-if="row.originalPrice" class="origin">¥{{ money(row.originalPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.stock < 20 }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="680px" top="6vh">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" maxlength="120" placeholder="如：麦饭石不粘炒锅 30cm" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="255" placeholder="卖点，如：无油烟不粘 / 少油更健康" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="所属分类" required>
            <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="单位">
            <el-input v-model="form.unit" placeholder="件 / 套 / 瓶" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="售价" required>
            <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="原价">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="库存" required>
            <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">上架</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="主图地址">
          <el-input v-model="form.mainImage" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="组图地址">
          <el-input
            v-model="form.images"
            type="textarea"
            :rows="2"
            placeholder="多张图片用英文逗号分隔"
          />
        </el-form-item>
        <el-form-item label="商品详情">
          <el-input v-model="form.detail" type="textarea" :rows="4" placeholder="材质、尺寸、使用说明等" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { money, onImgError } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const dialogVisible = ref(false)

const query = reactive({
  keyword: '',
  categoryId: null,
  status: null,
  pageNum: 1,
  pageSize: 10
})

const emptyForm = () => ({
  id: null,
  name: '',
  subtitle: '',
  categoryId: null,
  price: 0,
  originalPrice: 0,
  stock: 0,
  unit: '件',
  status: 1,
  mainImage: '',
  images: '',
  detail: ''
})

const form = ref(emptyForm())

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.productPage({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
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

const loadCategories = async () => {
  categories.value = await adminApi.categoryList()
}

const search = () => {
  query.pageNum = 1
  load()
}

const reset = () => {
  query.keyword = ''
  query.categoryId = null
  query.status = null
  query.pageNum = 1
  load()
}

const changePage = (page) => {
  query.pageNum = page
  load()
}

const openDialog = (row) => {
  form.value = row ? { ...row } : emptyForm()
  dialogVisible.value = true
}

const save = async () => {
  const data = form.value
  if (!data.name || !data.categoryId || !data.price) {
    ElMessage.warning('请填写商品名称、分类与售价')
    return
  }
  saving.value = true
  try {
    if (data.id) {
      await adminApi.productUpdate(data)
    } else {
      await adminApi.productSave(data)
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row, val) => {
  await adminApi.productStatus(row.id, val ? 1 : 0)
  row.status = val ? 1 : 0
  ElMessage.success(val ? '已上架' : '已下架')
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', { type: 'warning' })
  await adminApi.productDelete(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(() => {
  load()
  loadCategories()
})
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

.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.goods-cell img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f1ea;
  flex-shrink: 0;
}

.goods-text {
  min-width: 0;
}

.goods-name {
  font-size: 13.5px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-sub {
  font-size: 11.5px;
  color: var(--muted);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-name {
  font-size: 12px;
  color: #1f7a5c;
}

.origin {
  display: block;
  font-size: 11px;
  color: var(--muted);
  text-decoration: line-through;
}

.low-stock {
  color: var(--primary);
  font-weight: 600;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-row :deep(.el-form-item) {
  margin-bottom: 18px;
}
</style>
