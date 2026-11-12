/**
 * 端到端分层回归（零第三方依赖，Node 18+ 内置 node:test / node:assert）。
 *
 * 用法：
 *   1) 启动后端：java -jar backend/target/life-shop-server.jar --spring.profiles.active=demo
 *   2) node tests/regression.mjs [http://127.0.0.1:8080]
 *
 * 设计约定：
 *   - 全部用例只读，不写业务数据，可重复运行（CI 每次构建都能跑）；
 *   - 按 L0~L6 分层组织，失败时能直接从层级名判断故障归属；
 *   - 每条断言都带业务含义说明，见 docs/TESTING.md 的「断言清单」；
 *   - 每次运行使用一个独立的 X-Forwarded-For 源 IP，避免上轮限流窗口影响本轮。
 */

import { before, describe, it } from 'node:test'
import assert from 'node:assert/strict'
import net from 'node:net'

const BASE = (process.argv[2] || process.env.LIFE_SHOP_BASE || 'http://127.0.0.1:8080').replace(/\/$/, '')
const URL_OBJ = new URL(BASE)
const RUN_IP = '198.51.100.' + (Math.floor(Math.random() * 200) + 1)

const ctx = {
  buyerToken: null,
  merchantToken: null,
  adminToken: null,
  merchantId: null,
  product: null
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function call(method, path, { token, body, headers = {} } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': RUN_IP,
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
      ...headers
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    redirect: 'manual'
  })
  const text = await res.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch {
    // 非 JSON 响应（前端 HTML、静态资源）由调用方按 text 断言
  }
  return { status: res.status, headers: res.headers, text, json }
}

function tcpReachable(port, host, timeout = 2000) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ port, host })
    const done = (value) => {
      socket.destroy()
      resolve(value)
    }
    socket.setTimeout(timeout)
    socket.once('connect', () => done(true))
    socket.once('timeout', () => done(false))
    socket.once('error', () => done(false))
  })
}

before(async () => {
  const port = Number(URL_OBJ.port || (URL_OBJ.protocol === 'https:' ? 443 : 80))
  const reachable = await tcpReachable(port, URL_OBJ.hostname)
  assert.ok(reachable, `L0 失败：${URL_OBJ.host}:${port} 端口不可达，请先启动后端（demo 模式）`)
  console.log(`\n目标服务 ${BASE} ｜ 本轮源 IP ${RUN_IP}\n`)
})

// ---------------------------------------------------------------- L0

describe('L0 运行环境', () => {
  it('Node 版本 >= 18（fetch / node:test 内置可用）', () => {
    const major = Number(process.versions.node.split('.')[0])
    assert.ok(major >= 18, `当前 Node ${process.versions.node}`)
    assert.equal(typeof fetch, 'function')
  })

  it('目标地址是合法的 HTTP(S) 地址', () => {
    assert.ok(['http:', 'https:'].includes(URL_OBJ.protocol))
    assert.ok(URL_OBJ.hostname.length > 0)
  })
})

// ---------------------------------------------------------------- L1

describe('L1 网络 / 协议层', () => {
  it('健康检查返回 200 与统一响应结构', async () => {
    const r = await call('GET', '/api/auth/health')
    assert.equal(r.status, 200, 'HTTP 状态应为 200')
    assert.equal(r.json.code, 200, '业务码应为 200')
    assert.equal(r.json.data, 'ok')
    assert.match(r.headers.get('content-type') || '', /application\/json/)
  })

  it('所有响应都带安全响应头（外层拦截器统一下发）', async () => {
    const r = await call('GET', '/api/auth/health')
    assert.equal(r.headers.get('x-content-type-options'), 'nosniff')
    assert.equal(r.headers.get('x-frame-options'), 'SAMEORIGIN')
    assert.equal(r.headers.get('referrer-policy'), 'strict-origin-when-cross-origin')
    assert.match(r.headers.get('content-security-policy-report-only') || '', /default-src 'self'/)
  })

  it('OPTIONS 预检请求不被鉴权拦截器拒绝', async () => {
    const r = await call('OPTIONS', '/api/user/info', {
      headers: { 'Access-Control-Request-Method': 'GET', Origin: BASE }
    })
    assert.ok(r.status < 400, `预检被拒绝：${r.status}`)
  })

  it('HTTP 方法语义正确：对只读接口发 POST 返回 4xx', async () => {
    const r = await call('POST', '/api/auth/health', { body: {} })
    assert.ok(r.status >= 400 && r.status < 500, `实际 ${r.status}`)
  })
})

// ---------------------------------------------------------------- L2

