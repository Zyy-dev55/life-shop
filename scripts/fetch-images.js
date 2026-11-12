/**
 * 商品图片抓取脚本（仅用于本地演示）
 * 通过 Bing 图片搜索拿到真实电商商品图（alicdn / 360buyimg 等 CDN 直链），
 * 下载到 frontend/public/products/ 下，避免防盗链导致图片打不开。
 *
 * 运行：node scripts/fetch-images.js
 */
const fs = require('fs')
const path = require('path')
const https = require('https')
const http = require('http')

const ROOT = path.join(__dirname, '..')
const OUT_DIR = path.join(ROOT, 'frontend', 'public', 'products')

// 商品关键词统一来自 scripts/products.js
const { PRODUCTS: ALL_PRODUCTS } = require('./products')

const UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'

function request(url, opts = {}) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http
    const req = lib.get(
      url,
      {
        headers: {
          'User-Agent': UA,
          Referer: 'https://cn.bing.com/',
          'Accept-Language': 'zh-CN,zh;q=0.9',
          ...(opts.headers || {}),
        },
        timeout: 25000,
      },
      (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          res.resume()
          return resolve(request(new URL(res.headers.location, url).href, opts))
        }
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () =>
          resolve({ status: res.statusCode, headers: res.headers, body: Buffer.concat(chunks) })
        )
      }
    )
    req.on('timeout', () => req.destroy(new Error('timeout')))
    req.on('error', reject)
  })
}

function extOf(buf, contentType) {
  if (buf[0] === 0xff && buf[1] === 0xd8) return '.jpg'
  if (buf[0] === 0x89 && buf[1] === 0x50) return '.png'
  if (buf.slice(0, 4).toString() === 'RIFF' && buf.slice(8, 12).toString() === 'WEBP') return '.webp'
  if (/png/i.test(contentType || '')) return '.png'
  return null
}

async function searchImages(keyword, count = 24) {
  const url =
    'https://cn.bing.com/images/async?q=' +
    encodeURIComponent(keyword) +
    `&first=0&count=${count}&mkt=zh-CN&adlt=off`
  const res = await request(url)
  const html = res.body.toString('utf8')
  const found = []
  const re = /murl&quot;:&quot;(.*?)&quot;/g
  let m
  while ((m = re.exec(html))) {
    const u = m[1].replace(/\\u002f/g, '/').replace(/\\\//g, '/')
    if (/^https?:\/\//.test(u)) found.push(u)
  }
  return [...new Set(found)]
}

async function download(u) {
  const res = await request(u)
  if (res.status !== 200) return null
  const buf = res.body
  if (buf.length < 12000) return null // 太小基本是占位图/图标
  if (buf.length > 2 * 1024 * 1024) return null // 太大影响首屏，跳过
  const ext = extOf(buf, res.headers['content-type'])
  if (!ext) return null
  return { buf, ext }
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true })
  const manifest = {}
  for (const p of ALL_PRODUCTS) {
    let urls = []
    try {
      urls = await searchImages(p.keyword)
    } catch (e) {
      console.log(`搜索失败 ${p.id}: ${e.message}`)
    }
    const saved = []
    for (const u of urls) {
      if (saved.length >= 3) break
      try {
        const d = await download(u)
        if (!d) continue
        const name = `p${p.id}-${saved.length + 1}${d.ext}`
        fs.writeFileSync(path.join(OUT_DIR, name), d.buf)
        saved.push('/products/' + name)
      } catch (e) {
        /* 单张失败忽略，继续下一张 */
      }
    }
    manifest[p.id] = saved
    console.log(`商品 ${p.id} -> ${saved.length} 张`, saved.join(' '))
  }
  fs.writeFileSync(path.join(OUT_DIR, 'manifest.json'), JSON.stringify(manifest, null, 2))

  // 清理没被引用的历史文件（例如之前存成 png/gif 又重新抓到 jpg 的）
  const used = new Set()
  Object.values(manifest).forEach((arr) => arr.forEach((u) => used.add(path.basename(u))))
  for (const f of fs.readdirSync(OUT_DIR)) {
    if (f === 'manifest.json') continue
    if (!used.has(f)) fs.unlinkSync(path.join(OUT_DIR, f))
  }
  const total = Object.values(manifest).reduce((a, b) => a + b.length, 0)
  console.log(`\n完成：共 ${total} 张图片 -> ${OUT_DIR}`)
}

main()
