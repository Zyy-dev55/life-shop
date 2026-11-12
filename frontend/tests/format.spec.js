import { describe, expect, it } from 'vitest'
import {
  FALLBACK_IMAGE,
  formatTime,
  money,
  onImgError,
  ORDER_STATUS,
  ORDER_STATUS_TYPE,
  splitImages
} from '../src/utils/format.js'

/**
 * L6 前端呈现层回归：金额、状态文案、时间、图片容错等纯展示逻辑。
 * 这些函数被几乎所有页面复用，一旦退化会直接表现为「页面显示 NaN / 空白」。
 */
describe('L6 format.js 展示工具', () => {
  it('金额统一保留两位小数，非法输入回落为 0.00', () => {
    expect(money(129)).toBe('129.00')
    expect(money('19.9')).toBe('19.90')
    expect(money(0)).toBe('0.00')
    expect(money(null)).toBe('0.00')
    expect(money(undefined)).toBe('0.00')
    expect(money('abc')).toBe('0.00')
    expect(money(NaN)).toBe('0.00')
  })

  it('订单状态文案与标签类型一一对应，不允许缺项', () => {
    expect(ORDER_STATUS).toEqual({
      0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消'
    })
    expect(Object.keys(ORDER_STATUS)).toHaveLength(5)
    expect(Object.keys(ORDER_STATUS_TYPE)).toEqual(Object.keys(ORDER_STATUS))
    expect(ORDER_STATUS_TYPE[3]).toBe('success')
  })

  it('时间格式化：去掉 T、截断到秒，空值显示占位符', () => {
    expect(formatTime('2026-11-12T08:30:45.123')).toBe('2026-11-12 08:30:45')
    expect(formatTime('2026-11-12 08:30:45')).toBe('2026-11-12 08:30:45')
    expect(formatTime(null)).toBe('-')
    expect(formatTime('')).toBe('-')
  })

  it('商品组图拆分：去空白、去空项，缺省回退主图', () => {
    expect(splitImages('/a.jpg, /b.jpg ,,/c.jpg', '/main.jpg'))
      .toEqual(['/a.jpg', '/b.jpg', '/c.jpg'])
    expect(splitImages('', '/main.jpg')).toEqual(['/main.jpg'])
    expect(splitImages(null, '')).toEqual([])
    expect(splitImages('  ', '/main.jpg')).toEqual(['/main.jpg'])
  })

  it('图片加载失败时替换为内置占位图', () => {
    const target = { src: '/broken.jpg' }
    onImgError({ target })
    expect(target.src).toBe(FALLBACK_IMAGE)
    expect(FALLBACK_IMAGE).toMatch(/^data:image\/svg\+xml/)
    expect(() => onImgError(null)).not.toThrow()
  })
})
