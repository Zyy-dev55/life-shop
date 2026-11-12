/**
 * 种子数据生成器
 * 一次生成三份文件，保证 MySQL 正式脚本与 H2 演示脚本数据完全一致：
 *   1. sql/life_shop.sql                                 —— MySQL 8.0 正式脚本
 *   2. backend/src/main/resources/db/schema-h2.sql       —— H2 演示库建表
 *   3. backend/src/main/resources/db/data-h2.sql         —— H2 演示库数据
 *
 * 运行：node scripts/gen-seed.js
 */
const fs = require('fs')
const path = require('path')
const crypto = require('crypto')

const ROOT = path.join(__dirname, '..')
const SALT = 'LifeShop@2026'

function md5(s) {
  return crypto.createHash('md5').update(s + SALT).digest('hex')
}
function q(v) {
  if (v === null || v === undefined) return 'NULL'
  if (typeof v === 'number') return String(v)
  return "'" + String(v).replace(/'/g, "''") + "'"
}
function money(v) {
  return Number(v).toFixed(2)
}

// ---------------------------------------------------------------- 基础数据

const MAX_DAY = 6 // 演示数据都落在最近 7 天内，看板图表有内容

const USERS = [
  { id: 1, username: 'admin', password: 'admin123', nickname: '商城管理员', phone: '13800000000', gender: 1, role: 'ADMIN', avatar: '' },
  { id: 2, username: 'zhangsan', password: '123456', nickname: '张三', phone: '13900000001', gender: 1, role: 'USER', avatar: '' },
  { id: 3, username: 'lisi', password: '123456', nickname: '李四', phone: '13900000002', gender: 0, role: 'USER', avatar: '' },
  { id: 4, username: 'wangwu', password: '123456', nickname: '王五', phone: '13900000003', gender: 1, role: 'USER', avatar: '' },
  { id: 5, username: 'zhaoliu', password: '123456', nickname: '赵六', phone: '13900000004', gender: 0, role: 'USER', avatar: '' },
]

// 商品与分类统一来自 scripts/products.js（抓图脚本也用同一份）
const { CATEGORIES, PRODUCTS, MERCHANTS, merchantIdOfCategory } = require('./products')

// 商家账号（id 6-11）与 MERCHANTS 里的 userId 一一对应
const MERCHANT_USERS = MERCHANTS.map((m) => ({
  id: m.userId,
  username: m.username,
  password: m.password,
  nickname: m.contact + '（' + m.name + '）',
  phone: m.phone,
  gender: 1,
  role: 'MERCHANT',
  avatar: '',
}))

USERS.push(...MERCHANT_USERS)

// 抓图脚本会写 manifest.json，记录每个商品实际存下来的文件名（含扩展名，可能是 png）
const MANIFEST_PATH = path.join(ROOT, 'frontend', 'public', 'products', 'manifest.json')
const MANIFEST = fs.existsSync(MANIFEST_PATH)
  ? JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf-8'))
  : {}

// 每个商品 3 张图，第 1 张为主图
function imagesOf(id) {
  const real = MANIFEST[id] || MANIFEST[String(id)]
  if (real && real.length) return real
  return [`/products/p${id}-1.jpg`, `/products/p${id}-2.jpg`, `/products/p${id}-3.jpg`]
}

const BANNERS = [
  { id: 1, title: '厨房焕新季 · 好锅把家常菜做得更好吃', image: '/banners/b1.svg', link: '/list?categoryId=1', sort: 1 },
  { id: 2, title: '干净的家 · 每周 20 分钟清洁除菌', image: '/banners/b2.svg', link: '/list?categoryId=2', sort: 2 },
  { id: 3, title: '小家大收纳 · 换季被褥省 70% 空间', image: '/banners/b3.svg', link: '/list?categoryId=3', sort: 3 },
]

const ADDRESSES = [
  { id: 1, userId: 2, receiver: '张三', phone: '13900000001', province: '浙江省', city: '杭州市', district: '西湖区', detail: '文三路 100 号 3 幢 502 室', isDefault: 1 },
  { id: 2, userId: 2, receiver: '张三（公司）', phone: '13900000001', province: '浙江省', city: '杭州市', district: '滨江区', detail: '江南大道 588 号 A 座 12 楼', isDefault: 0 },
  { id: 3, userId: 3, receiver: '李四', phone: '13900000002', province: '江苏省', city: '南京市', district: '鼓楼区', detail: '中山北路 66 号 2 单元 801', isDefault: 1 },
  { id: 4, userId: 4, receiver: '王五', phone: '13900000003', province: '广东省', city: '深圳市', district: '南山区', detail: '科技园南路 18 号 5 栋 903', isDefault: 1 },
  { id: 5, userId: 5, receiver: '赵六', phone: '13900000004', province: '四川省', city: '成都市', district: '武侯区', detail: '天府大道中段 199 号 2 单元 601', isDefault: 1 },
]

// 订单：daysAgo 用于生成相对时间，让看板图表有近 7 天数据
const ORDERS = [
  {
    id: 1, userId: 2, status: 3, daysAgo: 6, addressId: 1,
    items: [{ productId: 1, quantity: 1 }, { productId: 14, quantity: 1 }],
  },
  {
    id: 2, userId: 2, status: 3, daysAgo: 5, addressId: 1,
    items: [{ productId: 11, quantity: 1 }, { productId: 17, quantity: 1 }],
  },
  {
    id: 3, userId: 2, status: 2, daysAgo: 2, addressId: 2,
    items: [{ productId: 7, quantity: 1 }, { productId: 12, quantity: 1 }],
  },
  {
    id: 4, userId: 2, status: 0, daysAgo: 1, addressId: 1,
    items: [{ productId: 5, quantity: 2 }, { productId: 8, quantity: 1 }],
  },
  {
    id: 5, userId: 3, status: 3, daysAgo: 5, addressId: 3,
    items: [{ productId: 4, quantity: 1 }, { productId: 2, quantity: 1 }],
  },
  {
    id: 6, userId: 4, status: 3, daysAgo: 4, addressId: 4,
    items: [{ productId: 18, quantity: 1 }],
  },
  {
    id: 7, userId: 4, status: 3, daysAgo: 3, addressId: 4,
    items: [{ productId: 16, quantity: 1 }, { productId: 10, quantity: 2 }],
  },
  {
    id: 8, userId: 5, status: 3, daysAgo: 3, addressId: 5,
    items: [{ productId: 13, quantity: 1 }, { productId: 15, quantity: 1 }],
  },
  {
    id: 9, userId: 3, status: 1, daysAgo: 0, addressId: 3,
    items: [{ productId: 3, quantity: 2 }, { productId: 9, quantity: 1 }],
  },
  {
    id: 10, userId: 5, status: 3, daysAgo: 2, addressId: 5,
    items: [{ productId: 6, quantity: 1 }],
  },
  // 这笔留给演示账号「待评价」，登录后个人中心就能看到
  {
    id: 11, userId: 2, status: 3, daysAgo: 1, addressId: 1,
    items: [{ productId: 20, quantity: 1 }, { productId: 24, quantity: 1 }],
  },
]

// 评价：只对已完成订单（status=3）的明细分评价
const REVIEWS = [
  { orderId: 1, productId: 1, rating: 5, content: '锅很沉实，不粘效果确实好，煎蛋基本不用放油。29cm 口径一家三口刚好，洗锅也方便。', daysAgo: 4 },
  { orderId: 1, productId: 14, rating: 5, content: '浴巾厚实，吸水快，洗完澡一裹就干了。尺寸够大，成年男生也不嫌小。', daysAgo: 4 },
  { orderId: 2, productId: 11, rating: 4, content: '纸确实厚，湿水不破。就是箱子里有几包压得有点扁，不影响使用。', daysAgo: 3 },
  { orderId: 2, productId: 17, rating: 5, content: '乳胶枕回弹很好，睡了三天脖子明显舒服了，透气孔设计夏天不闷。', daysAgo: 3 },
  { orderId: 5, productId: 4, rating: 4, content: '餐具颜值在线，釉面很亮。碗比想象中深一些，装汤挺合适。包装也扎实。', daysAgo: 3 },
  { orderId: 5, productId: 2, rating: 5, content: '保温效果超出预期，早上装的开水，下午还是烫的。弹盖单手能开，开车喝水方便。', daysAgo: 3 },
  { orderId: 6, productId: 18, rating: 5, content: '四件套手感细腻，不闷，洗了一次也没掉色缩水。颜色和图片一致。', daysAgo: 2 },
  { orderId: 7, productId: 16, rating: 3, content: '杯子哑光质感不错，但一只杯口有轻微瑕疵，联系客服补发了一个，态度还行。', daysAgo: 2 },
  { orderId: 7, productId: 10, rating: 5, content: '洗脸巾比之前买的厚，湿水搓都不破。抽取口设计好用，一次一张不黏连。', daysAgo: 2 },
  { orderId: 8, productId: 13, rating: 5, content: '毛巾很软，吸水也好，三条装正好分开用。就是深色那条掉了一点点浮毛，洗一次就好了。', daysAgo: 1 },
  { orderId: 8, productId: 15, rating: 4, content: '拖鞋轻，防滑还行，洗澡后地上有水也不打滑。建议大一码买，脚宽的话刚好。', daysAgo: 1 },
  { orderId: 10, productId: 6, rating: 5, content: '硅胶铲硬度刚好，炒菜不划锅，收纳桶放在台面上很整齐。', daysAgo: 1 },
]


// ---------------- 自动补齐评价（让每个商品都有真实评价与评分）----------------
const REVIEW_POOL = [
  '用了两周，做工比预期好，细节处理得挺干净，包装也严实，没有磕碰。',
  '发货很快，第二天就到了。东西和图片一致，日常用完全够，性价比可以。',
  '第二次回购了，家里人都说好用，这次趁活动多囤了一些。',
  '整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。',
  '收到就拆开试了，手感不错，用完再追评。客服回复也挺快。',
  '性价比很高，比超市便宜，质量没打折扣，推荐。',
  '尺寸比想象中大一些，先量好再买，做工没得挑。',
  '用了几天，没什么异味，清洗也方便，属于会回购的类型。',
  '物流稍微慢了一天，商品本身没问题，包装完整，用着顺手。',
  '买给爸妈用的，他们说很方便，操作不复杂，挺适合老人。',
]

const ratedProductIds = new Set(REVIEWS.map((r) => r.productId))
let autoOrderId = ORDERS.length + 1
const AUTO_ORDERS = []
const AUTO_REVIEWS = []
const USER_ADDRESS = { 2: 1, 3: 3, 4: 4, 5: 5 }

PRODUCTS.forEach((p, index) => {
  if (ratedProductIds.has(p.id)) return
  // 卖得多的商品多给一条评价，看起来更真实
  const count = p.sales >= 1000 ? 2 : 1
  for (let k = 0; k < count; k++) {
    const userId = [2, 3, 4, 5][(p.id + k) % 4]
    const orderId = autoOrderId++
    const daysAgo = 1 + ((p.id + k) % 6)
    AUTO_ORDERS.push({
      id: orderId,
      userId,
      status: 3,
      daysAgo,
      addressId: USER_ADDRESS[userId],
      items: [{ productId: p.id, quantity: 1 }],
    })
    AUTO_REVIEWS.push({
      orderId,
      productId: p.id,
      rating: [5, 5, 4, 5, 4, 3][(p.id + k) % 6],
      content: REVIEW_POOL[(index + k) % REVIEW_POOL.length],
      daysAgo: Math.max(daysAgo - 1, 0),
    })
  }
})
ORDERS.push(...AUTO_ORDERS)
REVIEWS.push(...AUTO_REVIEWS)

const REVIEW_REPLY = {
  '3-16': { reply: '非常抱歉给您带来不好的体验，已为您优先补发，后续如有任何问题可直接联系在线客服～', daysAgo: 1 },
  '2-17': { reply: '感谢认可！枕头建议先在通风处晾置半天再使用，回弹会更佳。', daysAgo: 2 },
}

// 咨询会话（买家 ↔ 店铺）。h = 该条消息距今多少小时
// readAll: true 表示双方都已读；否则按「最后一条由谁发出」推导对端未读数
const CONSULTS = [
  {
    id: 1,
    userId: 2,
    merchantId: 1,
    productId: 1,
    readAll: true,
    messages: [
      { from: 'USER', h: 50, text: '你好，这款麦饭石炒锅能用电磁炉吗？' },
      { from: 'MERCHANT', h: 49, text: '可以的，锅底是复合导磁片，电磁炉、燃气灶、电陶炉都能用。' },
      { from: 'USER', h: 47, text: '30cm 的直径，三口之家炒菜够用吗？' },
      { from: 'MERCHANT', h: 46, text: '够用，锅深 8.5cm，炒三四个人份的菜不会溢出来。' },
    ],
  },
  {
    id: 2,
    userId: 3,
    merchantId: 1,
    productId: 22,
    messages: [
      { from: 'USER', h: 30, text: '蒸锅收到啦，第一次用需要开锅吗？' },
      { from: 'MERCHANT', h: 29, text: '不锈钢锅用温水加一点洗洁精洗净擦干就行，第一次蒸东西前先蒸 10 分钟清水去味。' },
      { from: 'USER', h: 28, text: '好的，谢谢！' },
    ],
  },
  {
    id: 3,
    userId: 2,
    merchantId: 2,
    productId: 5,
    readAll: true,
    messages: [
      { from: 'USER', h: 70, text: '这个清洁剂对不锈钢灶台会有腐蚀吗？' },
      { from: 'MERCHANT', h: 69, text: '不会，配方中性偏碱，不锈钢、瓷砖、玻璃都能用，避开铝制品就行。' },
      { from: 'USER', h: 68, text: '明白了，那我先买两瓶试试。' },
    ],
  },
  {
    id: 4,
    userId: 4,
    merchantId: 3,
    productId: 30,
    messages: [
      { from: 'USER', h: 6, text: '保鲜盒能直接进微波炉吗？' },
      { from: 'MERCHANT', h: 5, text: '可以，盒身耐 -20℃~120℃，微波加热记得把盖子的四个卡扣打开。' },
      { from: 'USER', h: 4, text: '从冷冻室拿出来直接微波会不会裂？' },
      { from: 'MERCHANT', h: 3, text: '建议先解冻 5 分钟或者用中低火，骤冷骤热对任何塑料都不友好～' },
      { from: 'MERCHANT', h: 2, text: '另外我们随盒送了 2 个冰袋，冷冻更均匀。' },
    ],
  },
  {
    id: 5,
    userId: 5,
    merchantId: 4,
    productId: 15,
    messages: [
      { from: 'USER', h: 26, text: '拖鞋平时穿 42 码，拍 42 还是 43？' },
      { from: 'MERCHANT', h: 25, text: '按平时运动鞋的码数拍就行，脚背高或者要穿袜子可以大一码。' },
    ],
  },
  {
    id: 6,
    userId: 3,
    merchantId: 5,
    productId: 40,
    readAll: true,
    messages: [
      { from: 'USER', h: 12, text: '盖毯会起球吗？家里有猫。' },
      { from: 'MERCHANT', h: 11, text: '针织面料做过抗起球处理，正常使用不会大面积起球；被猫爪勾到的话把浮线剪掉就好。' },
      { from: 'USER', h: 10, text: '能机洗吗？' },
      { from: 'MERCHANT', h: 9, text: '可以，装洗衣袋冷水轻柔档，平铺晾干，别拧。' },
    ],
  },
  {
    id: 7,
    userId: 2,
    merchantId: 6,
    productId: null,
    readAll: true,
    messages: [
      { from: 'USER', h: 100, text: '你们家有 20cm 宽的衣架吗？肩宽的衣服挂着容易变形。' },
      { from: 'MERCHANT', h: 99, text: '有的，在「家居日用」分类里搜「加宽衣架」就能看到，实体肩宽 20cm。' },
    ],
  },
  {
    id: 8,
    userId: 4,
    merchantId: 1,
    productId: 3,
    messages: [
      { from: 'USER', h: 1, text: '洗碗布什么时候发货？' },
      { from: 'MERCHANT', h: 0, text: '今天就发出，晚上物流单号会同步到订单里。' },
      { from: 'USER', h: 0, text: '好的，麻烦尽快～' },
    ],
  },
]

// ---------------------------------------------------------------- 计算派生值

const productById = new Map(PRODUCTS.map((p) => [p.id, p]))

// 按店铺拆单：一次结算车里混了多个店铺的商品时，会拆成多笔订单（和真实电商一致），
// 商家后台只能看到属于自己店铺的那一笔。
function splitByMerchant(list) {
  const result = []
  let seq = 0
  for (const raw of list) {
    const addr = ADDRESSES.find((a) => a.id === raw.addressId)
    const groups = new Map()
    for (const it of raw.items) {
      const product = productById.get(it.productId)
      const mid = merchantIdOfCategory(product.categoryId)
      if (!groups.has(mid)) groups.set(mid, [])
      groups.get(mid).push(it)
    }
    for (const [merchantId, items] of groups) {
      const id = ++seq
      const total = items.reduce((s, it) => s + productById.get(it.productId).price * it.quantity, 0)
      const freight = total >= 99 ? 0 : 10
      result.push({
        ...raw,
        id,
        srcId: raw.id,
        merchantId,
        merchantName: MERCHANTS.find((m) => m.id === merchantId).name,
        items,
        orderNo: 'LS202610' + String(id).padStart(2, '0') + '1000' + String(1000 + id),
        address: addr,
        total: Number(total.toFixed(2)),
        freight,
        payAmount: Number((total + freight).toFixed(2)),
      })
    }
  }
  return result
}

const orders = splitByMerchant(ORDERS)

/** 评价关联到拆单后的订单（原订单号 + 商品 唯一定位） */
function orderOfReview(review) {
  return (
    orders.find((o) => o.srcId === review.orderId && o.items.some((it) => it.productId === review.productId)) ||
    orders.find((o) => o.srcId === review.orderId)
  )
}

// 商品销量/评价聚合：销量基准 + 演示订单里的数量，评价数按真实评价统计
const agg = new Map()
for (const r of REVIEWS) {
  const a = agg.get(r.productId) || { count: 0, sum: 0 }
  a.count += 1
  a.sum += r.rating
  agg.set(r.productId, a)
}

// ---------------------------------------------------------------- SQL 片段

// MySQL 的 KEY / UNIQUE KEY 写法 H2 不支持，演示库直接去掉（索引只影响性能，不影响功能）
function stripMysqlKeys(body) {
  const lines = body.split('\n').filter((l) => !/^\s*(UNIQUE\s+)?KEY\s+/i.test(l))
  let last = lines.length - 1
  while (last > 0 && lines[last].trim() === '') last--
  lines[last] = lines[last].replace(/,\s*$/, '')
  return lines.join('\n')
}

const D = {
  mysql: {
    now: 'NOW()',
    ago: (n) => (n === 0 ? 'NOW()' : `DATE_SUB(NOW(), INTERVAL ${n} DAY)`),
    hoursAgo: (n) => (n === 0 ? 'NOW()' : `DATE_SUB(NOW(), INTERVAL ${n} HOUR)`),
    autoInc: 'BIGINT NOT NULL AUTO_INCREMENT',
    table: (name, body, extra) =>
      `CREATE TABLE \`${name}\` (\n${body}\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci${extra || ''};`,
    text: 'TEXT',
  },
  h2: {
    now: 'CURRENT_TIMESTAMP',
    ago: (n) => (n === 0 ? 'CURRENT_TIMESTAMP' : `DATEADD('DAY', -${n}, CURRENT_TIMESTAMP)`),
    hoursAgo: (n) => (n === 0 ? 'CURRENT_TIMESTAMP' : `DATEADD('HOUR', -${n}, CURRENT_TIMESTAMP)`),
    autoInc: 'BIGINT AUTO_INCREMENT',
    table: (name, body) => `CREATE TABLE ${name} (\n${stripMysqlKeys(body)}\n);`,
    text: 'CLOB',
  },
}

function buildSchema(d) {
  const out = []
  if (d === D.mysql) out.push('SET NAMES utf8mb4;')
  out.push(d.table('t_user', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  username VARCHAR(50) NOT NULL,',
    '  password VARCHAR(64) NOT NULL,',
    '  nickname VARCHAR(50),',
    '  phone VARCHAR(20),',
    '  avatar VARCHAR(255),',
    '  gender TINYINT DEFAULT 1,',
    "  role VARCHAR(20) DEFAULT 'USER',",
    '  status TINYINT DEFAULT 1,',
    '  create_time DATETIME,',
    '  update_time DATETIME,',
    '  UNIQUE KEY uk_username (username)',
  ].join('\n')))
  out.push(d.table('t_category', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  name VARCHAR(50) NOT NULL,',
    '  icon VARCHAR(50),',
    '  sort INT DEFAULT 0,',
    '  status TINYINT DEFAULT 1,',
    '  create_time DATETIME',
  ].join('\n')))
  out.push(d.table('t_merchant', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  user_id BIGINT NOT NULL,',
    '  name VARCHAR(64) NOT NULL,',
    '  logo VARCHAR(255),',
    '  description VARCHAR(500),',
    '  contact VARCHAR(32),',
    '  phone VARCHAR(20),',
    '  address VARCHAR(255),',
    '  status TINYINT DEFAULT 1,',
    '  create_time DATETIME,',
    '  update_time DATETIME,',
    '  UNIQUE KEY uk_user (user_id)',
  ].join('\n')))
  out.push(d.table('t_product', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  merchant_id BIGINT NOT NULL,',
    '  category_id BIGINT NOT NULL,',
    '  name VARCHAR(100) NOT NULL,',
    '  subtitle VARCHAR(200),',
    '  main_image VARCHAR(255),',
    '  images VARCHAR(1000),',
    '  detail ' + d.text + ',',
    '  price DECIMAL(10,2) NOT NULL,',
    '  original_price DECIMAL(10,2),',
    '  stock INT DEFAULT 0,',
    '  sales INT DEFAULT 0,',
    '  unit VARCHAR(20) DEFAULT \'件\',',
    '  rating DECIMAL(2,1) DEFAULT 5.0,',
    '  review_count INT DEFAULT 0,',
    '  status TINYINT DEFAULT 1,',
    '  create_time DATETIME,',
    '  update_time DATETIME,',
    '  KEY idx_category (category_id),',
    '  KEY idx_merchant (merchant_id)',
  ].join('\n')))
  out.push(d.table('t_cart_item', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  user_id BIGINT NOT NULL,',
    '  product_id BIGINT NOT NULL,',
    '  quantity INT DEFAULT 1,',
    '  checked TINYINT DEFAULT 1,',
    '  create_time DATETIME,',
    '  KEY idx_user (user_id)',
  ].join('\n')))
  out.push(d.table('t_address', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  user_id BIGINT NOT NULL,',
    '  receiver VARCHAR(50) NOT NULL,',
    '  phone VARCHAR(20) NOT NULL,',
    '  province VARCHAR(50),',
    '  city VARCHAR(50),',
    '  district VARCHAR(50),',
    '  detail VARCHAR(255),',
    '  is_default TINYINT DEFAULT 0,',
    '  create_time DATETIME,',
    '  KEY idx_user (user_id)',
  ].join('\n')))
  out.push(d.table('t_order', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  order_no VARCHAR(32) NOT NULL,',
    '  user_id BIGINT NOT NULL,',
    '  merchant_id BIGINT NOT NULL,',
    '  merchant_name VARCHAR(64),',
    '  total_amount DECIMAL(10,2) NOT NULL,',
    '  freight DECIMAL(10,2) DEFAULT 0.00,',
    '  pay_amount DECIMAL(10,2) NOT NULL,',
    '  status TINYINT DEFAULT 0,',
    '  receiver VARCHAR(50),',
    '  phone VARCHAR(20),',
    '  address VARCHAR(255),',
    '  remark VARCHAR(255),',
    '  pay_time DATETIME,',
    '  ship_time DATETIME,',
    '  finish_time DATETIME,',
    '  create_time DATETIME,',
    '  update_time DATETIME,',
    '  UNIQUE KEY uk_order_no (order_no),',
    '  KEY idx_user (user_id)',
  ].join('\n')))
  out.push(d.table('t_order_item', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  order_id BIGINT NOT NULL,',
    '  order_no VARCHAR(32),',
    '  merchant_id BIGINT,',
    '  product_id BIGINT NOT NULL,',
    '  product_name VARCHAR(100),',
    '  product_image VARCHAR(255),',
    '  price DECIMAL(10,2),',
    '  quantity INT,',
    '  total_price DECIMAL(10,2),',
    '  KEY idx_order (order_id)',
  ].join('\n')))
  out.push(d.table('t_banner', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  title VARCHAR(100),',
    '  image VARCHAR(255),',
    '  link VARCHAR(255),',
    '  sort INT DEFAULT 0,',
    '  status TINYINT DEFAULT 1,',
    '  create_time DATETIME',
  ].join('\n')))
  out.push(d.table('t_review', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  user_id BIGINT NOT NULL,',
    '  user_name VARCHAR(50),',
    '  user_avatar VARCHAR(255),',
    '  order_id BIGINT NOT NULL,',
    '  order_no VARCHAR(32),',
    '  merchant_id BIGINT,',
    '  product_id BIGINT NOT NULL,',
    '  product_name VARCHAR(100),',
    '  product_image VARCHAR(255),',
    '  rating TINYINT DEFAULT 5,',
    '  content VARCHAR(1000),',
    '  images VARCHAR(1000),',
    '  anonymous TINYINT DEFAULT 0,',
    '  reply VARCHAR(500),',
    '  reply_time DATETIME,',
    '  create_time DATETIME,',
    '  UNIQUE KEY uk_order_product (order_id, product_id),',
    '  KEY idx_product (product_id)',
  ].join('\n')))
  out.push(d.table('t_consult', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  user_id BIGINT NOT NULL,',
    '  user_name VARCHAR(50),',
    '  user_avatar VARCHAR(255),',
    '  merchant_id BIGINT NOT NULL,',
    '  shop_name VARCHAR(64),',
    '  shop_logo VARCHAR(255),',
    '  product_id BIGINT,',
    '  product_name VARCHAR(100),',
    '  product_image VARCHAR(255),',
    '  last_content VARCHAR(500),',
    '  last_sender VARCHAR(16),',
    '  last_time DATETIME,',
    '  user_unread INT DEFAULT 0,',
    '  merchant_unread INT DEFAULT 0,',
    '  create_time DATETIME,',
    '  update_time DATETIME,',
    '  UNIQUE KEY uk_user_merchant (user_id, merchant_id),',
    '  KEY idx_merchant (merchant_id)',
  ].join('\n')))
  out.push(d.table('t_consult_message', [
    '  id ' + d.autoInc + ' PRIMARY KEY,',
    '  consult_id BIGINT NOT NULL,',
    '  sender_type VARCHAR(16) NOT NULL,',
    '  sender_id BIGINT,',
    '  sender_name VARCHAR(50),',
    '  content VARCHAR(1000),',
    '  create_time DATETIME,',
    '  KEY idx_consult (consult_id)',
  ].join('\n')))
  return out.join('\n\n')
}

