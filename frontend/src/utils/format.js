/** 金额格式化：129 -> 129.00 */
export const money = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

/** 订单状态文案 */
export const ORDER_STATUS = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消'
}

/** 订单状态对应的 Element 标签类型 */
export const ORDER_STATUS_TYPE = {
  0: 'warning',
  1: 'danger',
  2: 'primary',
  3: 'success',
  4: 'info'
}

/** 时间格式化 */
export const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 图片加载失败占位图 */
export const FALLBACK_IMAGE =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600">
      <rect width="600" height="600" fill="#f3ede6"/>
      <text x="300" y="300" font-size="26" fill="#b6a698" text-anchor="middle" dominant-baseline="middle">悦家生活</text>
    </svg>`
  )

export const onImgError = (event) => {
  if (event && event.target) {
    event.target.src = FALLBACK_IMAGE
  }
}

/** 商品组图拆分 */
export const splitImages = (images, mainImage) => {
  const list = (images || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
  if (list.length === 0 && mainImage) {
    list.push(mainImage)
  }
  return list
}