describe('L2 安全拦截层', () => {
  const attacks = [
    ['XSS', '/api/product/page?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E'],
    ['SQL 注入', "/api/product/page?q=1' or 1=1--"],
    ['UNION 注入', '/api/product/page?q=1 UNION ALL SELECT password FROM t_user'],
    ['路径穿越', '/api/product/page?file=../../etc/passwd']
  ]

  for (const [name, path] of attacks) {
    it(`${name} 载荷在到达 Controller 之前被 403 拦截`, async () => {
      const r = await call('GET', path)
      assert.equal(r.status, 403, `${name} 未被拦截，实际 ${r.status}`)
      assert.equal(r.json.code, 403)
      assert.match(r.json.message, /安全策略拦截/)
      assert.doesNotMatch(r.text, /Exception|at com\.lifeshop/, '错误体不得泄露内部堆栈')
    })
  }

  it('正常中文关键词不产生误拦截', async () => {
    const r = await call('GET', '/api/product/page?keyword=' + encodeURIComponent('保温杯'))
    assert.equal(r.status, 200)
    assert.equal(r.json.code, 200)
  })
})

// ---------------------------------------------------------------- L3

describe('L3 身份鉴权与权限边界', () => {
  it('未携带令牌访问受保护接口 → 业务码 401', async () => {
    const r = await call('GET', '/api/user/info')
    assert.equal(r.json.code, 401)
  })

  it('伪造令牌 → 业务码 401，且不返回用户信息', async () => {
    const r = await call('GET', '/api/user/info', { token: 'forged.token.value' })
    assert.equal(r.json.code, 401)
    assert.equal(r.json.data, null)
  })

  it('买家登录成功，返回令牌与 USER 角色', async () => {
    const r = await call('POST', '/api/auth/login', {
      body: { username: 'zhangsan', password: '123456' }
    })
    assert.equal(r.json.code, 200)
    assert.ok(r.json.data.token, '必须返回 JWT')
    assert.equal(r.json.data.user.username, 'zhangsan')
    assert.equal(r.json.data.user.role, 'USER')
    ctx.buyerToken = r.json.data.token
  })

  it('携带令牌可读取本人信息', async () => {
    const r = await call('GET', '/api/user/info', { token: ctx.buyerToken })
    assert.equal(r.json.code, 200)
    assert.equal(r.json.data.username, 'zhangsan')
  })

  it('普通买家访问管理员运维接口 → 403', async () => {
    const r = await call('GET', '/api/admin/secops/overview', { token: ctx.buyerToken })
    assert.equal(r.json.code, 403)
  })

  it('商家登录成功，且只能读到本店订单', async () => {
    const login = await call('POST', '/api/auth/login', {
      body: { username: 'shop_daily', password: '123456' }
    })
    assert.equal(login.json.code, 200)
    assert.equal(login.json.data.user.role, 'MERCHANT')
    ctx.merchantToken = login.json.data.token
    ctx.merchantId = login.json.data.merchant?.id
    assert.ok(ctx.merchantId, '商家登录必须带店铺信息')

    const r = await call('GET', '/api/merchant/order/page?pageNum=1&pageSize=50', {
      token: ctx.merchantToken
    })
    assert.equal(r.json.code, 200)
    assert.ok(Array.isArray(r.json.data.list))
    for (const order of r.json.data.list) {
      assert.equal(order.merchantId, ctx.merchantId,
        `出现越权订单 ${order.orderNo}（店铺 ${order.merchantId}）`)
    }
  })

  it('管理员登录成功，运维看板返回策略与统计字段', async () => {
    const login = await call('POST', '/api/auth/login', {
      body: { username: 'admin', password: 'admin123' }
    })
    assert.equal(login.json.code, 200, '管理员演示账号登录失败')
    ctx.adminToken = login.json.data.token

    const r = await call('GET', '/api/admin/secops/overview', { token: ctx.adminToken })
    assert.equal(r.json.code, 200)
    for (const key of ['totalRequests', 'errorRate', 'avgCostMs', 'activeIps',
      'blockedHits', 'suspiciousHits', 'unhandledAlerts', 'policy']) {
      assert.ok(key in r.json.data, `看板缺少字段 ${key}`)
    }
    assert.match(r.json.data.policy.algorithm, /3\.0/)
  })
})

// ---------------------------------------------------------------- L4