function buildData(d) {
  const out = []
  const t = (n) => `${d.ago(n)}`

  // 用户
  out.push('-- 用户（密码加密规则：md5(明文 + LifeShop@2026)）')
  out.push(
    'INSERT INTO t_user (id,username,password,nickname,phone,avatar,gender,role,status,create_time,update_time) VALUES\n' +
    USERS.map((u) =>
      `(${u.id},${q(u.username)},${q(md5(u.password))},${q(u.nickname)},${q(u.phone)},${q(u.avatar)},${u.gender},${q(u.role)},1,${t(30)},${t(30)})`
    ).join(',\n') + ';'
  )

  // 分类
  out.push('\n-- 商品分类')
  out.push(
    'INSERT INTO t_category (id,name,icon,sort,status,create_time) VALUES\n' +
    CATEGORIES.map((c) => `(${c.id},${q(c.name)},${q(c.icon)},${c.sort},1,${t(30)})`).join(',\n') + ';'
  )

  // 商家（一个商家经营一个分类，账号见 README）
  out.push('\n-- 入驻商家（role = MERCHANT，登录后进入商家后台）')
  out.push(
    'INSERT INTO t_merchant (id,user_id,name,logo,description,contact,phone,address,status,create_time,update_time) VALUES\n' +
    MERCHANTS.map((m) =>
      `(${m.id},${m.userId},${q(m.name)},${q('')},${q(m.description)},${q(m.contact)},${q(m.phone)},${q(m.address)},1,${t(60)},${t(5)})`
    ).join(',\n') + ';'
  )

  // 商品
  out.push('\n-- 商品（主图为抓取到本地的真实商品图，merchant_id 标记所属店铺）')
  out.push(
    'INSERT INTO t_product (id,merchant_id,category_id,name,subtitle,main_image,images,detail,price,original_price,stock,sales,unit,rating,review_count,status,create_time,update_time) VALUES\n' +
    PRODUCTS.map((p) => {
      const imgs = imagesOf(p.id)
      const a = agg.get(p.id)
      const rating = a ? (a.sum / a.count).toFixed(1) : '5.0'
      const count = a ? a.count : 0
      return `(${p.id},${merchantIdOfCategory(p.categoryId)},${p.categoryId},${q(p.name)},${q(p.subtitle)},${q(imgs[0])},${q(imgs.join(','))},${q(p.detail)},${money(p.price)},${money(p.originalPrice)},${p.stock},${p.sales},${q(p.unit)},${rating},${count},1,${t(28)},${t(2)})`
    }).join(',\n') + ';'
  )

  // 轮播图
  out.push('\n-- 首页轮播')
  out.push(
    'INSERT INTO t_banner (id,title,image,link,sort,status,create_time) VALUES\n' +
    BANNERS.map((b) => `(${b.id},${q(b.title)},${q(b.image)},${q(b.link)},${b.sort},1,${t(20)})`).join(',\n') + ';'
  )

  // 地址
  out.push('\n-- 收货地址')
  out.push(
    'INSERT INTO t_address (id,user_id,receiver,phone,province,city,district,detail,is_default,create_time) VALUES\n' +
    ADDRESSES.map((a) => `(${a.id},${a.userId},${q(a.receiver)},${q(a.phone)},${q(a.province)},${q(a.city)},${q(a.district)},${q(a.detail)},${a.isDefault},${t(20)})`).join(',\n') + ';'
  )

  // 购物车：给张三放 3 件，登录后购物车不是空的
  const cart = [
    { userId: 2, productId: 2, quantity: 1 },
    { userId: 2, productId: 5, quantity: 2 },
    { userId: 2, productId: 9, quantity: 1 },
    { userId: 3, productId: 12, quantity: 1 },
  ]
  out.push('\n-- 购物车（演示用户 zhangsan 有 3 件商品）')
  out.push(
    'INSERT INTO t_cart_item (id,user_id,product_id,quantity,checked,create_time) VALUES\n' +
    cart.map((c, i) => `(${i + 1},${c.userId},${c.productId},${c.quantity},1,${t(1)})`).join(',\n') + ';'
  )

  // 订单
  out.push('\n-- 订单（按店铺拆单，覆盖待付款/待发货/已发货/已完成四种状态）')
  out.push(
    'INSERT INTO t_order (id,order_no,user_id,merchant_id,merchant_name,total_amount,freight,pay_amount,status,receiver,phone,address,remark,pay_time,ship_time,finish_time,create_time,update_time) VALUES\n' +
    orders.map((o) => {
      const unpaid = o.status === 0
      const payTime = unpaid ? 'NULL' : t(o.daysAgo)
      const shipTime = o.status >= 2 ? t(Math.max(o.daysAgo - 1, 0)) : 'NULL'
      const finishTime = o.status === 3 ? t(Math.max(o.daysAgo - 2, 0)) : 'NULL'
      const addr = `${o.address.province}${o.address.city}${o.address.district}${o.address.detail}`
      const remark = o.srcId === 4 ? "'请放门口，谢谢'" : 'NULL'
      return `(${o.id},${q(o.orderNo)},${o.userId},${o.merchantId},${q(o.merchantName)},${money(o.total)},${money(o.freight)},${money(o.payAmount)},${o.status},${q(o.address.receiver)},${q(o.address.phone)},${q(addr)},${remark},${payTime},${shipTime},${finishTime},${t(o.daysAgo)},${t(o.daysAgo)})`
    }).join(',\n') + ';'
  )

  // 订单明细
  const itemRows = []
  let itemId = 1
  for (const o of orders) {
    for (const it of o.items) {
      const p = productById.get(it.productId)
      const total = (p.price * it.quantity).toFixed(2)
      itemRows.push(`(${itemId++},${o.id},${q(o.orderNo)},${o.merchantId},${p.id},${q(p.name)},${q(imagesOf(p.id)[0])},${money(p.price)},${it.quantity},${total})`)
    }
  }
  out.push('\n-- 订单明细（保存下单时的商品快照）')
  out.push(
    'INSERT INTO t_order_item (id,order_id,order_no,merchant_id,product_id,product_name,product_image,price,quantity,total_price) VALUES\n' +
    itemRows.join(',\n') + ';'
  )

  // 评价
  out.push('\n-- 商品评价（订单 2 中的乳胶枕、订单 7 中的漱口杯在后台有商家回复）')
  out.push(
    'INSERT INTO t_review (id,user_id,user_name,user_avatar,order_id,order_no,merchant_id,product_id,product_name,product_image,rating,content,images,anonymous,reply,reply_time,create_time) VALUES\n' +
    REVIEWS.map((r, i) => {
      const o = orderOfReview(r)
      const u = USERS.find((x) => x.id === o.userId)
      const p = productById.get(r.productId)
      const key = r.orderId + '-' + r.productId
      const rp = REVIEW_REPLY[key]
      const reply = rp ? q(rp.reply) : 'NULL'
      const replyTime = rp ? t(rp.daysAgo) : 'NULL'
      // 部分评价带图
      const withImg = i % 3 === 0
      const imgs = withImg ? q(imagesOf(p.id)[1] + ',' + imagesOf(p.id)[2]) : 'NULL'
      return `(${i + 1},${u.id},${q(u.nickname)},${q(u.avatar || '')},${o.id},${q(o.orderNo)},${o.merchantId},${p.id},${q(p.name)},${q(imagesOf(p.id)[0])},${r.rating},${q(r.content)},${imgs},0,${reply},${replyTime},${t(r.daysAgo)})`
    }).join(',\n') + ';'
  )

  // 买家咨询商家：会话 + 消息明细
  const consultRows = []
  const msgRows = []
  let msgId = 1
  for (const c of CONSULTS) {
    const user = USERS.find((u) => u.id === c.userId)
    const merchant = MERCHANTS.find((m) => m.id === c.merchantId)
    const product = c.productId ? productById.get(c.productId) : null
    const last = c.messages[c.messages.length - 1]
    const first = c.messages[0]
    // 尾部连续同一发送方的条数，就是对方的未读数
    let tail = 0
    for (let i = c.messages.length - 1; i >= 0; i--) {
      if (c.messages[i].from === last.from) tail++
      else break
    }
    const userUnread = !c.readAll && last.from === 'MERCHANT' ? tail : 0
    const merchantUnread = !c.readAll && last.from === 'USER' ? tail : 0
    const lastContent = last.text.length > 60 ? last.text.slice(0, 60) + '…' : last.text
    consultRows.push(
      `(${c.id},${user.id},${q(user.nickname)},${q(user.avatar || '')},${merchant.id},${q(merchant.name)},${q('')},` +
      `${product ? product.id : 'NULL'},${product ? q(product.name) : 'NULL'},${product ? q(imagesOf(product.id)[0]) : 'NULL'},` +
      `${q(lastContent)},${q(last.from)},${d.hoursAgo(last.h)},${userUnread},${merchantUnread},${d.hoursAgo(first.h)},${d.hoursAgo(last.h)})`
    )
    for (const m of c.messages) {
      msgRows.push(
        `(${msgId++},${c.id},${q(m.from)},${m.from === 'USER' ? user.id : merchant.id},` +
        `${q(m.from === 'USER' ? user.nickname : merchant.name)},${q(m.text)},${d.hoursAgo(m.h)})`
      )
    }
  }
  out.push('\n-- 买家咨询店铺（一个买家在一个店铺只有一条会话）')
  out.push(
    'INSERT INTO t_consult (id,user_id,user_name,user_avatar,merchant_id,shop_name,shop_logo,product_id,product_name,product_image,last_content,last_sender,last_time,user_unread,merchant_unread,create_time,update_time) VALUES\n' +
    consultRows.join(',\n') + ';'
  )
  out.push('\n-- 咨询消息明细（USER 买家 / MERCHANT 商家）')
  out.push(
    'INSERT INTO t_consult_message (id,consult_id,sender_type,sender_id,sender_name,content,create_time) VALUES\n' +
    msgRows.join(',\n') + ';'
  )

  return out.join('\n\n')
}

