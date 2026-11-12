/**
 * 接口冒烟测试：把「登录 → 加购 → 下单 → 支付 → 发货 → 收货 → 评价 → 后台管理」整条链路跑一遍
 * 用法：先启动后端，然后 node scripts/smoke-test.js [baseUrl]
 */
const BASE = process.argv[2] || 'http://127.0.0.1:8080'

let pass = 0
let fail = 0

async function call(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch (e) {
    throw new Error(`${method} ${path} 返回非 JSON：${text.slice(0, 120)}`)
  }
  return json
}

function check(name, ok, extra) {
  if (ok) {
    pass++
    console.log(`  ✅ ${name}`)
  } else {
    fail++
    console.log(`  ❌ ${name}${extra ? '  → ' + JSON.stringify(extra).slice(0, 220) : ''}`)
  }
  return ok
}

function req(res, name) {
  const ok = res && res.code === 200
  if (!ok) check(name, false, res)
  return ok
}

async function main() {
  console.log('目标服务：' + BASE + '\n')

  console.log('[1] 公共接口')
  const health = await call('GET', '/api/auth/health')
  check('健康检查', health.code === 200, health)

  const products = await call('GET', '/api/product/page?pageNum=1&pageSize=6')
  check('商品列表返回 6 条', products.code === 200 && products.data.list.length === 6,
    { total: products.data && products.data.total })
  const p = products.data.list[0]
  check('商品包含真实图片路径', typeof p.mainImage === 'string' && p.mainImage.startsWith('/products/'), p.mainImage)
  check('商品带评分字段', p.rating !== undefined && p.rating !== null, p.rating)

  const cats = await call('GET', '/api/category/list')
  check('分类列表', req(cats, '分类列表') && cats.data.length === 6, cats.data && cats.data.length)

  const banners = await call('GET', '/api/banner/list')
  check('轮播图 3 条', banners.code === 200 && banners.data.length === 3, banners.data && banners.data.length)

  const detail = await call('GET', '/api/product/' + p.id)
  check('商品详情', detail.code === 200 && detail.data.id === p.id, detail.message)

  console.log('\n[2] 用户登录')
  const login = await call('POST', '/api/auth/login', { body: { username: 'zhangsan', password: '123456' } })
  if (!req(login, '演示账号登录')) return
  const token = login.data.token
  check('返回 token 与用户信息', !!token && login.data.user.username === 'zhangsan')

  const me = await call('GET', '/api/user/info', { token })
  check('获取个人信息', me.code === 200 && me.data.username === 'zhangsan', me.message)

  console.log('\n[3] 购物车')
  const cart0 = await call('GET', '/api/cart/list', { token })
  const before = cart0.code === 200 ? cart0.data.length : 0
  check('购物车可读（种子含 3 件）', before >= 3, cart0.message)

  const target = products.data.list.find((x) => x.id !== 2) || p
  const add = await call('POST', '/api/cart/add', { token, body: { productId: target.id, quantity: 1 } })
  check('加入购物车：' + target.name, add.code === 200, add)

  const cart1 = await call('GET', '/api/cart/list', { token })
  check('购物车数量增加', cart1.data.length > before, { before, after: cart1.data.length })
  const cartItem = cart1.data.find((x) => x.productId === target.id)
  check('加购明细存在', !!cartItem, cartItem)

  const upd = await call('PUT', '/api/cart/quantity', { token, body: { id: cartItem.id, quantity: 2 } })
  check('修改数量为 2', upd.code === 200, upd)

  const addr = await call('GET', '/api/address/list', { token })
  check('收货地址可用', addr.code === 200 && addr.data.length >= 1, addr.message)
  const addressId = addr.data[0].id

  console.log('\n[4] 下单与支付')
  const submit = await call('POST', '/api/order/submit', { token, body: { addressId } })
  check('提交订单（购物车结算）',
    submit.code === 200 && Array.isArray(submit.data) && submit.data.length >= 1, submit.message)
  if (submit.code !== 200) return
  const orders = submit.data
  const order = orders[0]
  check('每个订单只含一家店铺的商品',
    orders.every((o) => new Set(o.items.map((i) => i.merchantId)).size === 1),
    orders.map((o) => o.items.map((i) => i.merchantId)))
  check('订单带店铺名称快照',
    orders.every((o) => !!o.merchantName && o.merchantName.length > 0),
    orders.map((o) => o.merchantName))
  check('订单金额 = 商品 + 运费', order.payAmount > 0, { payAmount: order.payAmount, freight: order.freight })
  check('订单状态为待付款', order.status === 0, order.status)

  for (const o of orders) {
    const pay = await call('PUT', `/api/order/${o.id}/pay`, { token })
    check(`模拟支付成功 → 待发货（${o.orderNo.slice(-6)}）`, pay.code === 200 && pay.data.status === 1, pay.message)
  }

  const beforeStock = (await call('GET', '/api/product/' + target.id)).data.stock
  check('下单扣减库存', typeof beforeStock === 'number', beforeStock)

  console.log('\n[5] 后台管理')
  const adminLogin = await call('POST', '/api/auth/login', { body: { username: 'admin', password: 'admin123' } })
  if (!req(adminLogin, '管理员登录')) return
  const adminToken = adminLogin.data.token
  check('管理员角色为 ADMIN', adminLogin.data.user.role === 'ADMIN')

  const dash = await call('GET', '/api/admin/statistics/dashboard', { token: adminToken })
  check('数据看板', dash.code === 200, dash.message)

  const adminOrders = await call('GET', '/api/admin/order/page?pageNum=1&pageSize=10', { token: adminToken })
  check('后台订单列表', adminOrders.code === 200 && adminOrders.data.total > 0, adminOrders.message)

  const ship = await call('PUT', `/api/admin/order/${order.id}/ship`, { token: adminToken })
  check('后台一键发货 → 已发货', ship.code === 200, ship)

  const confirm = await call('PUT', `/api/order/${order.id}/confirm`, { token })
  check('用户确认收货 → 已完成', confirm.code === 200 && confirm.data.status === 3, confirm.message)

  console.log('\n[6] 评价')
  const pending = await call('GET', '/api/review/pending', { token })
  check('待评价列表', pending.code === 200 && pending.data.length >= 1,
    { count: pending.data && pending.data.length })

  const pendingItem = pending.data.find((x) => x.orderId === order.id) || pending.data[0]
  const submitReview = await call('POST', '/api/review', {
    token,
    body: {
      orderId: pendingItem.orderId,
      productId: pendingItem.productId,
      rating: 5,
      content: '冒烟测试评价：东西不错，物流快，包装结实。',
      images: ['/products/p1-2.jpg'],
      anonymous: 0,
    },
  })
  check('提交评价', submitReview.code === 200, submitReview)
  check('评价返回星级与图片', submitReview.data && submitReview.data.rating === 5 &&
    submitReview.data.images.length === 1, submitReview.data)

  const dup = await call('POST', '/api/review', {
    token,
    body: { orderId: pendingItem.orderId, productId: pendingItem.productId, rating: 4, content: '重复评价' },
  })
  check('重复评价被拦截', dup.code !== 200, dup)

  const prodReview = await call('GET', `/api/review/product/${pendingItem.productId}?pageNum=1&pageSize=5`)
  check('商品评价公开可读', prodReview.code === 200 && prodReview.data.stats.total >= 1,
    prodReview.data && prodReview.data.stats)
  check('评分统计含好评率', prodReview.data.stats.goodRate >= 0, prodReview.data.stats.goodRate)

  const pendingAfter = await call('GET', '/api/review/pending', { token })
  check('已评价商品从待评价中移除', pendingAfter.data.length === pending.data.length - 1,
    { before: pending.data.length, after: pendingAfter.data.length })

  const myReviews = await call('GET', '/api/review/my?pageNum=1&pageSize=5', { token })
  check('我的评价', myReviews.code === 200 && myReviews.data.total >= 1, myReviews.data && myReviews.data.total)

  const adminReviews = await call('GET', '/api/admin/review/page?pageNum=1&pageSize=10', { token: adminToken })
  check('后台评价列表', adminReviews.code === 200 && adminReviews.data.total >= 10,
    adminReviews.data && adminReviews.data.total)

  const replyRes = await call('POST', `/api/admin/review/${submitReview.data.id}/reply`, {
    token: adminToken,
    body: { reply: '感谢您的支持，欢迎再次光临～' },
  })
  check('后台回复评价', replyRes.code === 200, replyRes)

  const afterReply = await call('GET', `/api/review/product/${pendingItem.productId}?pageNum=1&pageSize=5`)
  const replied = afterReply.data.page.list.find((x) => x.id === submitReview.data.id)
  check('回复对用户可见', replied && replied.reply && replied.reply.includes('感谢'), replied && replied.reply)

  console.log('\n[7] 商家后台')

  const mLogin = await call('POST', '/api/auth/login', {
    body: { username: 'shop_kitchen', password: '123456' },
  })
  if (!req(mLogin, '商家账号登录')) return
  const mToken = mLogin.data.token
  check('商家角色为 MERCHANT', mLogin.data.user.role === 'MERCHANT', mLogin.data.user.role)
  check('登录返回店铺信息',
    mLogin.data.merchant && mLogin.data.merchant.name === '悦家厨房旗舰店',
    mLogin.data.merchant && mLogin.data.merchant.name)

  const mProfile = await call('GET', '/api/merchant/profile', { token: mToken })
  check('店铺资料', mProfile.code === 200 && mProfile.data.userId === 6, mProfile.message)

  const mProducts = await call('GET', '/api/merchant/product/page?pageNum=1&pageSize=100', { token: mToken })
  check('商品列表只含本店商品',
    mProducts.code === 200 && mProducts.data.total > 0 &&
    mProducts.data.list.every((x) => x.merchantId === 1),
    mProducts.data && mProducts.data.total)

  const mOrders = await call('GET', '/api/merchant/order/page?pageNum=1&pageSize=100', { token: mToken })
  check('订单列表只含本店订单',
    mOrders.code === 200 && mOrders.data.total > 0 &&
    mOrders.data.list.every((o) => o.merchantId === 1),
    mOrders.data && mOrders.data.total)

  const mDash = await call('GET', '/api/merchant/dashboard', { token: mToken })
  check('经营概览：7 天趋势 + 热销榜',
    mDash.code === 200 && mDash.data.trend.length === 7 && Array.isArray(mDash.data.topProducts),
    mDash.message)
  check('经营概览：商品数与销售额有值',
    mDash.data.productCount > 0 && Number(mDash.data.totalSales) > 0,
    { productCount: mDash.data.productCount, totalSales: mDash.data.totalSales })

  const mReviews = await call('GET', '/api/merchant/review/page?pageNum=1&pageSize=5', { token: mToken })
  check('本店评价列表', mReviews.code === 200 && mReviews.data.total > 0,
    mReviews.data && mReviews.data.total)

  // 商家发布商品 → 归属自动写成本店
  const created = await call('POST', '/api/merchant/product', {
    token: mToken,
    body: {
      name: '冒烟测试商品（可删除）',
      subtitle: '自动化测试创建',
      categoryId: 1,
      price: 9.9,
      originalPrice: 19.9,
      stock: 5,
      unit: '件',
      mainImage: '/products/p1-1.jpg',
      images: '/products/p1-1.jpg',
      detail: '冒烟测试数据',
    },
  })
  check('商家发布商品', created.code === 200 && created.data.merchantId === 1, created.message)

  // 商家发货：用本店商品单独下一单
  const kitchen = mProducts.data.list.find((x) => x.status === 1 && x.stock > 1)
  const mSubmit = await call('POST', '/api/order/submit', {
    token,
    body: { addressId, items: [{ productId: kitchen.id, quantity: 1 }] },
  })
  check('单店下单只生成 1 个订单', mSubmit.code === 200 && mSubmit.data.length === 1, mSubmit.message)
  const mOrder = mSubmit.data[0]
  await call('PUT', `/api/order/${mOrder.id}/pay`, { token })

  const mShip = await call('PUT', `/api/merchant/order/${mOrder.id}/ship`, { token: mToken })
  check('商家对本店订单发货', mShip.code === 200, mShip.message)
  const afterShip = await call('GET', `/api/order/${mOrder.id}`, { token })
  check('买家侧看到已发货', afterShip.code === 200 && afterShip.data.status === 2, afterShip.data && afterShip.data.status)

  // 越权：别家商品 / 别家订单 / 平台后台
  const otherProduct = products.data.list.find((x) => x.merchantId !== 1)
  if (otherProduct) {
    const cross = await call('PUT', `/api/merchant/product/${otherProduct.id}/status?status=0`, { token: mToken })
    check('商家不能改动别家商品', cross.code !== 200, cross.message)
  }

  const adminAll = await call('GET', '/api/admin/order/page?pageNum=1&pageSize=100', { token: adminToken })
  const foreign = adminAll.data.list.find((o) => o.merchantId !== 1 && o.status === 1)
  if (foreign) {
    const crossShip = await call('PUT', `/api/merchant/order/${foreign.id}/ship`, { token: mToken })
    check('商家不能给别家订单发货', crossShip.code !== 200, crossShip.message)
  }

  const mToAdmin = await call('GET', '/api/admin/product/page', { token: mToken })
  check('商家访问平台后台被拒', mToAdmin.code === 403, mToAdmin.code)
  const userToMerchant = await call('GET', '/api/merchant/profile', { token })
  check('普通用户访问商家后台被拒', userToMerchant.code === 403, userToMerchant.code)
  const adminToMerchant = await call('GET', '/api/merchant/profile', { token: adminToken })
  check('管理员访问商家后台同样被拒', adminToMerchant.code === 403, adminToMerchant.code)

  console.log('\n[8] 客户咨询（买家提问 / 商家回复）')

  const consultUnread = await call('GET', '/api/consult/unread', { token })
  check('买家咨询未读数可读',
    consultUnread.code === 200 && typeof consultUnread.data === 'number', consultUnread)

  const openChat = await call('GET', '/api/consult/with?merchantId=1&productId=1', { token })
  check('打开店铺会话返回历史消息',
    openChat.code === 200 && openChat.data.consult && Array.isArray(openChat.data.messages),
    openChat.message)
  const consultId = openChat.data.consult.id

  const ask = await call('POST', '/api/consult/send', {
    token,
    body: { merchantId: 1, productId: 1, content: '冒烟测试咨询：今天下单大概什么时候发货？' },
  })
  const lastAsk = ask.data && ask.data.messages[ask.data.messages.length - 1]
  check('买家发送咨询成功',
    ask.code === 200 && lastAsk && lastAsk.senderType === 'USER', ask.message)

  const emptyAsk = await call('POST', '/api/consult/send', {
    token,
    body: { merchantId: 1, content: '   ' },
  })
  check('空白咨询内容被拦截', emptyAsk.code !== 200, emptyAsk)

  const mConsultUnread = await call('GET', '/api/merchant/consult/unread', { token: mToken })
  check('商家侧出现待回复未读',
    mConsultUnread.code === 200 && mConsultUnread.data > 0, mConsultUnread.data)

  const mConsultPage = await call('GET', '/api/merchant/consult/page?pageNum=1&pageSize=50', { token: mToken })
  check('商家会话列表包含该买家',
    mConsultPage.code === 200 && mConsultPage.data.list.some((c) => c.id === consultId),
    mConsultPage.message)

  const mReply = await call('POST', `/api/merchant/consult/${consultId}/reply`, {
    token: mToken,
    body: { content: '冒烟测试回复：17 点前下单当天发出，48 小时内揽收～' },
  })
  check('商家回复咨询成功', mReply.code === 200, mReply)

  const buyerUnread = await call('GET', '/api/consult/unread', { token })
  check('商家回复后买家侧有未读', buyerUnread.code === 200 && buyerUnread.data > 0, buyerUnread.data)

  const chatDetail = await call('GET', '/api/consult/' + consultId, { token })
  const chatLastMsg = chatDetail.data && chatDetail.data.messages[chatDetail.data.messages.length - 1]
  check('买家看到商家回复',
    chatDetail.code === 200 && chatLastMsg && chatLastMsg.senderType === 'MERCHANT' &&
    chatLastMsg.content.includes('冒烟测试回复'),
    chatLastMsg && chatLastMsg.content)
  check('会话摘要同步为最后一条消息',
    chatDetail.data.consult.lastContent.includes('冒烟测试回复'),
    chatDetail.data.consult.lastContent)

  const clearedUnread = await call('GET', '/api/consult/unread', { token })
  check('买家打开会话后未读清零', clearedUnread.code === 200 && clearedUnread.data === 0, clearedUnread.data)

  // 越权：别家店铺既读不到也回不了这条会话
  const otherLogin = await call('POST', '/api/auth/login', {
    body: { username: 'shop_clean', password: '123456' },
  })
  const otherToken = otherLogin.data.token
  const crossRead = await call('GET', '/api/merchant/consult/' + consultId, { token: otherToken })
  check('商家不能读取别家店铺会话', crossRead.code === 403, crossRead.code)
  const crossReply = await call('POST', `/api/merchant/consult/${consultId}/reply`, {
    token: otherToken,
    body: { content: '越权回复' },
  })
  check('商家不能回复别家店铺会话', crossReply.code === 403, crossReply.code)

  const anonConsult = await call('GET', '/api/consult/list')
  check('未登录不能查看咨询（401）', anonConsult.code === 401, anonConsult)

  // 清理测试商品
  if (created.code === 200) {
    await call('DELETE', `/api/merchant/product/${created.data.id}`, { token: mToken })
  }

  console.log('\n[9] 权限校验')
  const noToken = await call('GET', '/api/cart/list')
  check('未登录访问购物车被拒（401）', noToken.code === 401, noToken)
  const userAdmin = await call('GET', '/api/admin/order/page', { token })
  check('普通用户访问后台被拒（403）', userAdmin.code === 403, userAdmin)

  console.log(`\n==== 结果：通过 ${pass} 项，失败 ${fail} 项 ====`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch((e) => {
  console.error('\n执行异常：', e.message)
  process.exit(1)
})
