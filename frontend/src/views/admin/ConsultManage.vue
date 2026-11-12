<template>
  <div class="manage-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="买家昵称 / 店铺名 / 消息内容"
          clearable
          style="width: 260px"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <span class="hint">共 {{ total }} 个客服会话</span>
    </div>

    <div class="monitor-layout">
      <!-- 会话列表 -->
      <div class="panel conv-panel">
        <el-table
          :data="list"
          v-loading="loading"
          stripe
          highlight-current-row
          @row-click="select"
          :row-class-name="rowClass"
        >
          <el-table-column label="买家" min-width="150">
            <template #default="{ row }">
              <div class="cell-user">
                <el-avatar :size="32" :src="row.userAvatar || ''">{{ (row.userName || '客').slice(0, 1) }}</el-avatar>
                <span class="cell-name">{{ row.userName || '买家' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="店铺" min-width="140">
            <template #default="{ row }">
              <div class="cell-user">
                <el-avatar :size="32" :src="row.shopLogo || ''">{{ (row.shopName || '店').slice(0, 1) }}</el-avatar>
                <span class="cell-name">{{ row.shopName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="最近消息" min-width="200">
            <template #default="{ row }">{{ row.lastContent || '—' }}</template>
          </el-table-column>
          <el-table-column label="最近时间" width="150">
            <template #default="{ row }">{{ formatTime(row.lastTime) }}</template>
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

      <!-- 消息详情（只读监管） -->
      <div class="panel detail-panel">
        <template v-if="active">
          <div class="detail-head">
            <div class="detail-title">
              <strong>{{ active.shopName }}</strong>
              <span class="vs">↔</span>
              <strong>{{ active.userName || '买家' }}</strong>
            </div>
            <el-tag size="small" type="info" effect="plain">平台监管 · 只读</el-tag>
          </div>
          <div class="detail-body" ref="scrollRef" v-loading="msgLoading">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['msg', msg.senderType === 'USER' ? 'user' : 'merchant']"
            >
              <div class="msg-meta">
                <span class="msg-sender">{{ msg.senderName }}</span>
                <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
              </div>
              <div class="bubble">{{ msg.content }}</div>
            </div>
            <div v-if="!messages.length && !msgLoading" class="empty">该会话暂无消息记录</div>
          </div>
        </template>
        <div v-else class="empty">
          <el-icon :size="40"><ChatLineSquare /></el-icon>
          <p>点击左侧会话查看完整聊天记录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const msgLoading = ref(false)
const list = ref([])
const total = ref(0)
const active = ref(null)
const messages = ref([])
const scrollRef = ref(null)

const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.consultPage({
      keyword: query.keyword || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const select = async (row) => {
  active.value = row
  msgLoading.value = true
  try {
    const data = await adminApi.consultDetail(row.id)
    messages.value = data.messages || []
    await nextTick()
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  } finally {
    msgLoading.value = false
  }
}

const rowClass = ({ row }) => (active.value && row.id === active.value.id ? 'current-row' : '')

const search = () => {
  query.pageNum = 1
  load()
}

const reset = () => {
  query.keyword = ''
  query.pageNum = 1
  load()
}

const changePage = (page) => {
  query.pageNum = page
  load()
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

.hint {
  font-size: 12px;
  color: var(--muted);
}

.monitor-layout {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  align-items: start;
}

.conv-panel,
.detail-panel {
  padding: 8px 16px 16px;
}

.cell-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cell-user .el-avatar {
  background: linear-gradient(135deg, #f08a5d, var(--primary));
  color: #fff;
  font-size: 12px;
  flex-shrink: 0;
}

.cell-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  min-height: 520px;
  max-height: 640px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 4px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 12px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
}

.detail-title .vs {
  color: var(--muted);
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px;
}

.msg {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 92%;
}

.msg.user {
  align-self: flex-end;
  align-items: flex-end;
}

.msg.merchant {
  align-self: flex-start;
  align-items: flex-start;
}

.msg-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 11px;
  color: var(--muted);
}

.msg-sender {
  font-weight: 600;
  color: var(--ink-2);
}

.bubble {
  padding: 9px 12px;
  font-size: 13px;
  line-height: 1.6;
  border-radius: 12px;
  word-break: break-word;
  white-space: pre-wrap;
  background: #fff;
  border: 1px solid var(--line);
}

.msg.user .bubble {
  background: linear-gradient(135deg, #f0764a, var(--primary));
  color: #fff;
  border: none;
}

.empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
  padding: 40px 0;
}

@media (max-width: 1000px) {
  .monitor-layout {
    grid-template-columns: 1fr;
  }
}
</style>
