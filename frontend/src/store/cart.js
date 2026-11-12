import { defineStore } from 'pinia'
import { cartApi } from '@/api'
import { useUserStore } from './user'

export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0
  }),

  actions: {
    async refresh() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        this.count = 0
        return
      }
      try {
        this.count = await cartApi.count()
      } catch (e) {
        this.count = 0
      }
    },

    reset() {
      this.count = 0
    }
  }
})
