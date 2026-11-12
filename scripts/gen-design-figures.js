/**
 * 生成《系统设计说明书》所需插图（PNG，2 倍分辨率）
 * 输出目录：docs/figures
 *
 * 图 3-1  系统功能逻辑图
 * 图 3-2  分层架构图
 * 图 3-3 ~ 3-15  各实体属性图（Chen 记法：矩形实体 + 椭圆属性）
 * 图 3-16 全局 E-R 图
 */
const fs = require('fs')
const path = require('path')
const puppeteer = require('puppeteer')

const ROOT = path.resolve(__dirname, '..')
const OUT = path.join(ROOT, 'docs', 'figures')
fs.mkdirSync(OUT, { recursive: true })

const FONT = "'SimSun','宋体',serif"
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

/** 估算文本宽度：中文 11px，英文数字 6px */
function textW(s, fs = 12) {
  let w = 0
  for (const ch of s) w += /[\x00-\xff]/.test(ch) ? fs * 0.55 : fs
  return w
}

const clamp = (v, a, b) => Math.max(a, Math.min(b, v))

// ---------------------------------------------------------------- 实体属性图
function entitySvg(name, attrs) {
  const half = Math.ceil(attrs.length / 2)
  const top = attrs.slice(0, half)
  const bottom = attrs.slice(half)
  const cols = Math.max(top.length, bottom.length, 1)
  const W = Math.max(640, cols * 142 + 40)
  const H = 330
  const cx = W / 2
  const cy = H / 2
  const rw = Math.max(180, textW(name, 13.5) + 60)
  const rh = 52
  const ry = 20

  const posX = (n) => {
    if (n <= 1) return [cx]
    const span = W - 132
    return Array.from({ length: n }, (_, i) => 66 + (span * i) / (n - 1))
  }

  let s = ''
  const topX = posX(top.length)
  const botX = posX(bottom.length)
  const cyTop = 56
  const cyBot = H - 56

  // 连线
  top.forEach((a, i) => {
    const ex = topX[i]
    const x1 = clamp(ex, cx - rw / 2 + 14, cx + rw / 2 - 14)
    s += `<line x1="${x1.toFixed(1)}" y1="${cy - rh / 2}" x2="${ex.toFixed(1)}" y2="${cyTop + ry}" stroke="#333" stroke-width="1"/>`
  })
  bottom.forEach((a, i) => {
    const ex = botX[i]
    const x1 = clamp(ex, cx - rw / 2 + 14, cx + rw / 2 - 14)
    s += `<line x1="${x1.toFixed(1)}" y1="${cy + rh / 2}" x2="${ex.toFixed(1)}" y2="${cyBot - ry}" stroke="#333" stroke-width="1"/>`
  })

  // 实体矩形
  s += `<rect x="${cx - rw / 2}" y="${cy - rh / 2}" width="${rw}" height="${rh}" fill="#fff" stroke="#333" stroke-width="1.2"/>`
  s += `<text x="${cx}" y="${cy + 5}" text-anchor="middle" font-size="13.5" font-family="${FONT}" fill="#000">${esc(name)}</text>`

  // 属性椭圆
  const ellipse = (label, x, y) => {
    const rx = Math.max(40, textW(label, 12.5) / 2 + 14)
    return (
      `<ellipse cx="${x.toFixed(1)}" cy="${y}" rx="${rx.toFixed(1)}" ry="${ry}" fill="#fff" stroke="#333" stroke-width="1"/>` +
      `<text x="${x.toFixed(1)}" y="${y + 4.5}" text-anchor="middle" font-size="12.5" font-family="${FONT}" fill="#000">${esc(label)}</text>`
    )
  }
  top.forEach((a, i) => (s += ellipse(a, topX[i], cyTop)))
  bottom.forEach((a, i) => (s += ellipse(a, botX[i], cyBot)))

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`
}

// ---------------------------------------------------------------- 功能逻辑图
function functionSvg() {
  const COLS = [
    { title: '商品', items: ['商品分类展示', '热销商品推荐', '最新上架商品', '商品详情展示'] },
    { title: '交易', items: ['加入购物车', '提交订单', '在线支付', '订单查询', '取消订单'] },
    { title: '互动', items: ['商品评价', '商家回复', '在线咨询'] },
    { title: '商家管理', items: ['商品上架下架', '订单发货', '评价回复', '店铺设置'] },
    { title: '平台管理', items: ['用户管理', '分类管理', '菜单管理', '数据统计'] },
    { title: '用户注册', items: ['用户登录', '账号密码', '处理登录信息'] },
  ]
  const W = 830
  const startX = 70
  const gap = 130
  const colX = COLS.map((_, i) => startX + i * gap)
  const headY = 92
  const headH = 34
  const firstY = 166
  const step = 34

  let s = ''
  // 根节点（水平居中于整棵树）
  const rootW = 200
  const midX = (colX[0] + 48 + colX[COLS.length - 1] + 48) / 2
  s += `<rect x="${midX - rootW / 2}" y="20" width="${rootW}" height="42" fill="#fff" stroke="#333" stroke-width="1.2"/>`
  s += `<text x="${midX}" y="46" text-anchor="middle" font-size="14" font-family="${FONT}">在线生活用品商城</text>`

  // 根到各列的连线（走 y=72 的横梁）
  const barY = 72
  s += `<line x1="${midX}" y1="62" x2="${midX}" y2="${barY}" stroke="#333" stroke-width="1"/>`
  s += `<line x1="${colX[0] + 48}" y1="${barY}" x2="${colX[COLS.length - 1] + 48}" y2="${barY}" stroke="#333" stroke-width="1"/>`

  COLS.forEach((col, ci) => {
    const bx = colX[ci] + 48 // 子节点盒子中心 x
    const trunk = colX[ci] // 主干竖线 x
    s += `<line x1="${bx}" y1="${barY}" x2="${bx}" y2="${headY}" stroke="#333" stroke-width="1"/>`

    s += `<rect x="${bx - 52}" y="${headY}" width="104" height="${headH}" fill="#fff" stroke="#333" stroke-width="1.2"/>`
    s += `<text x="${bx}" y="${headY + 22}" text-anchor="middle" font-size="13" font-family="${FONT}">${esc(col.title)}</text>`

    const lastCy = firstY + (col.items.length - 1) * step + 13
    s += `<line x1="${trunk}" y1="${headY + headH}" x2="${trunk}" y2="${lastCy}" stroke="#333" stroke-width="1"/>`
    col.items.forEach((item, ii) => {
      const y = firstY + ii * step
      const cyItem = y + 13
      s += `<line x1="${trunk}" y1="${cyItem}" x2="${trunk + 8}" y2="${cyItem}" stroke="#333" stroke-width="1"/>`
      s += `<rect x="${trunk + 8}" y="${y}" width="104" height="26" fill="#fff" stroke="#333" stroke-width="1"/>`
      s += `<text x="${trunk + 60}" y="${y + 17.5}" text-anchor="middle" font-size="12" font-family="${FONT}">${esc(item)}</text>`
    })
  })

  // 登录分支：校验 → 角色分流
  const lx = colX[5] + 48
  const box = (x, y, w, h, t, fs2) =>
    `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="#fff" stroke="#333" stroke-width="1"/>` +
    `<text x="${x + w / 2}" y="${y + h / 2 + 4}" text-anchor="middle" font-size="${fs2 || 11}" font-family="${FONT}">${esc(t)}</text>`

  const y0 = 300
  s += `<line x1="${lx}" y1="${firstY + 2 * step + 26}" x2="${lx}" y2="${y0}" stroke="#333" stroke-width="1"/>`
  s += box(lx - 66, y0, 132, 32, '校验账号与密码')
  s += `<line x1="${lx}" y1="${y0 + 32}" x2="${lx}" y2="${y0 + 58}" stroke="#333" stroke-width="1"/>`
  s += `<polygon points="${lx},${y0 + 58} ${lx + 8},${y0 + 67} ${lx},${y0 + 76} ${lx - 8},${y0 + 67}" fill="#fff" stroke="#333" stroke-width="1"/>`
  s += `<text x="${lx + 16}" y="${y0 + 71}" font-size="11" font-family="${FONT}">正确</text>`
  s += `<text x="${lx - 30}" y="${y0 + 71}" text-anchor="end" font-size="11" font-family="${FONT}">不正确</text>`
  s += `<line x1="${lx - 8}" y1="${y0 + 67}" x2="${lx - 92}" y2="${y0 + 67}" stroke="#333" stroke-width="1"/>`
  s += box(lx - 232, y0 + 51, 140, 32, '返回错误提示')

  s += `<line x1="${lx}" y1="${y0 + 76}" x2="${lx}" y2="${y0 + 104}" stroke="#333" stroke-width="1"/>`
  s += box(lx - 78, y0 + 104, 156, 32, '按角色分流进入')
  s += `<text x="${lx - 92}" y="${y0 + 124}" text-anchor="end" font-size="10.5" font-family="${FONT}">签发 JWT 并写入登录态</text>`

  const roleY = y0 + 162
  const roles = [
    { x: 445, t: '买家 · 商城' },
    { x: 575, t: '卖家 · 商家中心' },
    { x: 705, t: '管理员 · 后台' },
  ]
  s += `<line x1="${lx}" y1="${y0 + 136}" x2="${lx}" y2="${roleY - 8}" stroke="#333" stroke-width="1"/>`
  s += `<line x1="${roles[0].x + 50}" y1="${roleY - 8}" x2="${roles[2].x + 50}" y2="${roleY - 8}" stroke="#333" stroke-width="1"/>`
  roles.forEach((r) => {
    s += `<line x1="${r.x + 50}" y1="${roleY - 8}" x2="${r.x + 50}" y2="${roleY}" stroke="#333" stroke-width="1"/>`
    s += box(r.x, roleY, 100, 28, r.t, 10.5)
  })

  const H = roleY + 56
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`
}