// ---------------------------------------------------------------- 输出

const mysqlSchema = buildSchema(D.mysql)
const mysqlData = buildData(D.mysql)
const mysqlFull = [
  '-- ============================================================',
  '-- 悦家生活 · 生活用品电商系统  数据库初始化脚本',
  '-- MySQL 8.0 / utf8mb4  直接执行本文件即可完成建库建表与数据初始化',
  '-- 账号：admin / admin123（平台管理员）  zhangsan / 123456（演示买家）',
  '-- 商家：shop_kitchen / 123456（悦家厨房旗舰店）等 6 家，详见 README',
  '-- ============================================================',
  '',
  'DROP DATABASE IF EXISTS `life_shop`;',
  'CREATE DATABASE `life_shop` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;',
  'USE `life_shop`;',
  '',
  'SET FOREIGN_KEY_CHECKS = 0;',
  'DROP TABLE IF EXISTS `t_consult_message`;',
  'DROP TABLE IF EXISTS `t_consult`;',
  'DROP TABLE IF EXISTS `t_review`;',
  'DROP TABLE IF EXISTS `t_order_item`;',
  'DROP TABLE IF EXISTS `t_order`;',
  'DROP TABLE IF EXISTS `t_address`;',
  'DROP TABLE IF EXISTS `t_cart_item`;',
  'DROP TABLE IF EXISTS `t_product`;',
  'DROP TABLE IF EXISTS `t_merchant`;',
  'DROP TABLE IF EXISTS `t_category`;',
  'DROP TABLE IF EXISTS `t_banner`;',
  'DROP TABLE IF EXISTS `t_user`;',
  'SET FOREIGN_KEY_CHECKS = 1;',
  '',
  mysqlSchema,
  '',
  mysqlData,
  '',
].join('\n')

