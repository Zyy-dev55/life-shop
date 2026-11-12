<template>
  <div class="secops" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>网络安全运维监控</h2>
        <p class="hint">访问审计 · 接口限流 · 动态基线异常检测 · 告警处置</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll">刷新数据</el-button>
        <el-button type="primary" @click="runDetect">立即检测</el-button>
        <el-button type="warning" plain @click="runSimulate">流量回放测试</el-button>
      </div>
    </div>

    <!-- 概览指标 -->
    <div class="stat-grid">
      <div class="stat-card">
        <span class="stat-label">24 小时请求总量</span>
        <strong class="stat-value">{{ fmt(overview.totalRequests) }}</strong>
        <span class="stat-foot">访问审计日志条数</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">平均响应时延</span>
        <strong class="stat-value">{{ overview.avgCostMs || 0 }}<i>ms</i></strong>
        <span class="stat-foot">接口处理耗时均值</span>
      </div>
      <div class="stat-card" :class="{ danger: Number(overview.errorRate) > 5 }">
        <span class="stat-label">错误率</span>
        <strong class="stat-value">{{ overview.errorRate || 0 }}<i>%</i></strong>
        <span class="stat-foot">HTTP 4xx / 5xx 占比</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">活跃源 IP</span>
        <strong class="stat-value">{{ overview.activeIps || 0 }}</strong>
        <span class="stat-foot">去重客户端地址</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">拦截命中</span>
        <strong class="stat-value">{{ fmt(overview.blockedHits) }}</strong>
        <span class="stat-foot">限流 / 黑名单 / 可疑载荷</span>
      </div>
      <div class="stat-card" :class="{ danger: Number(overview.unhandledAlerts) > 0 }">
        <span class="stat-label">未处理告警</span>
        <strong class="stat-value">{{ overview.unhandledAlerts || 0 }}</strong>
        <span class="stat-foot">智能检测生成</span>
      </div>
    </div>

    <!-- 安全策略 -->
    <div class="panel policy-panel">
      <div class="card-head">
        <div>
          <h3>当前生效的安全策略</h3>
          <p class="hint">由网络安全拦截器与运维检测服务执行</p>
        </div>
        <el-tag type="success" size="small" effect="plain">运行中</el-tag>
      </div>
      <div class="policy-list">
        <div v-for="(value, key) in overview.policy || {}" :key="key" class="policy-item">
          <span class="policy-key">{{ policyName(key) }}</span>
          <span class="policy-value">{{ value }}</span>
        </div>
      </div>
    </div>

    <!-- 趋势图 -->
    <div class="chart-grid">
      <div class="panel">
        <div class="card-head">
          <div>
            <h3>流量趋势</h3>
            <p class="hint">按分钟聚合的 QPS、错误数与平均时延</p>
          </div>
        </div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="panel">
        <div class="card-head">
          <div>
            <h3>HTTP 状态码分布</h3>
            <p class="hint">反映接口可用性与被拦截流量</p>
          </div>
        </div>
        <div ref="statusRef" class="chart"></div>
        <el-empty v-if="!statusDist.length" description="暂无访问数据" :image-size="60" />
      </div>
    </div>

    <!-- 检测报告 -->
    <div class="panel detect-panel" v-if="detectReport">
      <div class="card-head">
        <div>
          <h3>动态基线检测报告</h3>
          <p class="hint">
            {{ detectReport.status === 'WARMUP' ? '基线样本不足，仅更新模型暂不告警' : '检测完成' }}
            · 窗口 {{ detectReport.validWindows }} 个
          </p>
        </div>
        <el-tag :type="Number(detectReport.score) >= 3 ? 'danger' : 'success'" effect="dark">
          综合异常评分 {{ detectReport.score }} / 阈值 {{ detectReport.threshold }}
        </el-tag>
      </div>
      <el-table :data="detectRows" size="small" border stripe>
        <el-table-column prop="name" label="检测指标" width="180" />
        <el-table-column prop="current" label="当前值" width="130" />
        <el-table-column prop="baseline" label="EWMA 基线" width="130" />
        <el-table-column prop="deviation" label="标准化偏离" width="140">
          <template #default="{ row }">
            <span :class="{ 'text-danger': Number(row.deviation) >= 2 }">{{ row.deviation }}σ</span>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="评分权重" />
      </el-table>
      <div class="triggered" v-if="detectReport.triggered && detectReport.triggered.length">
        <el-tag v-for="t in detectReport.triggered" :key="t" type="danger" effect="plain" class="mr">
          {{ alertTypeName(t) }}
        </el-tag>
        <span class="hint">已生成告警，见下方告警列表</span>
      </div>
    </div>

    <!-- TOP 列表 -->
    <div class="table-grid">
      <div class="panel">
        <div class="card-head">
          <h3>源 IP 请求 TOP 10</h3>
        </div>
        <el-table :data="topIps" size="small" border stripe max-height="280">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="ip" label="客户端 IP" />
          <el-table-column prop="count" label="请求数" width="90" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="!row.blocked" link type="danger" size="small" @click="blockIp(row.ip)">封禁</el-button>
              <el-tag v-else type="info" size="small">已封禁</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!topIps.length" description="暂无访问数据" :image-size="60" />
      </div>
      <div class="panel">
        <div class="card-head">
          <h3>慢接口 TOP 10</h3>
        </div>
        <el-table :data="slowApis" size="small" border stripe max-height="280">
          <el-table-column prop="uri" label="接口路径" show-overflow-tooltip />
          <el-table-column prop="count" label="次数" width="70" />
          <el-table-column prop="avgCostMs" label="均耗时" width="90">
            <template #default="{ row }">{{ row.avgCostMs }} ms</template>
          </el-table-column>
          <el-table-column prop="maxCostMs" label="峰值" width="80">
            <template #default="{ row }">{{ row.maxCostMs }} ms</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!slowApis.length" description="暂无访问数据" :image-size="60" />
      </div>
    </div>

    <!-- 告警列表 -->
    <div class="panel">
      <div class="card-head">
        <div>
          <h3>智能异常告警</h3>
          <p class="hint">EWMA 动态基线 + 标准化偏离，综合评分达到阈值自动生成</p>
        </div>
        <el-button size="small" @click="loadAlerts">刷新</el-button>
      </div>
      <el-table :data="alerts" size="small" border stripe max-height="320">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small" effect="plain">{{ alertTypeName(row.alertType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="metricName" label="指标" width="140" />
        <el-table-column prop="metricValue" label="当前值" width="100" />
        <el-table-column prop="baselineValue" label="基线" width="100" />
        <el-table-column prop="score" label="评分" width="90" />
        <el-table-column prop="detail" label="说明" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-button v-if="row.handled === 0" link type="primary" size="small" @click="handleAlert(row.id)">标记处理</el-button>
            <el-tag v-else type="info" size="small">已处理</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!alerts.length" description="暂无告警，可点击「流量回放测试」复现异常场景" :image-size="70" />
    </div>

    <!-- 黑名单 -->
    <div class="panel">
      <div class="card-head">
        <div>
          <h3>IP 访问黑名单</h3>
          <p class="hint">支持手工封禁与登录防爆破自动封禁，到期自动失效</p>
        </div>
      </div>
      <div class="block-form">
        <el-input v-model="newIp" placeholder="输入 IP，如 203.0.113.77" style="width: 220px" clearable />
        <el-input v-model="newReason" placeholder="封禁原因" style="width: 240px" clearable />
        <el-input-number v-model="newMinutes" :min="1" :max="10080" placeholder="分钟" style="width: 150px" />
        <span class="hint">封禁时长（分钟）</span>
        <el-button type="danger" @click="addBlacklist">添加封禁</el-button>
      </div>
      <el-table :data="blacklist" size="small" border stripe max-height="260">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="ip" label="IP 地址" width="180" />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.source === 'AUTO' ? 'warning' : 'info'" size="small" effect="plain">
              {{ row.source === 'AUTO' ? '自动封禁' : '人工封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="expireTime" label="到期时间" width="160" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="removeBlacklist(row.id)">解封</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!blacklist.length" description="黑名单为空" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const trendRef = ref(null)
const statusRef = ref(null)
let trendChart = null
let statusChart = null

const overview = reactive({})
const trend = ref([])
const statusDist = ref([])
const topIps = ref([])
const slowApis = ref([])
const alerts = ref([])
const blacklist = ref([])
const detectReport = ref(null)

const newIp = ref('')
const newReason = ref('')
const newMinutes = ref(60)

const fmt = (v) => Number(v || 0).toLocaleString('zh-CN')

const policyName = (key) =>
  ({
    globalLimit: '全局限流',
    loginLimit: '登录防爆破',
    payloadFilter: '载荷过滤',
    detectWindow: '检测窗口',
    algorithm: '检测算法'
  }[key] || key)

const alertTypeName = (type) =>
  ({
    QPS_SPIKE: '流量突增',
    ERROR_RATE: '错误率异常',
    SLOW_RESPONSE: '响应变慢',
    IP_BURST: '单 IP 集中访问'
  }[type] || type)

const levelType = (level) => (level === 'CRITICAL' ? 'danger' : level === 'WARN' ? 'warning' : 'info')

const detectRows = computed(() => {
  const r = detectReport.value
  if (!r || !r.current) return []
  const names = {
    qps: 'QPS（每秒请求数）',
    errorRate: '错误率',
    avgCost: '平均响应时延(ms)',
    ipConcentration: '单 IP 请求集中度'
  }
  const weights = { qps: '35%', errorRate: '30%', avgCost: '20%', ipConcentration: '15%' }
  return Object.keys(names).map((key) => ({
    name: names[key],
    current: r.current[key] ?? '—',
    baseline: r.baseline?.[key] ?? '—',
    deviation: r.deviation?.[key] ?? '—',
    weight: weights[key]
  }))
})

const renderTrend = () => {
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  const rows = trend.value
  trendChart.setOption({
    grid: { left: 8, right: 16, top: 36, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { data: ['QPS', '错误数', '平均时延(ms)'], top: 0, textStyle: { fontSize: 11 } },
    xAxis: {
      type: 'category',
      data: rows.map((r) => r.time),
      axisLine: { lineStyle: { color: '#ece4da' } },
      axisTick: { show: false },
      axisLabel: { color: '#948b81', fontSize: 10, interval: Math.max(0, Math.floor(rows.length / 8) - 1) }
    },
    yAxis: [
      { type: 'value', name: 'QPS / 次数', splitLine: { lineStyle: { color: '#f4ede5', type: 'dashed' } }, axisLabel: { color: '#948b81', fontSize: 10 } },
      { type: 'value', name: 'ms', splitLine: { show: false }, axisLabel: { color: '#948b81', fontSize: 10 } }
    ],
    series: [
      {
        name: 'QPS', type: 'line', smooth: true, symbol: 'none', data: rows.map((r) => r.qps),
        lineStyle: { width: 2, color: '#e8612d' }, itemStyle: { color: '#e8612d' },
        areaStyle: { color: 'rgba(232,97,45,0.10)' }
      },
      { name: '错误数', type: 'bar', data: rows.map((r) => r.errors), itemStyle: { color: '#e4a23c' }, barMaxWidth: 12 },
      { name: '平均时延(ms)', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'none', data: rows.map((r) => r.avgCostMs), lineStyle: { width: 2, color: '#4b8bb8', type: 'dashed' }, itemStyle: { color: '#4b8bb8' } }
    ]
  }, true)
}

const renderStatus = () => {
  if (!statusRef.value) return
  if (!statusChart) statusChart = echarts.init(statusRef.value)
  const rows = statusDist.value
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 次 ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
        data: rows.map((r) => ({ name: r.status, value: r.count })),
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', fontSize: 11 }
      }
    ],
    color: ['#54a06a', '#e8612d', '#e4a23c', '#d95050', '#4b8bb8', '#948b81']
  }, true)
}

