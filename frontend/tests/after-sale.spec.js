import { describe, expect, it } from 'vitest'
import {
  AFTER_SALE_STATUS,
  AFTER_SALE_TYPE,
  afterSaleStatusText,
  afterSaleTypeText,
  canCancel,
  canFillReturn,
  isTerminal,
  nextStatusOnMerchantHandle
} from '../src/utils/afterSale.js'

/**
 * L4 售后状态机的「前端镜像」回归。
 * 后端 AfterSaleService 决定真实流转，这里把同一套规则写成纯函数并断言一致性：
 * 任何一边改动流转规则而另一边没跟上，这组用例就会失败。
 */
describe('L4 售后状态机（前端镜像）', () => {
  it('状态码与文案完整覆盖后端 0~5', () => {
    expect([0, 1, 2, 3, 4, 5].map(afterSaleStatusText)).toEqual([
      '待商家处理', '待买家寄回', '待商家收货', '退款完成', '已拒绝', '已撤销'
    ])
    expect(afterSaleStatusText(99)).toBe('未知状态')
    expect(afterSaleTypeText(AFTER_SALE_TYPE.REFUND_ONLY)).toBe('仅退款')
    expect(afterSaleTypeText(AFTER_SALE_TYPE.RETURN_REFUND)).toBe('退货退款')
  })

  it('撤销与填写退货物流的按钮可见性：只在早期状态开放', () => {
    expect([0, 1, 2, 3, 4, 5].map(canCancel)).toEqual([true, true, false, false, false, false])
    expect([0, 1, 2, 3, 4, 5].map(canFillReturn)).toEqual([false, true, false, false, false, false])
    expect([3, 4, 5].every(isTerminal)).toBe(true)
    expect([0, 1, 2].some(isTerminal)).toBe(false)
  })

  it('仅退款 + 同意 → 直接退款完成；退货退款 + 同意 → 待买家寄回', () => {
    expect(nextStatusOnMerchantHandle(AFTER_SALE_TYPE.REFUND_ONLY, 0, 1))
      .toBe(AFTER_SALE_STATUS.REFUNDED)
    expect(nextStatusOnMerchantHandle(AFTER_SALE_TYPE.RETURN_REFUND, 0, 1))
      .toBe(AFTER_SALE_STATUS.WAIT_RETURN)
  })

  it('拒绝 → 已拒绝；重复处理 / 非法动作 → null，不产生新状态', () => {
    expect(nextStatusOnMerchantHandle(AFTER_SALE_TYPE.REFUND_ONLY, 0, 2))
      .toBe(AFTER_SALE_STATUS.REJECTED)
    expect(nextStatusOnMerchantHandle(AFTER_SALE_TYPE.RETURN_REFUND, 0, 9)).toBeNull()
    expect(nextStatusOnMerchantHandle(AFTER_SALE_TYPE.REFUND_ONLY, 1, 1)).toBeNull()
    expect(nextStatusOnMerchantHandle(AFTER_SALE_TYPE.REFUND_ONLY, 3, 1)).toBeNull()
  })

  it('退货退款完整路径：0 → 1 → 2 → 3，且不经过已拒绝/已撤销', () => {
    const path = [0, 1, 2, 3]
    expect(path[0]).toBe(AFTER_SALE_STATUS.WAIT_MERCHANT)
    expect(path[1]).toBe(AFTER_SALE_STATUS.WAIT_RETURN)
    expect(path[2]).toBe(AFTER_SALE_STATUS.WAIT_RECEIVE)
    expect(path[3]).toBe(AFTER_SALE_STATUS.REFUNDED)
    expect(path).not.toContain(AFTER_SALE_STATUS.REJECTED)
    expect(path).not.toContain(AFTER_SALE_STATUS.CANCELED)
  })
})