describe('L4 业务链路（只读）', () => {
  it('商品分页返回完整字段，价格与库存合法', async () => {
    const r = await call('GET', '/api/product/page?pageNum=1&pageSize=6')
    assert.equal(r.json.code, 200)
    assert.equal(r.json.data.list.length, 6)
    for (const p of r.json.data.list) {
      assert.ok(p.id > 0 && p.name && p.mainImage, '商品关键字段缺失')
      assert.ok(Number(p.price) > 0, '价格必须为正')
      assert.ok(Number(p.stock) >= 0, '库存不得为负')
      assert.ok(p.merchantId > 0, '商品必须归属店铺')
    }
    ctx.product = r.json.data.list[0]
  })

  it('商品详情与列表数据一致', async () => {
    const r = await call('GET', `/api/product/${ctx.product.id}`)
    assert.equal(r.json.code, 200)
    assert.equal(r.json.data.id, ctx.product.id)
    assert.equal(r.json.data.name, ctx.product.name)
  })

  it('分类列表与买家订单分页结构正确', async () => {
    const cats = await call('GET', '/api/category/list')
    assert.equal(cats.json.code, 200)
    assert.equal(cats.json.data.length, 6)

    const orders = await call('GET', '/api/order/page?pageNum=1&pageSize=5', {
      token: ctx.buyerToken
    })
    assert.equal(orders.json.code, 200)
    for (const key of ['total', 'pageNum', 'pageSize', 'list']) {
      assert.ok(key in orders.json.data, `订单分页缺少 ${key}`)
    }
  })

  it('售后列表文案完整，且与状态码一一对应（回归 fillText 缺失缺陷）', async () => {
    const r = await call('GET', '/api/after-sale/my?pageNum=1&pageSize=50', {
      token: ctx.buyerToken
    })
    assert.equal(r.json.code, 200)
    const textByStatus = {
      0: '待商家处理', 1: '待买家寄回', 2: '待商家收货',
      3: '退款完成', 4: '已拒绝', 5: '已撤销'
    }
    for (const as of r.json.data.list) {
      assert.ok(as.statusText, 'statusText 不得为空')
      assert.ok(as.typeText, 'typeText 不得为空')
      assert.equal(as.statusText, textByStatus[as.status])
      assert.ok(['仅退款', '退货退款'].includes(as.typeText))
      assert.ok(Number(as.amount) > 0, '退款金额必须为正')
    }
  })

  it('不存在的订单项申请售后 → 业务异常，且不产生脏数据', async () => {
    const r = await call('POST', '/api/after-sale', {
      token: ctx.buyerToken,
      body: { orderItemId: 999999999, type: 1, reason: '回归测试' }
    })
    assert.equal(r.json.code, 500)
    assert.match(r.json.message, /订单商品不存在/)
  })

  it('商家售后列表同样受店铺数据边界约束', async () => {
    const r = await call('GET', '/api/merchant/after-sale/page?pageNum=1&pageSize=50', {
      token: ctx.merchantToken
    })
    assert.equal(r.json.code, 200)
    for (const as of r.json.data.list) {
      assert.equal(as.merchantId, ctx.merchantId)
      assert.ok(as.status >= 0 && as.status <= 5)
    }
  })
})

// ---------------------------------------------------------------- L5

describe('L5 数据一致性与审计落库', () => {
  it('全量商品库存 / 销量均非负（条件更新防超卖的结果约束）', async () => {
    const r = await call('GET', '/api/product/page?pageNum=1&pageSize=100')
    assert.equal(r.json.code, 200)
    assert.ok(r.json.data.list.length >= 40, '演示数据应含 48 件商品')
    for (const p of r.json.data.list) {
      assert.ok(Number(p.stock) >= 0, `${p.name} 库存为负`)
      assert.ok(Number(p.sales) >= 0, `${p.name} 销量为负`)
    }
  })

  it('商家钱包四个金额字段均非负（退款 / 提现落在同一余额上）', async () => {
    const r = await call('GET', '/api/merchant/wallet', { token: ctx.merchantToken })
    assert.equal(r.json.code, 200)
    for (const key of ['balance', 'unsettled', 'pendingWithdraw', 'totalWithdrawn']) {
      assert.ok(Number(r.json.data[key]) >= 0, `${key} 为负，资金账目异常`)
    }
  })

  it('L2 的攻击请求已经异步落入访问审计（跨层验证）', async () => {
    // 审计写入是独立线程批量落库，等待一个写入周期
    await sleep(1200)
    const r = await call('GET', '/api/admin/secops/status-distribution', {
      token: ctx.adminToken
    })
    assert.equal(r.json.code, 200)
    const row403 = r.json.data.find((x) => x.status === '403')
    assert.ok(row403 && Number(row403.count) >= 4,
      '未查到 403 审计记录，检查 SecOpsService 异步写线程或 t_access_log 表')
  })

  it('运维看板统计到本轮请求与可疑命中', async () => {
    const r = await call('GET', '/api/admin/secops/overview', { token: ctx.adminToken })
    assert.equal(r.json.code, 200)
    assert.ok(r.json.data.totalRequests > 0, '审计日志表为空')
    assert.ok(r.json.data.suspiciousHits >= 4, '可疑载荷命中数应至少为 4')
  })
})

// ---------------------------------------------------------------- L6

describe('L6 前端呈现层', () => {
  it('首页返回 SPA 入口 HTML', async () => {
    const r = await call('GET', '/')
    assert.equal(r.status, 200)
    assert.match(r.headers.get('content-type') || '', /text\/html/)
    assert.match(r.text, /<div id="app"/)
  })

  it('history 路由直达地址回退到 index.html', async () => {
    const r = await call('GET', '/after-sale')
    assert.equal(r.status, 200)
    assert.match(r.text, /<div id="app"/)
  })

  it('首页引用的本地静态资源全部可访问（防止白屏 / chunk 404）', async () => {
    const home = await call('GET', '/')
    const refs = [...home.text.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map((m) => m[1])
    assert.ok(refs.length > 0, '未解析到任何本地静态资源，检查前端构建产物')
    for (const ref of refs) {
      const r = await call('GET', ref)
      assert.equal(r.status, 200, `静态资源缺失：${ref}`)
    }
  })
})
