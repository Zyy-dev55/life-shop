import request from './request'

/** 认证 */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data)
}

/** 当前用户 */
export const userApi = {
  info: () => request.get('/user/info'),
  update: (data) => request.put('/user/info', data),
  changePassword: (data) => request.put('/user/password', data)
}

/** 商品 / 分类 / 轮播图 */
export const productApi = {
  page: (params) => request.get('/product/page', { params }),
  detail: (id) => request.get(`/product/${id}`),
  hot: (limit = 8) => request.get('/product/hot', { params: { limit } }),
  newest: (limit = 8) => request.get('/product/new', { params: { limit } }),
  categories: () => request.get('/category/list'),
  banners: () => request.get('/banner/list')
}

/** 购物车 */
export const cartApi = {
  list: () => request.get('/cart/list'),
  count: () => request.get('/cart/count'),
  add: (data) => request.post('/cart/add', data),
  updateQuantity: (data) => request.put('/cart/quantity', data),
  updateChecked: (id, checked) => request.put('/cart/checked', null, { params: { id, checked } }),
  checkAll: (checked) => request.put('/cart/check-all', null, { params: { checked } }),
  remove: (id) => request.delete(`/cart/${id}`),
  clear: () => request.delete('/cart/clear')
}

/** 订单 */
export const orderApi = {
  submit: (data) => request.post('/order/submit', data),
  page: (params) => request.get('/order/page', { params }),
  counts: () => request.get('/order/counts'),
  detail: (id) => request.get(`/order/${id}`),
  pay: (id) => request.put(`/order/${id}/pay`),
  cancel: (id) => request.put(`/order/${id}/cancel`),
  confirm: (id) => request.put(`/order/${id}/confirm`),
  logistics: (id) => request.get(`/order/${id}/logistics`)
}

/** 优惠券 */
export const couponApi = {
  /** 领券中心 */
  center: () => request.get('/coupon/center'),
  /** 领取 */
  receive: (couponId) => request.post(`/coupon/${couponId}/receive`),
  /** 我的优惠券 */
  my: (status) => request.get('/coupon/my', { params: status === undefined ? {} : { status } }),
  /** 结算可用券 */
  usable: (amount) => request.get('/coupon/usable', { params: { amount } })
}

/** 售后（仅退款 / 退货退款） */
export const afterSaleApi = {
  /** 申请售后 */
  apply: (data) => request.post('/after-sale', data),
  /** 我的售后 */
  my: (params) => request.get('/after-sale/my', { params }),
  /** 售后详情 */
  detail: (id) => request.get(`/after-sale/${id}`),
  /** 撤销申请 */
  cancel: (id) => request.put(`/after-sale/${id}/cancel`),
  /** 填写退货物流 */
  returnGoods: (id, data) => request.put(`/after-sale/${id}/return`, data)
}

/** 收货地址 */
export const addressApi = {
  list: () => request.get('/address/list'),
  getDefault: () => request.get('/address/default'),
  save: (data) => request.post('/address', data),
  update: (data) => request.put('/address', data),
  remove: (id) => request.delete(`/address/${id}`),
  setDefault: (id) => request.put(`/address/${id}/default`)
}

/** 评价 */
export const reviewApi = {
  /** 商品评价（统计 + 分页，公开） */
  productReviews: (productId, params) => request.get(`/review/product/${productId}`, { params }),
  /** 待评价商品 */
  pending: () => request.get('/review/pending'),
  /** 我的评价 */
  my: (params) => request.get('/review/my', { params }),
  /** 提交评价 */
  submit: (data) => request.post('/review', data)
}

/** 图片上传（评价晒图 / 商品图） */
export const uploadApi = {
  image: (file) => {
    const form = new FormData()
    form.append('file', file)
    // 交给浏览器自动带上 multipart boundary，不要手写 Content-Type
    return request.post('/upload', form)
  }
}

/** 咨询商家（买家端） */
export const consultApi = {
  /** 我的会话列表 */
  list: () => request.get('/consult/list'),
  /** 我的未读消息数 */
  unread: () => request.get('/consult/unread'),
  /** 打开与某店铺的会话（含历史消息） */
  with: (merchantId, productId) => request.get('/consult/with', { params: { merchantId, productId } }),
  /** 会话详情 */
  detail: (id) => request.get(`/consult/${id}`),
  /** 发送咨询 */
  send: (data) => request.post('/consult/send', data)
}

