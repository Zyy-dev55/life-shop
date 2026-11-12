/**
 * 统一响应判定（纯函数，不触碰浏览器环境）。
 *
 * 把「响应怎么分类」从 axios 拦截器里抽出来，是为了让这部分判定逻辑
 * 可以脱离 DOM / localStorage 直接做回归断言；拦截器只负责执行副作用
 * （清理登录态、跳转、弹提示）。
 */

export const RESPONSE_KIND = {
  SUCCESS: 'success',
  UNAUTHORIZED: 'unauthorized',
  BUSINESS: 'business'
}

/**
 * @param {unknown} payload 后端响应体
 * @returns {{kind: string, data: unknown, message: string}}
 */
export function classifyResponse(payload) {
  if (!payload || typeof payload.code !== 'number') {
    return { kind: RESPONSE_KIND.BUSINESS, data: null, message: '响应格式异常' }
  }
  if (payload.code === 200) {
    return { kind: RESPONSE_KIND.SUCCESS, data: payload.data, message: payload.message || '操作成功' }
  }
  if (payload.code === 401) {
    return { kind: RESPONSE_KIND.UNAUTHORIZED, data: null, message: payload.message || '请先登录' }
  }
  return { kind: RESPONSE_KIND.BUSINESS, data: null, message: payload.message || '操作失败' }
}

/** 网络层错误的友好提示，避免把英文原始异常直接抛给用户 */
export function networkErrorMessage(error) {
  if (error && error.message === 'Network Error') {
    return '网络异常，请确认后端服务（8080）已启动'
  }
  return '请求失败，请稍后重试'
}
