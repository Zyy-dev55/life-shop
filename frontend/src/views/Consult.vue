<template>
  <div class="consult-page container">
    <div class="page-head">
      <h2>我的咨询</h2>
      <span class="sub">商品尺码、库存、发货时间都可以直接问店铺客服，商家回复会在这里提醒你</span>
    </div>

    <div class="chat-layout" v-loading="listLoading">
      <!-- 会话列表 -->
      <aside class="conv-list">
        <div class="conv-head">
          全部会话
          <span class="count">{{ conversations.length }}</span>
        </div>

        <div v-if="!conversations.length && !listLoading" class="conv-empty">
          <el-icon :size="26"><ChatDotRound /></el-icon>
          <p>还没有咨询过店铺</p>
          <router-link to="/list" class="go">去逛逛商品</router-link>
        </div>

        <div
          v-for="c in conversations"
          :key="c.id"
          :class="['conv-item', { active: c.id === activeId }]"
          @click="select(c)"
        >
          <el-avatar :size="38" :src="c.shopLogo || ''" class="conv-avatar">
            {{ (c.shopName || '店').slice(0, 1) }}
          </el-avatar>
          <div class="conv-main">
            <div class="conv-top">
              <strong>{{ c.shopName }}</strong>
              <span class="conv-time">{{ shortTime(c.lastTime) }}</span>
            </div>
            <div class="conv-bottom">
              <p>{{ c.lastContent || '开始咨询吧' }}</p>
              <span v-if="c.userUnread" class="unread">{{ c.userUnread }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 聊天区 -->
      <section class="chat-panel">
        <template v-if="active">
          <header class="chat-head">
            <div class="shop">
              <el-avatar :size="34" :src="active.shopLogo || ''" class="shop-avatar">
                {{ (active.shopName || '店').slice(0, 1) }}
              </el-avatar>
              <div class="shop-text">
                <strong>{{ active.shopName }}</strong>
                <span><i class="dot"></i> 客服在线时间 9:00 - 21:00</span>
              </div>
            </div>
            <router-link
              v-if="active.productId"
              :to="`/product/${active.productId}`"
              class="ctx-product"
            >
              <img :src="active.productImage" @error="onImgError" />
              <span>{{ active.productName }}</span>
            </router-link>
          </header>

          <div ref="scrollRef" class="chat-body" v-loading="msgLoading">
            <div class="day-line"><span>咨询记录仅本店铺客服可见</span></div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['msg', msg.senderType === 'USER' ? 'mine' : 'theirs']"
            >
              <el-avatar v-if="msg.senderType !== 'USER'" :size="30" :src="active.shopLogo || ''" class="msg-avatar">
                {{ (active.shopName || '店').slice(0, 1) }}
              </el-avatar>
              <div class="bubble-wrap">
                <div class="bubble">{{ msg.content }}</div>
                <span class="time">{{ shortTime(msg.createTime) }}</span>
              </div>
            </div>
            <div v-if="messages.length && messages[messages.length - 1].senderType === 'USER'" class="waiting">
              已送达，等待商家回复…
            </div>
          </div>

          <footer class="chat-foot">
            <el-input
              v-model="input"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="继续咨询，Enter 发送 / Shift+Enter 换行"
              @keydown.enter.exact.prevent="send"
            />
            <div class="foot-bar">
              <span class="hint">商家回复后页面会自动刷新</span>
              <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="send">
                发送
              </el-button>
            </div>
          </footer>
        </template>

        <div v-else class="chat-empty">
          <el-icon :size="40"><ChatLineSquare /></el-icon>
          <p>选择左侧的一个店铺，查看咨询记录</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { consultApi } from '@/api'
import { onImgError } from '@/utils/format'

const route = useRoute()

const conversations = ref([])
const activeId = ref(null)
const messages = ref([])
const input = ref('')
const listLoading = ref(false)
const msgLoading = ref(false)
const sending = ref(false)
const scrollRef = ref(null)
let timer = null

const active = computed(() => conversations.value.find((c) => c.id === activeId.value) || null)

const shortTime = (value) => {
  if (!value) return ''
  const d = new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(value).slice(5, 16)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (sameDay) return `${hh}:${mm}`
  const diffDay = Math.floor((now - d) / 86400000)
  if (diffDay === 1) return '昨天'
  return `${d.getMonth() + 1}-${d.getDate()}`
}

const scrollBottom = async () => {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const loadList = async (keepActive = true) => {
  listLoading.value = !conversations.value.length
  try {
    conversations.value = await consultApi.list()
    if (!keepActive || !conversations.value.some((c) => c.id === activeId.value)) {
      if (conversations.value.length) {
        select(conversations.value[0])
      } else {
        activeId.value = null
        messages.value = []
      }
    }
  } finally {
    listLoading.value = false
  }
}

const select = async (conv) => {
  activeId.value = conv.id
  msgLoading.value = true
  try {
    const data = await consultApi.detail(conv.id)
    messages.value = data.messages || []
    // 打开即已读，同步列表里的未读角标
    const target = conversations.value.find((c) => c.id === conv.id)
    if (target) target.userUnread = 0
    await scrollBottom()
  } finally {
    msgLoading.value = false
  }
}

const send = async () => {
  const text = input.value.trim()
  if (!text || !active.value) return
  sending.value = true
  try {
    const data = await consultApi.send({
      merchantId: active.value.merchantId,
      productId: active.value.productId || null,
      content: text
    })
    messages.value = data.messages || []
    input.value = ''
    const idx = conversations.value.findIndex((c) => c.id === active.value.id)
    if (idx > -1 && data.consult) {
      conversations.value[idx] = { ...conversations.value[idx], ...data.consult }
    }
    await scrollBottom()
  } finally {
    sending.value = false
  }
}

const startPolling = () => {
  stopPolling()
  timer = setInterval(async () => {
    if (!activeId.value || sending.value) return
    try {
      const data = await consultApi.detail(activeId.value)
      const before = messages.value.length
      messages.value = data.messages || []
      if (data.consult) {
        const idx = conversations.value.findIndex((c) => c.id === activeId.value)
        if (idx > -1) conversations.value[idx] = { ...conversations.value[idx], ...data.consult, userUnread: 0 }
      }
      if (messages.value.length !== before) await scrollBottom()
    } catch (e) {
      /* 轮询错误忽略 */
    }
  }, 6000)
}

const stopPolling = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  await loadList(false)
  // 支持从消息提醒带会话 id 直达
  const wanted = Number(route.query.id)
  if (wanted) {
    const conv = conversations.value.find((c) => c.id === wanted)
    if (conv) await select(conv)
  }
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.consult-page {
  padding: 28px 0 56px;
}

.page-head {
  margin-bottom: 18px;
}

.page-head h2 {
  font-size: 22px;
  font-weight: 600;
  color: var(--ink);
}

.page-head .sub {
  display: block;
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--muted);
}

.chat-layout {
  display: flex;
  height: 620px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.conv-list {
  width: 288px;
  flex-shrink: 0;
  border-right: 1px solid var(--line);
  background: #fffdfa;
  overflow-y: auto;
}

.conv-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  letter-spacing: 0.5px;
}

.conv-head .count {
  background: var(--primary-soft);
  color: var(--primary);
  border-radius: 999px;
  padding: 0 7px;
  font-size: 11px;
  line-height: 17px;
}

.conv-empty {
  padding: 48px 20px;
  text-align: center;
  color: var(--muted);
  font-size: 12.5px;
}

.conv-empty p {
  margin: 10px 0 12px;
}

.conv-empty .go {
  color: var(--primary);
  font-weight: 600;
}

.conv-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background 0.18s;
}

