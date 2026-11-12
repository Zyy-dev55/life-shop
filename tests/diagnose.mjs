/**
 * 分层排障探测器（零第三方依赖，Node 18+ 可直接运行）。
 *
 * 用法：
 *   node tests/diagnose.mjs [http://127.0.0.1:8080] [--probe]
 *
 * 与 tests/regression.mjs 的区别：
 *   - regression.mjs 是「判定对错」的回归测试，断言失败即构建失败；
 *   - diagnose.mjs 是「定位故障层」的排查工具，逐层体检并给出第一个失败层、
 *     该层的检查点、可复制的验证命令与文档链接。
 *
 * --probe 会主动触发登录接口的暴力破解防护（连续 9 次失败登录），
 * 用来复现 429 + 自动封禁链路。使用独立的 X-Forwarded-For 源 IP，
 * 不会把本机 127.0.0.1 封掉；封禁 10 分钟后自动失效。
 * 配套说明见 docs/TROUBLESHOOTING.md 的「故障复现示例」。
 */

import net from 'node:net'

const args = process.argv.slice(2)
const BASE = (args.find((a) => !a.startsWith('--')) || process.env.LIFE_SHOP_BASE || 'http://127.0.0.1:8080').replace(/\/$/, '')
const PROBE = args.includes('--probe')
const URL_OBJ = new URL(BASE)
const RUN_IP = '203.0.113.' + (Math.floor(Math.random() * 200) + 1)
const PROBE_IP = '198.51.100.' + (Math.floor(Math.random() * 200) + 1)

const results = []
let firstFailedLayer = null

const ok = (detail) => ({ ok: true, detail })
const bad = (detail, hint) => ({ ok: false, detail, hint })
const warn = (detail, hint) => ({ ok: 'warn', detail, hint })

async function call(method, path, { token, body, ip = RUN_IP, headers = {} } = {}) {
  try {
    const res = await fetch(BASE + path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'X-Forwarded-For': ip,
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
        ...headers
      },
      body: body === undefined ? undefined : JSON.stringify(body)
    })
    const text = await res.text()
    let json = null
    try { json = JSON.parse(text) } catch { /* HTML 或静态资源 */ }
    return { status: res.status, headers: res.headers, text, json, error: null }
  } catch (e) {
    return { status: 0, headers: new Map(), text: '', json: null, error: e.message }
  }
}

function tcpReachable(port, host, timeout = 2000) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ port, host })
    const done = (value) => { socket.destroy(); resolve(value) }
    socket.setTimeout(timeout)
    socket.once('connect', () => done(true))
    socket.once('timeout', () => done(false))
    socket.once('error', () => done(false))
  })
}

async function layer(id, name, docAnchor, checks) {
  console.log(`\n[${id}] ${name}`)
  const layerResult = { id, name, docAnchor, passed: 0, failed: 0, warned: 0 }
  for (const [label, fn] of checks) {
    let r
    try {
      r = await fn()
    } catch (e) {
      r = bad(`检查过程抛出异常：${e.message}`, '查看后端控制台堆栈')
    }
    if (r.ok === true) {
      layerResult.passed++
      console.log(`  ✓ ${label}${r.detail ? ' — ' + r.detail : ''}`)
    } else if (r.ok === 'warn') {
      layerResult.warned++
      console.log(`  ! ${label} — ${r.detail}`)
      if (r.hint) console.log(`      → ${r.hint}`)
    } else {
      layerResult.failed++
      if (!firstFailedLayer) firstFailedLayer = layerResult
      console.log(`  ✗ ${label} — ${r.detail}`)
      if (r.hint) console.log(`      → ${r.hint}`)
    }
  }
  results.push(layerResult)
}

const state = { buyerToken: null, adminToken: null, refs: [] }

