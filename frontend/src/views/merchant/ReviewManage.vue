<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="评价内容 / 商品 / 用户 / 订单号"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        />
        <el-select v-model="query.rating" placeholder="全部评分" clearable style="width: 120px">
          <el-option v-for="star in [5, 4, 3, 2, 1]" :key="star" :label="star + ' 星'" :value="star" />
        </el-select>
        <el-select v-model="query.replied" placeholder="全部回复状态" clearable style="width: 150px">
          <el-option label="待回复" :value="0" />
          <el-option label="已回复" :value="1" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <span class="hint">本店共 {{ total }} 条评价</span>
    </div>

    <div class="panel table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="商品" min-width="205">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.productImage" @error="onImgError" />
              <div>
                <p class="p-name">{{ row.productName }}</p>
                <span class="order-no">{{ row.orderNo }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="用户" width="110">
          <template #default="{ row }">
            <span>{{ row.userName }}</span>
            <el-tag v-if="row.anonymous === 1" size="small" type="info" effect="plain">匿名</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="135">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
          </template>
        </el-table-column>

        <el-table-column label="评价内容" min-width="250">
          <template #default="{ row }">
            <p class="content">{{ row.content || '（未填写内容）' }}</p>
            <div class="shots" v-if="row.images && row.images.length">
              <el-image
                v-for="(img, index) in row.images"
                :key="index"
                :src="img"
                :preview-src-list="row.images"
                :initial-index="index"
                fit="cover"
                class="shot"
                preview-teleported
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="我的回复" min-width="190">
          <template #default="{ row }">
            <p v-if="row.reply" class="reply">{{ row.reply }}</p>
            <span v-else class="no-reply">待回复</span>
          </template>
        </el-table-column>

        <el-table-column label="评价时间" width="155">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>

        <el-table-column label="操作" width="105" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openReply(row)">
              {{ row.reply ? '改回复' : '回复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.pageSize"
        class="pager"
        layout="total, prev, pager, next"
        background
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        @current-change="changePage"
      />
    </div>

    <el-dialog v-model="replyVisible" title="回复买家评价" width="520px">
      <div class="reply-quote" v-if="current">
        <el-rate :model-value="current.rating" disabled size="small" />
        <p class="quote-text">{{ current.content || '（未填写内容）' }}</p>
        <span class="quote-user">—— {{ current.userName }}</span>
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="回复会展示在商品详情页，友善沟通能提升复购～"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api'
import { onImgError } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', rating: null, replied: null, pageNum: 1, pageSize: 10 })

const replyVisible = ref(false)
const replyText = ref('')
const replying = ref(false)
const current = ref(null)

const load = async () => {
  loading.value = true
  try {
    const data = await merchantApi.reviewPage({
      keyword: query.keyword || undefined,
      rating: query.rating || undefined,
      replied: query.replied === null || query.replied === '' ? undefined : query.replied,
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
  query.rating = null
  query.replied = null
  query.pageNum = 1
  load()
}

const changePage = (page) => {
  query.pageNum = page
  load()
}

const openReply = (row) => {
  current.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}

const submitReply = async () => {
  if (!replyText.value.trim()) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replying.value = true
  try {
    await merchantApi.reviewReply(current.value.id, replyText.value.trim())
    ElMessage.success('回复成功')
    replyVisible.value = false
    load()
  } finally {
    replying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.manage-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  flex-wrap: wrap;
  gap: 12px;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hint {
  font-size: 12.5px;
  color: var(--muted);
}

.table-card {
  padding: 6px 12px 16px;
}

.product-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.product-cell img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f1ea;
}

.p-name {
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.order-no {
  font-size: 11px;
  color: var(--muted);
}

.content {
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--ink-2);
}

.shots {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.shot {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  cursor: zoom-in;
}

.reply {
  font-size: 12.5px;
  line-height: 1.8;
  color: #1f7a5c;
  background: #f2f9f5;
  border-radius: 8px;
  padding: 6px 9px;
}

.no-reply {
  font-size: 12px;
  color: var(--primary);
}

.reply-quote {
  background: #faf7f2;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 14px;
}

.quote-text {
  font-size: 13px;
  line-height: 1.8;
  margin: 6px 0;
}

.quote-user {
  font-size: 12px;
  color: var(--muted);
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