const loadAlerts = async () => {
  alerts.value = (await adminApi.secOpsAlerts(50)) || []
}

const loadAll = async () => {
  loading.value = true
  try {
    const [ov, tr, sd, ti, sa, al, bl] = await Promise.all([
      adminApi.secOpsOverview(),
      adminApi.secOpsTrend(30),
      adminApi.secOpsStatusDist(),
      adminApi.secOpsTopIps(10),
      adminApi.secOpsSlowApis(10),
      adminApi.secOpsAlerts(50),
      adminApi.secOpsBlacklist()
    ])
    Object.assign(overview, ov || {})
    trend.value = tr || []
    statusDist.value = sd || []
    topIps.value = ti || []
    slowApis.value = sa || []
    alerts.value = al || []
    blacklist.value = bl || []
    await nextTick()
    renderTrend()
    renderStatus()
  } finally {
    loading.value = false
  }
}

const runDetect = async () => {
  detectReport.value = await adminApi.secOpsDetect()
  ElMessage.success(
    detectReport.value.status === 'WARMUP'
      ? '基线样本不足，已更新模型，可用「流量回放测试」补充样本'
      : `检测完成，综合评分 ${detectReport.value.score}`
  )
  await loadAll()
  await nextTick()
  renderTrend()
  renderStatus()
}