// ---------------------------------------------------------------- L0
await layer('L0', '运行环境 / 主机连通性', '#l0-运行环境', [
  ['Node 版本 >= 18', () => {
    const major = Number(process.versions.node.split('.')[0])
    return major >= 18 ? ok(`Node ${process.versions.node}`) : bad(`Node ${process.versions.node}`, '升级 Node 到 18+')
  }],
  ['目标地址可解析', () => ok(`${URL_OBJ.protocol}//${URL_OBJ.host}`)],
  ['TCP 端口可达', async () => {
    const port = Number(URL_OBJ.port || (URL_OBJ.protocol === 'https:' ? 443 : 80))
    const alive = await tcpReachable(port, URL_OBJ.hostname)
    return alive
      ? ok(`${URL_OBJ.hostname}:${port} 已监听`)
      : bad(`${URL_OBJ.hostname}:${port} 连不上`, '先启动后端；Windows 用 netstat -ano | findstr :8080 查端口占用')
  }]
])

// ---------------------------------------------------------------- L1
await layer('L1', '网络 / HTTP 协议层', '#l1-网络协议层', [
  ['健康检查返回 200 + JSON', async () => {
    const r = await call('GET', '/api/auth/health')
    return r.status === 200 && r.json?.code === 200
      ? ok(`HTTP ${r.status}`)
      : bad(`HTTP ${r.status}, body=${r.text.slice(0, 120)}`,
        'curl -i http://127.0.0.1:8080/api/auth/health；404 检查 Controller 扫描包，500 查启动日志')
  }],
  ['Content-Type 为 application/json', async () => {
    const r = await call('GET', '/api/auth/health')
    return /application\/json/.test(r.headers.get('content-type') || '')
      ? ok('application/json')
      : bad(r.headers.get('content-type') || '(空)', '检查是否被网关/静态资源回退改写')
  }],
  ['HTTP 方法语义正确', async () => {
    const r = await call('POST', '/api/auth/health', { body: {} })
    return r.status >= 400 && r.status < 500 ? ok(`POST → ${r.status}`) : bad(`POST → ${r.status}`, '检查 @GetMapping 是否被误改为 @RequestMapping')
  }],
  ['CORS 预检不被拦截', async () => {
    const r = await call('OPTIONS', '/api/user/info', {
      headers: { 'Access-Control-Request-Method': 'GET', Origin: BASE }
    })
    return r.status < 400 ? ok(`OPTIONS → ${r.status}`) : bad(`OPTIONS → ${r.status}`, '检查 WebMvcConfig.addCorsMappings 与拦截器 OPTIONS 放行分支')
  }]
])

// ---------------------------------------------------------------- L2
await layer('L2', '安全拦截层（黑名单 / 限流 / 载荷过滤）', '#l2-安全拦截层', [
  ['安全响应头齐全', async () => {
    const r = await call('GET', '/api/auth/health')
    const required = ['x-content-type-options', 'x-frame-options', 'referrer-policy', 'content-security-policy-report-only']
    const missing = required.filter((h) => !r.headers.get(h))
    return missing.length === 0 ? ok('4 项齐全') : bad(`缺少 ${missing.join(', ')}`, 'SecurityInterceptor.setSecurityHeaders 未生效，检查是否被注册到 /api/**')
  }],
  ['XSS 载荷被 403 拦截', async () => {
    const r = await call('GET', '/api/product/page?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E')
    return r.status === 403 ? ok('HTTP 403') : bad(`HTTP ${r.status}`, 'curl -i "http://127.0.0.1:8080/api/product/page?q=<script>"; 检查 SUSPICIOUS_PATTERNS 与拦截器链顺序')
  }],
  ['SQL 注入载荷被 403 拦截', async () => {
    const r = await call('GET', "/api/product/page?q=1' or 1=1--")
    return r.status === 403 ? ok('HTTP 403') : bad(`HTTP ${r.status}`, '确认 query string 原始串被传入 hasSuspiciousPayload')
  }],
  ['正常中文关键词不误拦', async () => {
    const r = await call('GET', '/api/product/page?keyword=' + encodeURIComponent('保温杯'))
    return r.status === 200 ? ok('HTTP 200') : bad(`HTTP ${r.status}`, '命中了误报规则，缩小正则或加白名单')
  }]
])

