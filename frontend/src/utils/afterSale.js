/**
 * 售后状态机（前端侧）。与后端 AfterSale 实体常量一一对应。
 *
 * 后端负责状态流转的落库与校验，这里只做「展示文案」与「按钮可见性」判定，
 * 并在回归测试中与后端状态机做一致性断言：两边一旦有任何一方改了流转规则，
 * 测试就会立刻失败，避免前后端各自漂移。
 */

export const AFTER_SALE_TYPE = {
  REFUND_ONLY: 1,
  RETURN_REFUND: 2
}

export const AFTER_SALE_STATUS = {
  WAIT_MERCHANT: 0,
  WAIT_RETURN: 1,
  WAIT_RECEIVE: 2,
  REFUNDED: 3,
  REJECTED: 4,
  CANCELED: 5
}

/** 状态码 → 中文文案（后端 fillText 的兜底来源） */
const STATUS_TEXT = {
  0: '待商家处理',
  1: '待买家寄回',
  2: '待商家收货',
  3: '退款完成',
  4: '已拒绝',
  5: '已撤销'
}

export const afterSaleStatusText = (status) => STATUS_TEXT[status] || '未知状态'
export const afterSaleTypeText = (type) =>
  type === AFTER_SALE_TYPE.REFUND_ONLY ? '仅退款' : type === AFTER_SALE_TYPE.RETURN_REFUND ? '退货退款' : '未知类型'

/** 仅「待商家处理」「待买家寄回」允许买家撤销 */
export const canCancel = (status) =>
  status === AFTER_SALE_STATUS.WAIT_MERCHANT || status === AFTER_SALE_STATUS.WAIT_RETURN

/** 仅「待买家寄回」允许填写退货物流 */
export const canFillReturn = (status) => status === AFTER_SALE_STATUS.WAIT_RETURN

export const isTerminal = (status) =>
  status === AFTER_SALE_STATUS.REFUNDED ||
  status === AFTER_SALE_STATUS.REJECTED ||
  status === AFTER_SALE_STATUS.CANCELED

/**
 * 预测商家处理动作之后的状态，与后端 AfterSaleService.handleByMerchant 保持一致：
 *   仅退款 + 同意 → 3；退货退款 + 同意 → 1；拒绝 → 4；非法动作/重复处理 → null
 */
export const nextStatusOnMerchantHandle = (type, status, action) => {
  if (status !== AFTER_SALE_STATUS.WAIT_MERCHANT) return null
  if (action === 2) return AFTER_SALE_STATUS.REJECTED
  if (action === 1) {
    return type === AFTER_SALE_TYPE.REFUND_ONLY
      ? AFTER_SALE_STATUS.REFUNDED
      : AFTER_SALE_STATUS.WAIT_RETURN
  }
  return null
}