const runSimulate = async () => {
  loading.value = true
  try {
    const res = await adminApi.secOpsSimulate()
    detectReport.value = res.detect
    ElMessage.success(
      `已回放 ${res.normalLogs} 条正常流量与 ${res.burstLogs} 条突发流量，命中 ${(res.detect.triggered || []).length} 类告警`
    )
    await loadAll()
    await nextTick()
    renderTrend()
    renderStatus()
  } finally {
    loading.value = false
  }
}

const handleAlert = async (id) => {
  await adminApi.secOpsHandleAlert(id)
  ElMessage.success('已标记处理')
  await loadAll()
}

const blockIp = async (ip) => {
  await adminApi.secOpsAddBlacklist({ ip, reason: '监控看板手工封禁', minutes: 60 })
  ElMessage.success(`${ip} 已封禁 60 分钟`)
  await loadAll()
}

const addBlacklist = async () => {
  if (!newIp.value.trim()) {
    ElMessage.warning('请填写要封禁的 IP 地址')
    return
  }
  await adminApi.secOpsAddBlacklist({
    ip: newIp.value.trim(),
    reason: newReason.value.trim() || '管理员手工封禁',
    minutes: newMinutes.value
  })
  newIp.value = ''
  newReason.value = ''
  ElMessage.success('已加入黑名单')
  await loadAll()
}

