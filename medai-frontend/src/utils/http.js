import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
    baseURL: '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor - add auth token
http.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response interceptor - handle errors
http.interceptors.response.use(
    (response) => {
        return response
    },
    async (error) => {
        const originalRequest = error.config

        // Handle 401 Unauthorized
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true

            const refreshToken = localStorage.getItem('refreshToken')
            if (refreshToken) {
                try {
                    const response = await axios.post('/api/auth/refresh', {
                        refreshToken: refreshToken
                    })

                    const newToken = response.data.accessToken
                    localStorage.setItem('accessToken', newToken)

                    originalRequest.headers.Authorization = `Bearer ${newToken}`
                    return http(originalRequest)
                } catch (refreshError) {
                    // Refresh failed, logout user
                    localStorage.removeItem('accessToken')
                    localStorage.removeItem('refreshToken')
                    localStorage.removeItem('user')
                    window.location.href = '/login'
                    return Promise.reject(refreshError)
                }
            } else {
                window.location.href = '/login'
            }
        }

        // Handle other errors
        if (error.response?.status === 403) {
            ElMessage.error('Access denied')
        } else if (error.response?.status === 404) {
            ElMessage.error('Resource not found')
        } else if (error.response?.status >= 500) {
            ElMessage.error('Server error. Please try again later.')
        }

        return Promise.reject(error)
    }
)

export default http