/** 后台管理 */
export const adminApi = {
  dashboard: () => request.get('/admin/statistics/dashboard'),

  productPage: (params) => request.get('/admin/product/page', { params }),
  productDetail: (id) => request.get(`/admin/product/${id}`),
  productSave: (data) => request.post('/admin/product', data),
  productUpdate: (data) => request.put('/admin/product', data),
  productStatus: (id, status) => request.put(`/admin/product/${id}/status`, null, { params: { status } }),
  productDelete: (id) => request.delete(`/admin/product/${id}`),

  categoryList: () => request.get('/admin/category/list'),
  categorySave: (data) => request.post('/admin/category', data),
  categoryUpdate: (data) => request.put('/admin/category', data),
  categoryDelete: (id) => request.delete(`/admin/category/${id}`),

  orderPage: (params) => request.get('/admin/order/page', { params }),
  orderDetail: (id) => request.get(`/admin/order/${id}`),
  orderShip: (id, data) => request.put(`/admin/order/${id}/ship`, data),

  userPage: (params) => request.get('/admin/user/page', { params }),
  userCreate: (data) => request.post('/admin/user', data),
  userStatus: (id, status) => request.put(`/admin/user/${id}/status`, null, { params: { status } }),

  merchantPage: (params) => request.get('/admin/merchant/page', { params }),
  merchantCreate: (data) => request.post('/admin/merchant', data),
  merchantStatus: (id, status) => request.put(`/admin/merchant/${id}/status`, null, { params: { status } }),

  consultPage: (params) => request.get('/admin/consult/page', { params }),
  consultDetail: (id) => request.get(`/admin/consult/${id}`),

  reviewPage: (params) => request.get('/admin/review/page', { params }),
  reviewReply: (id, reply) => request.post(`/admin/review/${id}/reply`, { reply }),
  reviewDelete: (id) => request.delete(`/admin/review/${id}`),

  // 提现审核
  withdrawPage: (params) => request.get('/admin/withdraw/page', { params }),
  withdrawAudit: (id, data) => request.put(`/admin/withdraw/${id}/audit`, data),

  // 售后监管
  afterSalePage: (params) => request.get('/admin/after-sale/page', { params }),

  // 网络安全运维监控
  secOpsOverview: () => request.get('/admin/secops/overview'),
  secOpsTrend: (minutes = 30) => request.get('/admin/secops/trend', { params: { minutes } }),
  secOpsStatusDist: () => request.get('/admin/secops/status-distribution'),
  secOpsTopIps: (limit = 10) => request.get('/admin/secops/top-ips', { params: { limit } }),
  secOpsSlowApis: (limit = 10) => request.get('/admin/secops/slow-apis', { params: { limit } }),
  secOpsAlerts: (limit = 30) => request.get('/admin/secops/alerts', { params: { limit } }),
  secOpsHandleAlert: (id) => request.put(`/admin/secops/alerts/${id}/handle`),
  secOpsBlacklist: () => request.get('/admin/secops/blacklist'),
  secOpsAddBlacklist: (data) => request.post('/admin/secops/blacklist', data),
  secOpsRemoveBlacklist: (id) => request.delete(`/admin/secops/blacklist/${id}`),
  secOpsDetect: () => request.post('/admin/secops/detect'),
  secOpsSimulate: () => request.post('/admin/secops/simulate')
}

/** 商家后台（所有接口只作用于当前登录商家自己的店铺） */
export const merchantApi = {
  profile: () => request.get('/merchant/profile'),
  updateProfile: (data) => request.put('/merchant/profile', data),
  dashboard: () => request.get('/merchant/dashboard'),

  productPage: (params) => request.get('/merchant/product/page', { params }),
  productDetail: (id) => request.get(`/merchant/product/${id}`),
  productSave: (data) => request.post('/merchant/product', data),
  productUpdate: (data) => request.put('/merchant/product', data),
  productStatus: (id, status) => request.put(`/merchant/product/${id}/status`, null, { params: { status } }),
  productDelete: (id) => request.delete(`/merchant/product/${id}`),

  orderPage: (params) => request.get('/merchant/order/page', { params }),
  orderPendingCount: () => request.get('/merchant/order/pending-count'),
  orderDetail: (id) => request.get(`/merchant/order/${id}`),
  orderShip: (id, data) => request.put(`/merchant/order/${id}/ship`, data),

  reviewPage: (params) => request.get('/merchant/review/page', { params }),
  reviewReply: (id, reply) => request.post(`/merchant/review/${id}/reply`, { reply }),

  consultPage: (params) => request.get('/merchant/consult/page', { params }),
  consultUnread: () => request.get('/merchant/consult/unread'),
  consultDetail: (id) => request.get(`/merchant/consult/${id}`),
  consultReply: (id, content) => request.post(`/merchant/consult/${id}/reply`, { content }),
  consultReadAll: () => request.post('/merchant/consult/read-all'),

  // 钱包与提现
  wallet: () => request.get('/merchant/wallet'),
  withdrawApply: (data) => request.post('/merchant/wallet/withdraw', data),
  withdrawPage: (params) => request.get('/merchant/wallet/withdraw/page', { params }),

  // 售后处理
  afterSalePage: (params) => request.get('/merchant/after-sale/page', { params }),
  afterSaleHandle: (id, data) => request.put(`/merchant/after-sale/${id}/handle`, data),
  afterSaleConfirmRefund: (id) => request.put(`/merchant/after-sale/${id}/confirm-refund`)
}