// ---------------------------------------------------------------- 分层架构图
function archSvg() {
  const W = 700
  const layers = [
    ['用户接入层', ['买家商城（PC / H5）', '卖家中心', '平台管理后台']],
    ['前端展示层', ['Vue 3 + Vite', 'Vue Router / Pinia', 'Element Plus / ECharts']],
    ['接口鉴权层', ['RESTful API（/api）', 'JWT 三级拦截器', '统一响应与异常处理']],
    ['业务服务层', ['商品·购物车·订单', '评价·咨询·统计', '店铺·用户·菜单']],
    ['数据访问层', ['MyBatis-Plus Mapper', '条件构造器与分页', '声明式事务']],
    ['数据存储层', ['MySQL 8 / H2（演示）', '本地文件 /uploads', '静态资源 /static']],
  ]
  const rowH = 74
  const H = 20 + layers.length * rowH + 16
  let s = ''
  layers.forEach(([label, boxes], i) => {
    const y = 16 + i * rowH
    s += `<rect x="8" y="${y}" width="${W - 16}" height="${rowH - 12}" fill="#fbfcfe" stroke="#c9d3e2" stroke-width="1"/>`
    s += `<text x="24" y="${y + (rowH - 12) / 2 + 4}" font-size="11.5" font-family="${FONT}" font-weight="bold">${esc(label)}</text>`
    boxes.forEach((b, j) => {
      const x = 124 + j * 190
      s += `<rect x="${x}" y="${y + 16}" width="176" height="${rowH - 44}" fill="#fff" stroke="#8fa3bf" stroke-width="1"/>`
      s += `<text x="${x + 88}" y="${y + 16 + (rowH - 44) / 2 + 4}" text-anchor="middle" font-size="11" font-family="${FONT}">${esc(b)}</text>`
    })
    if (i < layers.length - 1) {
      s += `<polygon points="${W / 2 - 4},${y + rowH - 12} ${W / 2 + 4},${y + rowH - 12} ${W / 2},${y + rowH - 5}" fill="#64748b"/>`
    }
  })
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`
}

// ---------------------------------------------------------------- 全局 E-R 图
function globalErSvg() {
  // 放大 2 倍画布与字体，提升打印清晰度（横向布局，10 个实体）
  const S = 2
  const W = 860 * S
  const H = 470 * S
  let s = ''
  const EW = 110 * S
  const EH = 40 * S
  const box = (cx, cy, t, fs2) =>
    `<rect x="${cx - EW / 2}" y="${cy - EH / 2}" width="${EW}" height="${EH}" fill="#fff" stroke="#333" stroke-width="1.6"/>` +
    `<text x="${cx}" y="${cy + 5}" text-anchor="middle" font-size="${(fs2 || 13.5) * S}" font-family="${FONT}">${esc(t)}</text>`

  const dw = 74 * S
  const dh = 38 * S
  const diamond = (cx, cy, t) =>
    `<polygon points="${cx},${cy - dh / 2} ${cx + dw / 2},${cy} ${cx},${cy + dh / 2} ${cx - dw / 2},${cy}" fill="#fff" stroke="#333" stroke-width="1.4"/>` +
    `<text x="${cx}" y="${cy + 4}" text-anchor="middle" font-size="${12 * S}" font-family="${FONT}">${esc(t)}</text>`

  const line = (x1, y1, x2, y2) =>
    `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#333" stroke-width="1.3"/>`

  const lab = (x, y, t) =>
    `<text x="${x}" y="${y}" font-size="${11.5 * S}" font-family="${FONT}" fill="#333">${t}</text>`

  const P = (a) => a * S
  const E = {
    user: [P(85), P(250)],
    addr: [P(85), P(120)],
    cart: [P(85), P(380)],
    order: [P(300), P(250)],
    review: [P(300), P(105)],
    item: [P(520), P(250)],
    prod: [P(750), P(250)],
    cate: [P(750), P(105)],
    shop: [P(300), P(410)],
  }

  // ---- 连线（先画，后画节点压住线端）
  s += line(P(85), P(230), P(85), P(207)) + lab(P(91), P(222), '1')
  s += line(P(85), P(169), P(85), P(140)) + lab(P(91), P(158), 'N')
  s += line(P(85), P(270), P(85), P(299)) + lab(P(91), P(286), 'N')
  s += line(P(85), P(337), P(85), P(360)) + lab(P(91), P(353), '1')
  s += line(P(140), P(250), P(158), P(250)) + lab(P(145), P(244), '1')
  s += line(P(232), P(250), P(245), P(250)) + lab(P(235), P(244), 'N')
  s += line(P(300), P(230), P(300), P(191)) + lab(P(306), P(214), '1')
  s += line(P(300), P(153), P(300), P(125)) + lab(P(306), P(140), 'N')
  s += line(P(355), P(250), P(378), P(250)) + lab(P(361), P(244), '1')
  s += line(P(452), P(250), P(465), P(250)) + lab(P(455), P(244), 'N')
  s += line(P(575), P(250), P(598), P(250)) + lab(P(581), P(244), 'N')
  s += line(P(672), P(250), P(695), P(250)) + lab(P(678), P(244), '1')
  s += line(P(750), P(230), P(750), P(197)) + lab(P(756), P(218), 'N')
  s += line(P(750), P(159), P(750), P(125)) + lab(P(756), P(145), '1')
  s += line(P(750), P(270), P(750), P(301)) + lab(P(756), P(288), '1')
  s += line(P(750), P(339), P(750), P(380)) + lab(P(756), P(362), 'N')
  // 店铺 1:N 订单
  s += line(P(300), P(270), P(300), P(321)) + lab(P(306), P(296), 'N')
  s += line(P(300), P(359), P(300), P(390)) + lab(P(306), P(378), '1')
  // 店铺 1:N 商品
  s += line(P(355), P(402), P(487), P(352)) + lab(P(400), P(372), '1')
  s += line(P(553), P(330), P(700), P(278)) + lab(P(676), P(296), 'N')

  s += box(...E.user, '用户账号')
  s += box(...E.addr, '收货地址')
  s += box(...E.cart, '购物车')
  s += box(...E.order, '订单')
  s += box(...E.review, '商品评价')
  s += box(...E.item, '订单明细')
  s += box(...E.prod, '商品')
  s += box(...E.cate, '商品类别')
  s += box(...E.shop, '卖家店铺')
  s += box(P(750), P(400), '上下架记录')

  s += diamond(P(85), P(188), '拥有')
  s += diamond(P(85), P(318), '加购')
  s += diamond(P(195), P(250), '提交')
  s += diamond(P(300), P(172), '评价')
  s += diamond(P(415), P(250), '包含')
  s += diamond(P(635), P(250), '包含')
  s += diamond(P(750), P(178), '归类')
  s += diamond(P(750), P(320), '变更')
  s += diamond(P(300), P(340), '拆单')
  s += diamond(P(520), P(341), '发布')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`
}

// ---------------------------------------------------------------- 图目录
const ENTITIES = [
  ['fig-3-3-user', '用户账号信息', ['账号ID', '用户名', '账号密码', '姓名', '手机号', '性别', '用户类型', '账号状态', '创建时间']],
  ['fig-3-4-shop', '卖家店铺信息', ['店铺ID', '账号ID', '店铺名称', '店铺Logo', '联系人', '联系电话', '发货地址', '店铺状态']],
  ['fig-3-5-address', '收货地址信息', ['地址ID', '账号ID', '收货人', '手机号', '省份', '城市', '区县', '详细地址', '是否默认', '创建时间']],
  ['fig-3-6-menu', '菜单信息', ['菜单ID', '父级菜单ID', '菜单名称', '路由地址', '权限标识', '菜单类型', '排序号', '显示状态']],
  ['fig-3-7-cart', '购物车信息', ['购物车ID', '账号ID', '商品ID', '商品名称', '商品图片地址', '商品单价', '商品数量', '交付金额', '是否勾选']],
  ['fig-3-8-category', '商品类别信息', ['类别ID', '类别名称', '类别图标', '排序号', '类别状态']],
  ['fig-3-9-product', '商品信息', ['商品ID', '店铺ID', '类别ID', '商品名称', '商品图片', '商品单价', '商品库存', '商品销量', '上架状态']],
  ['fig-3-10-shelf', '卖家上下架信息', ['记录ID', '商品ID', '店铺ID', '操作类型', '操作前状态', '操作后状态', '操作原因', '操作时间']],
  ['fig-3-11-order', '订单信息', ['订单ID', '订单编号', '账号ID', '店铺ID', '商品总额', '运费', '实付金额', '订单状态', '收货人', '创建时间']],
  ['fig-3-12-orderitem', '订单明细信息', ['明细ID', '订单ID', '商品ID', '商品名称', '商品单价', '购买数量', '小计金额']],
  ['fig-3-13-history', '历史订单信息', ['历史ID', '订单ID', '操作方', '操作动作', '原状态', '新状态', '操作时间']],
  ['fig-3-14-review', '商品评价信息', ['评价ID', '订单ID', '商品ID', '账号ID', '评价等级', '评价内容', '评价图片', '商家回复', '创建时间']],
  ['fig-3-15-admin', '管理员信息', ['管理员ID', '登录账号', '登录密码', '真实姓名', '角色编码', '账号状态', '最后登录时间']],
]

const FIGS = [
  { name: 'fig-3-1-function', svg: functionSvg() },
  { name: 'fig-3-2-arch', svg: archSvg() },
  ...ENTITIES.map(([name, title, attrs]) => ({ name, svg: entitySvg(title, attrs) })),
  { name: 'fig-3-16-global-er', svg: globalErSvg() },
]

;(async () => {
  const browser = await puppeteer.launch({ args: ['--no-sandbox', '--font-render-hinting=none'] })
  const page = await browser.newPage()
  await page.setViewport({ width: 1200, height: 800, deviceScaleFactor: 2 })

  for (const fig of FIGS) {
    const m = fig.svg.match(/width="(\d+)" height="(\d+)"/)
    const w = Number(m[1])
    const h = Number(m[2])
    await page.setViewport({ width: w, height: h, deviceScaleFactor: 2 })
    await page.setContent(
      `<!DOCTYPE html><html><head><meta charset="utf-8"><style>html,body{margin:0;padding:0;background:#fff}</style></head><body>${fig.svg}</body></html>`,
      { waitUntil: 'load' }
    )
    const file = path.join(OUT, fig.name + '.png')
    await page.screenshot({ path: file, clip: { x: 0, y: 0, width: w, height: h } })
    console.log('  ' + fig.name + '.png  ' + w + 'x' + h)
  }

  await browser.close()
  console.log('共 ' + FIGS.length + ' 张图 -> ' + path.relative(ROOT, OUT))
})()
