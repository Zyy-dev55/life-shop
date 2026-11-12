<template>
  <div class="consult-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="买家昵称 / 咨询内容 / 商品"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-checkbox v-model="query.unreadOnly" @change="search">只看未读</el-checkbox>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <div class="tool-right">
        <span class="hint">
          共 {{ page.total }} 个会话
          <template v-if="unreadTotal">· <b class="warn">{{ unreadTotal }}</b> 条待回复</template>
        </span>
        <el-button text :disabled="!unreadTotal" @click="readAll">全部标记已读</el-button>
      </div>
    </div>

    <div class="layout" v-loading="listLoading">
      <!-- 会话列表 -->
      <aside class="conv-list panel">
        <div class="conv-head">
          客户会话
          <span class="count">{{ page.total }}</span>
        </div>

        <div v-if="!conversations.length && !listLoading" class="conv-empty">
          <el-icon :size="26"><ChatDotRound /></el-icon>
          <p>暂时没有客户咨询</p>
        </div>

        <div
          v-for="c in conversations"
          :key="c.id"
          :class="['conv-item', { active: c.id === activeId }]"
          @click="select(c)"
        >
          <el-avatar :size="38" :src="c.userAvatar || ''" class="conv-avatar">
            {{ (c.userName || '客').slice(0, 1) }}
          </el-avatar>
          <div class="conv-main">
            <div class="conv-top">
              <strong>{{ c.userName || '匿名买家' }}</strong>
              <span class="conv-time">{{ shortTime(c.lastTime) }}</span>
            </div>
            <div class="conv-bottom">
              <p>
                <span v-if="c.lastSender === 'MERCHANT'" class="me-tag">我：</span>
                {{ c.lastContent || '开始咨询吧' }}
              </p>
              <span v-if="c.merchantUnread" class="unread">{{ c.merchantUnread }}</span>
            </div>
            <p v-if="c.productName" class="conv-product">{{ c.productName }}</p>
          </div>
        </div>

        <div v-if="page.total > page.pageSize" class="pager">
          <el-pagination
            small
            layout="prev, pager, next"
            :current-page="page.pageNum"
            :page-size="page.pageSize"
            :total="page.total"
            @current-change="load"
          />
        </div>
      </aside>

      <!-- 聊天区 -->
      <section class="chat-panel panel">
        <template v-if="active">
          <header class="chat-head">
            <div class="buyer">
              <el-avatar :size="34" :src="active.userAvatar || ''" class="buyer-avatar">
                {{ (active.userName || '客').slice(0, 1) }}
              </el-avatar>
              <div class="buyer-text">
                <strong>{{ active.userName || '匿名买家' }}</strong>
                <span>首次咨询 {{ fmtDate(active.createTime) }}</span>
              </div>
            </div>
            <router-link
              v-if="active.productId"
              :to="`/product/${active.productId}`"
              target="_blank"
              class="ctx-product"
            >
              <img :src="active.productImage" @error="onImgError" />
              <span>{{ active.productName }}</span>
            </router-link>
          </header>

          <div ref="scrollRef" class="chat-body" v-loading="msgLoading">
            <div class="day-line"><span>买家咨询 · 及时回复有助于提升转化</span></div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['msg', msg.senderType === 'MERCHANT' ? 'mine' : 'theirs']"
            >
              <el-avatar v-if="msg.senderType !== 'MERCHANT'" :size="30" :src="active.userAvatar || ''" class="msg-avatar">
                {{ (active.userName || '客').slice(0, 1) }}
              </el-avatar>
              <div class="bubble-wrap">
                <div class="bubble">{{ msg.content }}</div>
                <span class="time">{{ shortTime(msg.createTime) }}</span>
              </div>
            </div>
          </div>

          <footer class="chat-foot">
            <div class="templates">
              <button v-for="t in templates" :key="t" class="tpl" @click="useTemplate(t)">{{ t }}</button>
            </div>
            <el-input
              v-model="input"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="回复买家，Enter 发送 / Shift+Enter 换行"
              @keydown.enter.exact.prevent="reply"
            />
            <div class="foot-bar">
              <span class="hint">回复内容会实时推送到买家的「我的咨询」</span>
              <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="reply">
                发送回复
              </el-button>
            </div>
          </footer>
        </template>

        <div v-else class="chat-empty">
          <el-icon :size="40"><ChatLineSquare /></el-icon>
          <p>从左侧选择一个会话开始回复</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi } from '@/api'