const h2Schema = [
  '-- 演示模式（H2 内存库）建表脚本，由 Spring Boot 启动时自动执行',
  buildSchema(D.h2),
  '',
].join('\n')

const h2Data = [
  '-- 演示模式初始数据',
  buildData(D.h2),
  '',
].join('\n')

fs.mkdirSync(path.join(ROOT, 'sql'), { recursive: true })
fs.mkdirSync(path.join(ROOT, 'backend/src/main/resources/db'), { recursive: true })
fs.writeFileSync(path.join(ROOT, 'sql/life_shop.sql'), mysqlFull)
fs.writeFileSync(path.join(ROOT, 'backend/src/main/resources/db/schema-h2.sql'), h2Schema)
fs.writeFileSync(path.join(ROOT, 'backend/src/main/resources/db/data-h2.sql'), h2Data)

console.log('已生成：')
console.log('  sql/life_shop.sql                              ', mysqlFull.length, '字符')
console.log('  backend/src/main/resources/db/schema-h2.sql    ', h2Schema.length, '字符')
console.log('  backend/src/main/resources/db/data-h2.sql      ', h2Data.length, '字符')
console.log(`商品 ${PRODUCTS.length} 个 / 分类 ${CATEGORIES.length} 个 / 店铺 ${MERCHANTS.length} 家 / 订单 ${orders.length} 笔 / 评价 ${REVIEWS.length} 条 / 咨询 ${CONSULTS.length} 组`)
