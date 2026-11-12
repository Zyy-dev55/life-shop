import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * L1 接口契约回归：把 axios 客户端替换成 spy，断言前端调用的 URL 与 HTTP 方法
 * 与后端 @RequestMapping 完全一致。重构时最容易出错的就是「前端改了路径、
 * 后端没跟上」，这组用例能在不发网络请求的情况下把它拦下来。
 */
const { request } = vi.hoisted(() => ({
  request: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('../src/api/request.js', () => ({ default: request }))

const { authApi, orderApi, afterSaleApi, adminApi, merchantApi } =
  await import('../src/api/index.js')

describe('L1 API 契约：路径 / 方法', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('认证与订单主链路', () => {
    const login = { username: 'zhangsan', password: '123456' }
    authApi.login(login)
    expect(request.post).toHaveBeenCalledWith('/auth/login', login)

    const submit = { addressId: 7, items: [{ productId: 1, quantity: 2 }] }
    orderApi.submit(submit)
    expect(request.post).toHaveBeenCalledWith('/order/submit', submit)

    orderApi.pay(11)
    expect(request.put).toHaveBeenCalledWith('/order/11/pay')

    orderApi.confirm(11)
    expect(request.put).toHaveBeenCalledWith('/order/11/confirm')
  })

  it('售后申请、撤销、退货物流三条买家路径', () => {
    const apply = { orderItemId: 20, type: 1, reason: '商品与描述不符' }
    afterSaleApi.apply(apply)
    expect(request.post).toHaveBeenCalledWith('/after-sale', apply)

    afterSaleApi.my({ pageNum: 1, pageSize: 10 })
    expect(request.get).toHaveBeenCalledWith('/after-sale/my', {
      params: { pageNum: 1, pageSize: 10 }
    })

    afterSaleApi.cancel(100)
    expect(request.put).toHaveBeenCalledWith('/after-sale/100/cancel')

    const tracking = { trackingNo: 'SF1234567890', company: '顺丰速运' }
    afterSaleApi.returnGoods(100, tracking)
    expect(request.put).toHaveBeenCalledWith('/after-sale/100/return', tracking)
  })

  it('商家处理售后与确认退款只走本店接口', () => {
    merchantApi.afterSaleHandle(100, { action: 1 })
    expect(request.put).toHaveBeenCalledWith('/merchant/after-sale/100/handle', { action: 1 })

    merchantApi.afterSaleConfirmRefund(100)
    expect(request.put).toHaveBeenCalledWith('/merchant/after-sale/100/confirm-refund')

    merchantApi.afterSalePage({ status: 0 })
    expect(request.get).toHaveBeenCalledWith('/merchant/after-sale/page', {
      params: { status: 0 }
    })
  })

  it('运维监控接口全部挂在 /admin/secops 下', () => {
    adminApi.secOpsOverview()
    adminApi.secOpsTrend(15)
    adminApi.secOpsStatusDist()
    adminApi.secOpsTopIps(5)
    adminApi.secOpsAlerts()
    adminApi.secOpsHandleAlert(8)
    adminApi.secOpsBlacklist()
    adminApi.secOpsAddBlacklist({ ip: '203.0.113.77' })
    adminApi.secOpsRemoveBlacklist(3)
    adminApi.secOpsDetect()
    adminApi.secOpsSimulate()

    const getUrls = request.get.mock.calls.map((call) => call[0])
    expect(getUrls).toEqual(expect.arrayContaining([
      '/admin/secops/overview',
      '/admin/secops/trend',
      '/admin/secops/status-distribution',
      '/admin/secops/top-ips',
      '/admin/secops/alerts',
      '/admin/secops/blacklist'
    ]))
    expect(request.get).toHaveBeenCalledWith('/admin/secops/trend', { params: { minutes: 15 } })
    expect(request.get).toHaveBeenCalledWith('/admin/secops/top-ips', { params: { limit: 5 } })
    expect(request.put).toHaveBeenCalledWith('/admin/secops/alerts/8/handle')
    expect(request.post).toHaveBeenCalledWith('/admin/secops/blacklist', { ip: '203.0.113.77' })
    expect(request.delete).toHaveBeenCalledWith('/admin/secops/blacklist/3')
    expect(request.post).toHaveBeenCalledWith('/admin/secops/detect')
    expect(request.post).toHaveBeenCalledWith('/admin/secops/simulate')
  })

  it('所有接口都带 /api 之外的相对前缀，未出现硬编码域名或端口', () => {
    authApi.login({})
    orderApi.page({})
    adminApi.secOpsOverview()
    merchantApi.wallet()
    const urls = [
      ...request.post.mock.calls, ...request.get.mock.calls,
      ...request.put.mock.calls, ...request.delete.mock.calls
    ].map((call) => String(call[0]))
    expect(urls.every((url) => url.startsWith('/'))).toBe(true)
    expect(urls.some((url) => /^https?:/.test(url) || url.includes(':8080'))).toBe(false)
  })
})
