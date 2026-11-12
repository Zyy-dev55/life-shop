<template>
  <el-drawer
    v-model="visible"
    :size="drawerSize"
    :with-header="false"
    class="consult-drawer"
    append-to-body
    @closed="onClosed"
  >
    <div class="chat">
      <header class="chat-head">
        <div class="shop">
          <el-avatar :size="38" :src="shopLogo || ''" class="shop-avatar">
            {{ (shopName || '店').slice(0, 1) }}
          </el-avatar>
          <div class="shop-text">
            <strong>{{ shopName || consult.shopName || '在线客服' }}</strong>
            <span class="status"><i class="dot"></i> 商家客服 · 通常 5 分钟内回复</span>
          </div>
        </div>
        <el-button text class="close" @click="visible = false">
          <el-icon :size="18"><Close /></el-icon>
        </el-button>
      </header>

      <div v-if="consult.productName" class="ctx-product">
        <img :src="consult.productImage" @error="onImgError" />
        <div class="ctx-info">
          <p class="ctx-name">{{ consult.productName }}</p>
          <span class="ctx-tip">正在咨询这件商品</span>
        </div>
      </div>

      <div ref="scrollRef" class="chat-body" v-loading="loading">
        <div class="day-line"><span>咨询记录仅本店铺客服可见</span></div>

        <template v-if="messages.length">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['msg', msg.senderType === 'USER' ? 'mine' : 'theirs']"
          >
            <el-avatar v-if="msg.senderType !== 'USER'" :size="30" :src="shopLogo || ''" class="msg-avatar">
              {{ (shopName || '店').slice(0, 1) }}
            </el-avatar>
            <div class="bubble-wrap">
              <div class="bubble">{{ msg.content }}</div>
              <span class="time">{{ shortTime(msg.createTime) }}</span>
            </div>
          </div>
        </template>
        <div v-else class="welcome">
          <p class="hi">你好呀，我是 {{ shopName || '店铺' }} 的客服 👋</p>
          <p class="sub">关于商品尺码、库存、发货时间都可以直接问我，看到就会回。</p>
          <div class="quick">
            <button v-for="q in quickAsks" :key="q" class="quick-item" @click="send(q)">{{ q }}</button>
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
          placeholder="输入想咨询的问题，Enter 发送 / Shift+Enter 换行"
          @keydown.enter.exact.prevent="send()"
        />
        <div class="foot-bar">
          <span class="hint">工作日 9:00-21:00 在线</span>
          <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="send()">
            发送
          </el-button>
        </div>
      </footer>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { consultApi } from '@/api'
import { onImgError } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  merchantId: { type: [Number, String], default: null },
  productId: { type: [Number, String], default: null },
  shopName: { type: String, default: '' },
  shopLogo: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'sent'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const drawerSize = computed(() => (window.innerWidth < 640 ? '100%' : '440px'))

const quickAsks = ['这款有现货吗？', '大概什么时候发货？', '支持 7 天无理由退换吗？', '可以开发票吗？']

const consult = ref({})
const messages = ref([])
const input = ref('')
const loading = ref(false)
const sending = ref(false)
const scrollRef = ref(null)
let timer = null

const scrollBottom = async () => {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const shortTime = (value) => {
  if (!value) return ''
  const d = new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(value).slice(5, 16)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return sameDay ? `${hh}:${mm}` : `${d.getMonth() + 1}-${d.getDate()} ${hh}:${mm}`
}

const load = async () => {
  if (!props.merchantId) return
  loading.value = true
  try {
    const data = await consultApi.with(props.merchantId, props.productId || undefined)
    consult.value = data.consult || {}
    messages.value = data.messages || []
    await scrollBottom()
  } catch (e) {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

/** 轮询新消息：打开着就每 5 秒拉一次 */
const startPolling = () => {
  stopPolling()
  timer = setInterval(async () => {
    if (!consult.value.id || sending.value) return
    try {
      const data = await consultApi.detail(consult.value.id)
      const before = messages.value.length
      consult.value = data.consult || consult.value
      messages.value = data.messages || []
      if (messages.value.length !== before) await scrollBottom()
    } catch (e) {
      /* 忽略轮询中的偶发错误 */
    }
  }, 5000)
}

const stopPolling = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const send = async (preset) => {
  const text = (preset ?? input.value).trim()
  if (!text) return
  if (!props.merchantId) {
    ElMessage.warning('未获取到店铺信息')
    return
  }
  sending.value = true
  try {
    const data = await consultApi.send({
      merchantId: props.merchantId,
      productId: props.productId || null,
      content: text
    })
    consult.value = data.consult || consult.value
    messages.value = data.messages || []
    input.value = ''
    await scrollBottom()
    emit('sent', consult.value)
  } finally {
    sending.value = false
  }
}

const onClosed = () => {
  stopPolling()
  messages.value = []
  consult.value = {}
  input.value = ''
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      load()
      startPolling()
    } else {
      stopPolling()
    }
  }
)

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid var(--line);
}

.shop {
  display: flex;
  align-items: center;
  gap: 10px;
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
  font-size: 14.5px;
  color: var(--ink);
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  font-size: 11.5px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 5px;
}

.status .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3fae86;
  display: inline-block;
}

.close {
  color: var(--muted);
}

.ctx-product {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 9px 16px;
  background: #fffdf9;
  border-bottom: 1px solid var(--line);
}

.ctx-product img {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background: #f3ece4;
}

.ctx-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ctx-name {
  font-size: 12.5px;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ctx-tip {
  font-size: 11px;
  color: var(--muted);
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
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
  max-width: 78%;
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

.welcome {
  padding: 8px 4px;
}

.welcome .hi {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 6px;
}

.welcome .sub {
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 1.7;
  margin-bottom: 14px;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-item {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  font-size: 12.5px;
  padding: 6px 11px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.quick-item:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}

.waiting {
  text-align: center;
  font-size: 11.5px;
  color: var(--muted);
}

.chat-foot {
  padding: 12px 16px 14px;
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
</style>

<style>
/* 抽屉自带的内边距会打乱聊天布局，这里去掉（append-to-body 后 scoped 选不中） */
.consult-drawer .el-drawer__body {
  padding: 0;
  overflow: hidden;
}
</style>
