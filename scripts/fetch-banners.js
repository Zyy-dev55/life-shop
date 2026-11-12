/**
 * 首页轮播图抓取（本地演示用）
 * 运行：node scripts/fetch-banners.js
 */
const fs = require('fs')
const path = require('path')
const https = require('https')
const http = require('http')

const ROOT = path.join(__dirname, '..')
const OUT_DIR = path.join(ROOT, 'frontend', 'public', 'banners')

const BANNERS = [
  { id: 1, keyword: '厨房 烹饪 场景 家居 横幅' },
  { id: 2, keyword: '清洁 打扫 厨房 擦拭 场景' },
  { id: 3, keyword: '衣柜 收纳 整理 家居 场景' },
]

const UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'

function request(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http
    const req = lib.get(
      url,
      {
        headers: {
          'User-Agent': UA,
          Referer: 'https://cn.bing.com/',
          'Accept-Language': 'zh-CN,zh;q=0.9',
        },
        timeout: 25000,
      },
      (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          res.resume()
          return resolve(request(new URL(res.headers.location, url).href))
        }
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: Buffer.concat(chunks) }))
      }
    )
    req.on('timeout', () => req.destroy(new Error('timeout')))
    req.on('error', reject)
  })
}

function extOf(buf) {
  if (buf[0] === 0xff && buf[1] === 0xd8) return '.jpg'
  if (buf[0] === 0x89 && buf[1] === 0x50) return '.png'
  if (buf.slice(0, 4).toString() === 'RIFF' && buf.slice(8, 12).toString() === 'WEBP') return '.webp'
  return null
}

async function search(keyword) {
  const url =
    'https://cn.bing.com/images/async?q=' +
    encodeURIComponent(keyword) +
    '&first=0&count=30&mkt=zh-CN&adlt=off&qft=+filterui:aspect-wide'
  const res = await request(url)
  const html = res.body.toString('utf8')
  const out = []
  const re = /murl&quot;:&quot;(.*?)&quot;/g
  let m
  while ((m = re.exec(html))) {
    const u = m[1].replace(/\\u002f/g, '/').replace(/\\\//g, '/')
    if (/^https?:\/\//.test(u)) out.push(u)
  }
  return [...new Set(out)]
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true })
  for (const b of BANNERS) {
    let urls = []
    try {
      urls = await search(b.keyword)
    } catch (e) {
      console.log('搜索失败', b.id, e.message)
    }
    let ok = false
    for (const u of urls) {
      try {
        const res = await request(u)
        if (res.status !== 200 || res.body.length < 40000) continue
        const ext = extOf(res.body)
        if (!ext) continue
        fs.writeFileSync(path.join(OUT_DIR, `b${b.id}${ext}`), res.body)
        console.log(`banner ${b.id} -> b${b.id}${ext} (${Math.round(res.body.length / 1024)}KB)`)
        ok = true
        break
      } catch (e) {
        /* ignore */
      }
    }
    if (!ok) console.log(`banner ${b.id} 未抓到合适图片`)
  }
}

main()