import { onImgError } from '@/utils/format'

const refreshConsultBadge = inject('refreshConsultBadge', () => {})

const conversations = ref([])
const activeId = ref(null)
const messages = ref([])
const input = ref('')
const listLoading = ref(false)
const msgLoading = ref(false)
const sending = ref(false)
const scrollRef = ref(null)
const unreadTotal = ref(0)
let timer = null

const query = ref({ keyword: '', unreadOnly: false })
const page = ref({ total: 0, pageNum: 1, pageSize: 20 })

const active = computed(() => conversations.value.find((c) => c.id === activeId.value) || null)

const templates = [
  '您好，在的，请问有什么可以帮您？',
  '这款有现货，今天下单 48 小时内发出。',
  '满 99 元包邮，支持 7 天无理由退换。',
  '已为您备注加急，请放心。'
]

const shortTime = (value) => {
  if (!value) return ''
  const d = new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(value).slice(5, 16)
  const now = new Date()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (d.toDateString() === now.toDateString()) return `${hh}:${mm}`
  const diffDay = Math.floor((now - d) / 86400000)
  if (diffDay === 1) return '昨天'
  return `${d.getMonth() + 1}-${d.getDate()}`
}

const fmtDate = (value) => (value ? String(value).slice(0, 16) : '-')

const scrollBottom = async () => {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const loadBadge = async () => {
  try {
    unreadTotal.value = await merchantApi.consultUnread()
    refreshConsultBadge()
  } catch (e) {
    unreadTotal.value = 0
  }
}

const load = async (pageNum = 1) => {
  listLoading.value = !conversations.value.length
  try {
    const data = await merchantApi.consultPage({
      keyword: query.value.keyword || undefined,
      unreadOnly: query.value.unreadOnly ? 1 : undefined,
      pageNum,
      pageSize: page.value.pageSize
    })
    conversations.value = data.list
    page.value = { total: data.total, pageNum: data.pageNum, pageSize: data.pageSize }
    if (!activeId.value || !conversations.value.some((c) => c.id === activeId.value)) {
      if (conversations.value.length) await select(conversations.value[0])
      else {
        activeId.value = null
        messages.value = []
      }
    }
    loadBadge()
  } finally {
    listLoading.value = false
  }
}

const select = async (conv) => {
  activeId.value = conv.id
  msgLoading.value = true
  try {
    const data = await merchantApi.consultDetail(conv.id)
    messages.value = data.messages || []
    const target = conversations.value.find((c) => c.id === conv.id)
    if (target) target.merchantUnread = 0
    loadBadge()
    await scrollBottom()
  } finally {
    msgLoading.value = false
  }
}

const reply = async () => {
  const text = input.value.trim()
  if (!text || !active.value) return
  sending.value = true
  try {
    await merchantApi.consultReply(active.value.id, text)
    input.value = ''
    const data = await merchantApi.consultDetail(active.value.id)
    messages.value = data.messages || []
    const idx = conversations.value.findIndex((c) => c.id === active.value.id)
    if (idx > -1 && data.consult) conversations.value[idx] = data.consult
    await scrollBottom()
    loadBadge()
  } finally {
    sending.value = false
  }
}

const useTemplate = (text) => {
  input.value = input.value ? `${input.value}\n${text}` : text
}

const search = () => load(1)

const readAll = async () => {
  await ElMessageBox.confirm('确定把所有会话标记为已读吗？', '提示', { type: 'warning' })
  await merchantApi.consultReadAll()
  ElMessage.success('已全部标记为已读')
  conversations.value.forEach((c) => (c.merchantUnread = 0))
  unreadTotal.value = 0
  loadBadge()
}

const startPolling = () => {
  stopPolling()
  timer = setInterval(async () => {
    if (sending.value) return
    try {
      const data = await merchantApi.consultPage({
        keyword: query.value.keyword || undefined,
        unreadOnly: query.value.unreadOnly ? 1 : undefined,
        pageNum: page.value.pageNum,
        pageSize: page.value.pageSize
      })
      // 保留当前打开会话的未读态（已读）
      const list = data.list.map((c) => (c.id === activeId.value ? { ...c, merchantUnread: 0 } : c))
      conversations.value = list
      page.value = { total: data.total, pageNum: data.pageNum, pageSize: data.pageSize }
      if (activeId.value) {
        const detail = await merchantApi.consultDetail(activeId.value)
        const before = messages.value.length
        messages.value = detail.messages || []
        if (messages.value.length !== before) await scrollBottom()
      }
      loadBadge()
    } catch (e) {
      /* 轮询错误忽略 */
    }
  }, 8000)
}

const stopPolling = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  await load(1)
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.consult-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tool-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hint {
  font-size: 12.5px;
  color: var(--muted);
}

.warn {
  color: #e8612d;
}

.layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 240px);
  min-height: 520px;
}

