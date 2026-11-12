import axios from 'axios'
import { ElMessage } from 'element-plus'
import { classifyResponse, networkErrorMessage, RESPONSE_KIND } from '@/utils/response'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ls_token')
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const verdict = classifyResponse(response.data)

    if (verdict.kind === RESPONSE_KIND.SUCCESS) {
      return verdict.data
    }
    if (verdict.kind === RESPONSE_KIND.UNAUTHORIZED) {
      localStorage.removeItem('ls_token')
      localStorage.removeItem('ls_user')
      const current = window.location.pathname + window.location.search
      if (!current.startsWith('/login')) {
        window.location.href = '/login?redirect=' + encodeURIComponent(current)
      }
      return Promise.reject(new Error(verdict.message))
    }
    ElMessage.error(verdict.message)
    return Promise.reject(new Error(verdict.message))
  },
  (error) => {
    ElMessage.error(networkErrorMessage(error))
    return Promise.reject(error)
  }
)

export default request