if (PROBE) {
  await layer('L2-P', '故障注入：登录防爆破链路', '#l2-安全拦截层', [
    ['连续失败登录触发 429 并自动封禁', async () => {
      let last = null
      for (let i = 0; i < 9; i++) {
        last = await call('POST', '/api/auth/login',
          { body: { username: 'nobody', password: 'wrong' }, ip: PROBE_IP })
      }
      return last.status === 429
        ? ok(`第 9 次返回 429，源 IP ${PROBE_IP} 已被封禁 10 分钟`)
        : bad(`第 9 次返回 ${last.status}`, '检查 lifeshop.security.login-limit（默认 8）与 autoBlock 调用')
    }],
    ['被封禁 IP 后续请求直接 403', async () => {
      const r = await call('GET', '/api/auth/health', { ip: PROBE_IP })
      return r.status === 403 ? ok('HTTP 403') : bad(`HTTP ${r.status}`, '检查 SecOpsService.isBlocked 与 blockCache 是否同步')
    }]
  ])
  console.log(`  （演练 IP ${PROBE_IP} 将在 10 分钟后自动解封，也可在运维看板手工解除）`)
}

// ---------------------------------------------------------------- L3
await layer('L3', '身份鉴权与权限边界', '#l3-身份鉴权层', [
  ['无令牌访问受保护接口被拒', async () => {
    const r = await call('GET', '/api/user/info')
    return r.json?.code === 401 ? ok('业务码 401') : bad(`code=${r.json?.code}`, '检查 JwtInterceptor 是否注册、parse() 是否返回 null')
  }],
  ['买家登录成功', async () => {
    const r = await call('POST', '/api/auth/login', { body: { username: 'zhangsan', password: '123456' } })
    state.buyerToken = r.json?.data?.token || null
    return state.buyerToken ? ok('role=USER') : bad(r.json?.message || r.text.slice(0, 80), '检查 t_user 种子数据与 PasswordUtil 盐值')
  }],
  ['越权访问管理员接口被拒', async () => {
    const r = await call('GET', '/api/admin/secops/overview', { token: state.buyerToken })
    return r.json?.code === 403 ? ok('业务码 403') : bad(`code=${r.json?.code}`, '检查 AdminInterceptor 与 UserContext.requireAdmin')
  }],
  ['管理员登录与运维看板可用', async () => {
    const login = await call('POST', '/api/auth/login', { body: { username: 'admin', password: 'admin123' } })
    state.adminToken = login.json?.data?.token || null
    if (!state.adminToken) return bad(login.json?.message || '登录失败', '确认 admin 账号未被禁用')
    const r = await call('GET', '/api/admin/secops/overview', { token: state.adminToken })
    return r.json?.code === 200 ? ok('看板可读') : bad(`code=${r.json?.code}`, '检查 AdminSecOpsController 与 secops 相关表')
  }]
])

// ---------------------------------------------------------------- L4
await layer('L4', '业务服务层', '#l4-业务服务层', [
  ['商品列表可读', async () => {
    const r = await call('GET', '/api/product/page?pageNum=1&pageSize=6')
    return r.json?.code === 200 && r.json.data.list.length === 6 ? ok('6 条') : bad(r.json?.message || '结构异常', '检查 t_product 与 ProductService')
  }],
  ['订单分页结构完整', async () => {
    const r = await call('GET', '/api/order/page?pageNum=1&pageSize=5', { token: state.buyerToken })
    return r.json?.data && Array.isArray(r.json.data.list) ? ok('list/total 齐全') : bad('分页结构异常', '检查 PageResult.of 与 MybatisPlus 分页插件')
  }],
  ['售后列表文案非空', async () => {
    const r = await call('GET', '/api/after-sale/my?pageNum=1&pageSize=50', { token: state.buyerToken })
    if (r.json?.code !== 200) return bad(r.json?.message || '接口异常', '检查 t_after_sale 表是否存在')
    const broken = r.json.data.list.filter((x) => !x.statusText || !x.typeText)
    return broken.length === 0
      ? ok(`${r.json.data.list.length} 条记录文案齐全`)
      : bad(`${broken.length} 条缺失文案`, 'AfterSaleService.fillText 应在返回前调用（历史缺陷）')
  }]
])

