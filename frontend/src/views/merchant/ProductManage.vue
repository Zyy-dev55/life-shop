<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input v-model="query.keyword" placeholder="搜索本店商品" clearable style="width: 190px" @keyup.enter="search" />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 140px">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
          <el-option label="在售中" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" plain @click="openDialog()">
        <el-icon><Plus /></el-icon>
        发布商品
      </el-button>
    </div>

    <div class="panel table-card">
      <div class="table-tip">
        这里只显示本店铺的商品，其它店铺的商品不会出现在列表中。
      </div>
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
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="价格" width="125">
          <template #default="{ row }">
            <span class="price">¥{{ money(row.price) }}</span>
            <span v-if="row.originalPrice" class="origin">¥{{ money(row.originalPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.stock < 10 }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="75" />
        <el-table-column label="评价" width="110">
          <template #default="{ row }">
            <span class="rate">★ {{ row.rating || '5.0' }}</span>
            <span class="rate-count">({{ row.reviewCount || 0 }})</span>
          </template>
        </el-table-column>
        <el-table-column label="上架" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(val) => toggleStatus(row, val)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '发布商品'" width="680px" top="6vh">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" maxlength="120" placeholder="如：麦饭石不粘炒锅 30cm" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="255" placeholder="一句话卖点，如：无油烟不粘 / 少油更健康" />
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
        <el-form-item label="商品主图">
          <div class="image-row">
            <el-input v-model="form.mainImage" placeholder="/products/p1-1.jpg 或上传图片" />
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="uploadMain"
              accept="image/*"
            >
              <el-button :loading="uploading">{{ uploading ? '上传中' : '上传' }}</el-button>
            </el-upload>
          </div>
          <img v-if="form.mainImage" :src="form.mainImage" class="preview" @error="onImgError" />
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
import { merchantApi, productApi, uploadApi } from '@/api'
import { money, onImgError } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
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
    const data = await merchantApi.productPage({
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
  categories.value = await productApi.categories()
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

const beforeUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  return true
}

const uploadMain = async ({ file }) => {
  uploading.value = true
  try {
    const data = await uploadApi.image(file)
    form.value.mainImage = data.url
    if (!form.value.images) {
      form.value.images = data.url
    }
    ElMessage.success('图片已上传')
  } finally {
    uploading.value = false
  }
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
      await merchantApi.productUpdate(data)
    } else {
      if (!data.mainImage) {
        ElMessage.warning('请先设置商品主图')
        return
      }
      if (!data.images) {
        data.images = data.mainImage
      }
      await merchantApi.productSave(data)
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row, val) => {
  await merchantApi.productStatus(row.id, val ? 1 : 0)
  row.status = val ? 1 : 0
  ElMessage.success(val ? '已上架' : '已下架')
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', { type: 'warning' })
  await merchantApi.productDelete(row.id)
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

.table-tip {
  margin: 12px 0 4px;
  padding: 8px 12px;
  background: #f2f9f5;
  border-radius: 8px;
  font-size: 12px;
  color: #1f7a5c;
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

.rate {
  font-size: 12.5px;
  color: #d68a1e;
}

.rate-count {
  font-size: 11px;
  color: var(--muted);
  margin-left: 2px;
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

.preview {
  width: 74px;
  height: 74px;
  border-radius: 10px;
  object-fit: cover;
  margin-top: 8px;
  background: #f6f1ea;
}
</style>
