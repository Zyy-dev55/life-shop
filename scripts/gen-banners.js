/**
 * 生成首页轮播 Banner（SVG，商品图以 base64 内嵌，避免外链失效/水印）
 * 运行：node scripts/gen-banners.js
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.join(__dirname, '..')
const PRODUCT_DIR = path.join(ROOT, 'frontend', 'public', 'products')
const OUT_DIR = path.join(ROOT, 'frontend', 'public', 'banners')

const SLIDES = [
  {
    id: 1,
    product: 'p1-1.jpg',
    eyebrow: '厨房焕新季',
    title: ['一口好锅，', '把家常菜做得更好吃'],
    sub: '麦饭石不粘 · 少油也香 · 满 99 元包邮',
    cta: '去看看厨房好物',
    from: '#FFF6EC',
    to: '#FFDCC0',
    accent: '#E4633A',
  },
  {
    id: 2,
    product: 'p7-1.jpg',
    eyebrow: '干净的家',
    title: ['每周 20 分钟，', '把家擦得亮亮的'],
    sub: '清洁除菌专场 · 拖把 / 清洁剂 / 洗衣机槽片',
    cta: '一键囤清洁好物',
    from: '#F2FBF6',
    to: '#CDEBD9',
    accent: '#2F7A5A',
  },
  {
    id: 3,
    product: 'p12-1.jpg',
    eyebrow: '小家大收纳',
    title: ['换季不用愁，', '被褥衣物一压就省 70%'],
    sub: '真空压缩袋 8 件套 · 含手泵 · 防潮防虫',
    cta: '整理我的衣柜',
    from: '#F4F7FF',
    to: '#D6E0F7',
    accent: '#3C5A9A',
  },
]

function dataUri(file) {
  const p = path.join(PRODUCT_DIR, file)
  const buf = fs.readFileSync(p)
  const mime = buf[0] === 0x89 ? 'image/png' : 'image/jpeg'
  return `data:${mime};base64,${buf.toString('base64')}`
}

function esc(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function build(s) {
  const W = 1200
  const H = 420
  const img = dataUri(s.product)
  const titleLines = s.title
    .map(
      (line, i) =>
        `<text x="88" y="${168 + i * 58}" font-family="'PingFang SC','Microsoft YaHei',sans-serif" font-size="46" font-weight="700" fill="#2C2118">${esc(line)}</text>`
    )
    .join('\n  ')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${s.from}"/>
      <stop offset="100%" stop-color="${s.to}"/>
    </linearGradient>
    <linearGradient id="card" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95"/>
      <stop offset="100%" stop-color="#ffffff" stop-opacity="0.75"/>
    </linearGradient>
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="18" stdDeviation="22" flood-color="#8a5a34" flood-opacity="0.22"/>
    </filter>
    <clipPath id="round">
      <rect x="742" y="58" width="330" height="304" rx="30"/>
    </clipPath>
  </defs>

  <rect width="${W}" height="${H}" fill="url(#bg)"/>
  <circle cx="1080" cy="40" r="190" fill="#ffffff" opacity="0.35"/>
  <circle cx="700" cy="400" r="150" fill="#ffffff" opacity="0.28"/>
  <circle cx="620" cy="70" r="26" fill="${s.accent}" opacity="0.18"/>

  <g filter="url(#shadow)">
    <rect x="730" y="44" width="354" height="332" rx="42" fill="url(#card)"/>
  </g>
  <g clip-path="url(#round)">
    <image href="${img}" x="742" y="58" width="330" height="304" preserveAspectRatio="xMidYMid slice"/>
  </g>

  <g>
    <rect x="88" y="80" width="${s.eyebrow.length * 22 + 40}" height="40" rx="20" fill="${s.accent}" opacity="0.12"/>
    <text x="${88 + 20}" y="107" font-family="'PingFang SC','Microsoft YaHei',sans-serif" font-size="20" font-weight="600" fill="${s.accent}">${esc(s.eyebrow)}</text>
  </g>

  ${titleLines}

  <text x="88" y="${168 + s.title.length * 58 + 14}" font-family="'PingFang SC','Microsoft YaHei',sans-serif" font-size="24" fill="#7A6A5C">${esc(s.sub)}</text>

  <g>
    <rect x="88" y="${H - 96}" width="230" height="52" rx="26" fill="${s.accent}"/>
    <text x="${88 + 115}" y="${H - 62}" text-anchor="middle" font-family="'PingFang SC','Microsoft YaHei',sans-serif" font-size="22" font-weight="600" fill="#ffffff">${esc(s.cta)}</text>
  </g>
</svg>
`
}

fs.mkdirSync(OUT_DIR, { recursive: true })
// 旧的水印图清掉
for (const f of fs.readdirSync(OUT_DIR)) {
  if (/^b\d+\.(jpg|png|jpeg|webp)$/i.test(f)) fs.unlinkSync(path.join(OUT_DIR, f))
}
for (const s of SLIDES) {
  const svg = build(s)
  fs.writeFileSync(path.join(OUT_DIR, `b${s.id}.svg`), svg)
  console.log(`b${s.id}.svg  ${Math.round(svg.length / 1024)}KB`)
}
console.log('完成')