// ---------------------------------------------------------------- L5
await layer('L5', '数据持久层与审计', '#l5-数据持久层', [
  ['库存与销量非负', async () => {
    const r = await call('GET', '/api/product/page?pageNum=1&pageSize=100')
    const badRows = (r.json?.data?.list || []).filter((p) => Number(p.stock) < 0 || Number(p.sales) < 0)
    return badRows.length === 0 ? ok(`${r.json?.data?.list?.length || 0} 件商品`) : bad(`${badRows.length} 件异常`, '检查扣库存的条件 UPDATE 是否被改成先查后写')
  }],
  ['访问审计日志已落库', async () => {
    if (!state.adminToken) return warn('无管理员令牌，跳过', '先修复 L3')
    const r = await call('GET', '/api/admin/secops/overview', { token: state.adminToken })
    const total = Number(r.json?.data?.totalRequests || 0)
    return total > 0 ? ok(`${total} 条`) : bad('审计表为空', '检查 SecOpsService.drainQueue 线程、t_access_log 表与异步写入异常')
  }],
  ['403 拦截已计入审计', async () => {
    if (!state.adminToken) return warn('无管理员令牌，跳过', '先修复 L3')
    const r = await call('GET', '/api/admin/secops/status-distribution', { token: state.adminToken })
    const hit = (r.json?.data || []).find((x) => x.status === '403')
    return hit ? ok(`${hit.count} 条 403`) : bad('未见 403 记录', 'L2 攻击请求没有留痕，检查 record() 的入队与落库')
  }],
  ['动态基线检测可运行', async () => {
    if (!state.adminToken) return warn('无管理员令牌，跳过', '先修复 L3')
    const r = await call('POST', '/api/admin/secops/detect', { token: state.adminToken })
    return r.json?.code === 200 && r.json.data?.score !== undefined
      ? ok(`${r.json.data.status}，评分 ${r.json.data.score}`)
      : bad('检测接口异常', '检查 SecOpsService.detectNow 与 t_security_alert')
  }]
])

// ---------------------------------------------------------------- L6
await layer('L6', '前端呈现层', '#l6-前端呈现层', [
  ['首页返回 SPA 入口', async () => {
    const r = await call('GET', '/')
    return r.status === 200 && /<div id="app"/.test(r.text) ? ok('index.html') : bad(`HTTP ${r.status}`, '确认 frontend 已执行 npm run build:server 并重新打 jar')
  }],
  ['history 路由回退正常', async () => {
    const r = await call('GET', '/after-sale')
    return r.status === 200 && /<div id="app"/.test(r.text) ? ok('/after-sale → index.html') : bad(`HTTP ${r.status}`, '检查 WebMvcConfig 的 PathResourceResolver 回退规则')
  }],
  ['静态资源无 404', async () => {
    const home = await call('GET', '/')
    const refs = [...home.text.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map((m) => m[1])
    state.refs = refs
    if (!refs.length) return bad('未解析到 /assets/ 资源', '前端产物可能未打进 jar')
    const broken = []
    for (const ref of refs) {
      const r = await call('GET', ref)
      if (r.status !== 200) broken.push(`${ref}(${r.status})`)
    }
    return broken.length === 0 ? ok(`${refs.length} 个资源全部 200`) : bad(broken.join(', '), '重新构建前端并清空 backend/src/main/resources/static 后重打 jar')
  }]
])

// ---------------------------------------------------------------- 汇总
console.log('\n' + '─'.repeat(64))
console.log('分层体检汇总')
console.log('─'.repeat(64))
for (const r of results) {
  const flag = r.failed ? 'FAIL' : r.warned ? 'WARN' : 'PASS'
  console.log(`  ${r.id.padEnd(5)} ${r.name.padEnd(30)} ${flag}  ✓${r.passed} ✗${r.failed} !${r.warned}`)
}
console.log('─'.repeat(64))
if (firstFailedLayer) {
  console.log(`\n➜ 建议从 ${firstFailedLayer.id} 开始排查：${firstFailedLayer.name}`)
  console.log(`  检查点、命令与常见根因见 docs/TROUBLESHOOTING.md${firstFailedLayer.docAnchor}`)
  console.log('  逐层向上排查：下层正常而上层异常时，优先怀疑两层之间的配置或数据。')
  process.exitCode = 1
} else {
  console.log('\n✓ 所有层通过。若仍有业务异常，优先复现路径并对照 docs/TESTING.md 的断言清单。')
}
console.log('')
