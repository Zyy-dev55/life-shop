import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('ls_token') || '',
    user: JSON.parse(localStorage.getItem('ls_user') || 'null'),
    // 商家账号登录时后端会带上店铺信息
    merchant: JSON.parse(localStorage.getItem('ls_merchant') || 'null')
  }),

  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user && state.user.role === 'ADMIN',
    isMerchant: (state) => state.user && state.user.role === 'MERCHANT',
    shopName: (state) => (state.merchant ? state.merchant.name : ''),
    nickname: (state) => (state.user ? state.user.nickname || state.user.username : '游客')
  },

  actions: {
    saveLogin(data) {
      this.token = data.token
      this.user = data.user
      this.merchant = data.merchant || null
      localStorage.setItem('ls_token', data.token)
      localStorage.setItem('ls_user', JSON.stringify(data.user))
      localStorage.setItem('ls_merchant', JSON.stringify(this.merchant))
    },

    async login(form) {
      const data = await authApi.login(form)
      this.saveLogin(data)
      return data
    },

    async register(form) {
      const data = await authApi.register(form)
      this.saveLogin(data)
      return data
    },

    async fetchInfo() {
      const data = await userApi.info()
      this.user = data
      localStorage.setItem('ls_user', JSON.stringify(data))
      return data
    },

    logout() {
      this.token = ''
      this.user = null
      this.merchant = null
      localStorage.removeItem('ls_token')
      localStorage.removeItem('ls_user')
      localStorage.removeItem('ls_merchant')
    }
  }
})
