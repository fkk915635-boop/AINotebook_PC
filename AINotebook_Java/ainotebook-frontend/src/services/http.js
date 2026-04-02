import axios from 'axios'
import { getToken, clearToken } from './auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      clearToken()
      if (location.pathname !== '/login') location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`
    }
    return Promise.reject(err)
  }
)

export default http