.conv-item:hover {
  background: #fbf6ef;
}

.conv-item.active {
  background: var(--primary-soft);
  border-left-color: var(--primary);
}

.conv-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #f08a5d, var(--primary));
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
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread {
  flex-shrink: 0;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  font-size: 10.5px;
  line-height: 17px;
  text-align: center;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg);
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 18px;
  background: #fff;
  border-bottom: 1px solid var(--line);
}

.shop {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.shop-avatar {
  background: linear-gradient(135deg, #f08a5d, var(--primary));
  color: #fff;
  font-weight: 600;
}

.shop-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;
}

.shop-text strong {
  font-size: 14px;
  color: var(--ink);
}

.shop-text span {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--muted);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3fae86;
}

.ctx-product {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border: 1px solid var(--line);
  border-radius: 999px;
  max-width: 260px;
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
  background: linear-gradient(135deg, #f08a5d, var(--primary));
  color: #fff;
  font-size: 12px;
}

.bubble-wrap {
  max-width: 66%;
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
  background: linear-gradient(135deg, #f0764a, var(--primary));
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 6px 16px rgba(232, 97, 45, 0.22);
}

.time {
  font-size: 10.5px;
  color: var(--muted);
  padding: 0 2px;
}

.waiting {
  text-align: center;
  font-size: 11.5px;
  color: var(--muted);
}

.chat-foot {
  padding: 12px 18px 14px;
  background: #fff;
  border-top: 1px solid var(--line);
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

.hint {
  font-size: 11px;
  color: var(--muted);
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

@media (max-width: 860px) {
  .chat-layout {
    height: auto;
    flex-direction: column;
  }

  .conv-list {
    width: 100%;
    max-height: 240px;
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .chat-panel {
    height: 520px;
  }
}
</style>