.conv-list {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.conv-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  letter-spacing: 0.5px;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 2;
  border-bottom: 1px solid var(--line);
}

.conv-head .count {
  background: #e8f5ef;
  color: #1f7a5c;
  border-radius: 999px;
  padding: 0 7px;
  font-size: 11px;
  line-height: 17px;
}

.conv-empty {
  padding: 46px 18px;
  text-align: center;
  color: var(--muted);
  font-size: 12.5px;
}

.conv-empty p {
  margin-top: 10px;
}

.conv-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background 0.18s;
  border-bottom: 1px solid #f6f1ea;
}

.conv-item:hover {
  background: #fbf6ef;
}

.conv-item.active {
  background: #f1f8f4;
  border-left-color: #1f7a5c;
}

.conv-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #8fbfa9, #1f7a5c);
  color: #fff;
  font-size: 13px;
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conv-top strong {
  font-size: 13px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 10.5px;
  color: var(--muted);
  flex-shrink: 0;
}

.conv-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
}

.conv-bottom p {
  flex: 1;
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.me-tag {
  color: #1f7a5c;
}

.unread {
  flex-shrink: 0;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 999px;
  background: #e8612d;
  color: #fff;
  font-size: 10.5px;
  line-height: 17px;
  text-align: center;
}

.conv-product {
  margin-top: 4px;
  font-size: 11px;
  color: #1f7a5c;
  background: #f1f8f4;
  display: inline-block;
  padding: 1px 7px;
  border-radius: 4px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  margin-top: auto;
  padding: 10px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--line);
  background: #fff;
  position: sticky;
  bottom: 0;
}

.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--line);
}

.buyer {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.buyer-avatar {
  background: linear-gradient(135deg, #8fbfa9, #1f7a5c);
  color: #fff;
  font-weight: 600;
}

.buyer-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.buyer-text strong {
  font-size: 14px;
  color: var(--ink);
}

.buyer-text span {
  font-size: 11px;
  color: var(--muted);
}

.ctx-product {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border: 1px solid var(--line);
  border-radius: 999px;
  max-width: 280px;
  background: #fffdf9;
}

.ctx-product img {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
  background: #f3ece4;
}

.ctx-product span {
  font-size: 11.5px;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: #faf7f2;
}

.day-line {
  text-align: center;
}

.day-line span {
  font-size: 11px;
  color: var(--muted);
  background: rgba(0, 0, 0, 0.04);
  padding: 3px 10px;
  border-radius: 999px;
}

.msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.msg.mine {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #8fbfa9, #1f7a5c);
  color: #fff;
  font-size: 12px;
}

.bubble-wrap {
  max-width: 68%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.msg.mine .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 9px 12px;
  font-size: 13px;
  line-height: 1.62;
  border-radius: 12px;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg.theirs .bubble {
  background: #fff;
  color: var(--ink);
  border: 1px solid var(--line);
  border-top-left-radius: 4px;
}

.msg.mine .bubble {
  background: linear-gradient(135deg, #3fae86, #1f7a5c);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 6px 16px rgba(31, 122, 92, 0.22);
}

.time {
  font-size: 10.5px;
  color: var(--muted);
  padding: 0 2px;
}

.chat-foot {
  border-top: 1px solid var(--line);
  padding: 10px 18px 14px;
  background: #fff;
}

.templates {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 9px;
}

.tpl {
  border: 1px solid var(--line);
  background: #fbf8f4;
  color: var(--ink-2);
  font-size: 11.5px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.tpl:hover {
  border-color: #1f7a5c;
  color: #1f7a5c;
  background: #f1f8f4;
}

.chat-foot :deep(.el-textarea__inner) {
  font-size: 13px;
  border-radius: 10px;
  background: #fbf8f4;
  box-shadow: none;
  border: 1px solid var(--line);
}

.foot-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .layout {
    flex-direction: column;
    height: auto;
  }

  .conv-list {
    width: 100%;
    max-height: 300px;
  }

  .chat-panel {
    height: 560px;
  }
}
</style>
