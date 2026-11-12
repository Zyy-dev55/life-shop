import { describe, expect, it } from 'vitest'
import { classifyResponse, networkErrorMessage, RESPONSE_KIND } from '../src/utils/response.js'

/**
 * L1 协议层 / L3 鉴权层在前端的投影：统一响应体的分类判定。
 * 断言点在于「只有 code=200 才解包 data」「401 独立分类」「异常结构不抛错」。
 */
describe('L1/L3 classifyResponse 统一响应判定', () => {
  it('code=200 → 解包 data 交给页面', () => {
    const data = { list: [{ id: 1 }], total: 1 }
    expect(classifyResponse({ code: 200, message: '操作成功', data })).toEqual({
      kind: RESPONSE_KIND.SUCCESS,
      data,
      message: '操作成功'
    })
  })

  it('code=401 → 独立分类，交给拦截器清理登录态', () => {
    expect(classifyResponse({ code: 401, message: '登录状态已失效，请重新登录' })).toEqual({
      kind: RESPONSE_KIND.UNAUTHORIZED,
      data: null,
      message: '登录状态已失效，请重新登录'
    })
    expect(classifyResponse({ code: 401 }).message).toBe('请先登录')
  })

  it('业务失败码（400/403/500）→ 保留错误文案，不解包', () => {
    for (const code of [400, 403, 429, 500]) {
      const verdict = classifyResponse({ code, message: '库存不足', data: { secret: true } })
      expect(verdict.kind).toBe(RESPONSE_KIND.BUSINESS)
      expect(verdict.data).toBeNull()
      expect(verdict.message).toBe('库存不足')
    }
    expect(classifyResponse({ code: 500 }).message).toBe('操作失败')
  })

  it('响应体结构异常时不得抛出 TypeError，而是转成可读提示', () => {
    for (const payload of [null, undefined, {}, 'gateway html', 0, []]) {
      const verdict = classifyResponse(payload)
      expect(verdict.kind).toBe(RESPONSE_KIND.BUSINESS)
      expect(verdict.message).toBe('响应格式异常')
    }
  })

  it('网络层异常给出可排障的中文提示', () => {
    expect(networkErrorMessage({ message: 'Network Error' }))
      .toContain('8080')
    expect(networkErrorMessage({ message: 'timeout of 15000ms exceeded' }))
      .toBe('请求失败，请稍后重试')
    expect(networkErrorMessage(null)).toBe('请求失败，请稍后重试')
  })
})