const removeBlacklist = async (id) => {
  await adminApi.secOpsRemoveBlacklist(id)
  ElMessage.success('已解封')
  await loadAll()
}

const onResize = () => {
  trendChart && trendChart.resize()
  statusChart && statusChart.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart && trendChart.dispose()
  statusChart && statusChart.dispose()
})
</script>

<style scoped>
.secops { padding: 4px 2px 30px; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 18px; gap: 16px; flex-wrap: wrap; }
.page-head h2 { margin: 0 0 6px; font-size: 20px; color: #2f2a25; }
.hint { margin: 0; font-size: 12px; color: #948b81; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.stat-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card { background: #fff; border: 1px solid #f0e8de; border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 6px; }
.stat-card.danger { border-color: #f3c4b4; background: #fff9f6; }
.stat-label { font-size: 12px; color: #948b81; }
.stat-value { font-size: 24px; color: #2f2a25; line-height: 1.2; }
.stat-value i { font-size: 12px; font-style: normal; color: #948b81; margin-left: 3px; }
.stat-foot { font-size: 11px; color: #b4aaa0; }
.panel { background: #fff; border: 1px solid #f0e8de; border-radius: 10px; padding: 16px; margin-bottom: 16px; }
.card-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 14px; gap: 12px; }
.card-head h3 { margin: 0 0 4px; font-size: 15px; color: #2f2a25; }
.policy-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 10px; }
.policy-item { display: flex; gap: 10px; align-items: baseline; background: #faf7f3; border-radius: 6px; padding: 9px 12px; font-size: 12px; }
.policy-key { color: #e8612d; font-weight: 600; white-space: nowrap; }
.policy-value { color: #5c534b; }
.chart-grid, .table-grid { display: grid; grid-template-columns: 1.6fr 1fr; gap: 16px; }
.chart-grid .panel, .table-grid .panel { margin-bottom: 16px; }
.chart { height: 260px; width: 100%; }
.detect-panel .triggered { margin-top: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.mr { margin-right: 4px; }
.text-danger { color: #d95050; font-weight: 600; }
.block-form { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 14px; }
@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .chart-grid, .table-grid { grid-template-columns: 1fr; }
}
</style>
