import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000,
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

// Response interceptor - handle errors globally
http.interceptors.response.use(
    (response) => {
        return response
    },
    (error) => {
        // ✅ FIX: Handle authentication errors during logout gracefully
        if (error.response) {
            const { status } = error.response

            switch (status) {
                case 401:
                    // Unauthorized - token expired or invalid
                    // Only show message if user is not already on login page
                    if (router.currentRoute.value.path !== '/login') {
                        ElMessage.warning('Session expired. Please login again.')
                        localStorage.removeItem('accessToken')
                        localStorage.removeItem('user')
                        router.push('/login')
                    }
                    break

                case 403:
                    // Forbidden - user doesn't have permission
                    // ✅ FIX: Don't show error during logout or if already logged out
                    const isLoggedOut = !localStorage.getItem('accessToken')
                    const isLoggingOut = router.currentRoute.value.path === '/login'

                    if (!isLoggedOut && !isLoggingOut) {
                        // Only show error if user is actually logged in and not logging out
                        if (!error.config.url?.includes('/notifications/unread-count')) {
                            ElMessage.error('Access denied')
                        }
                    }
                    break

                case 404:
                    // Not found
                    if (!error.config.skipErrorMessage) {
                        ElMessage.error('Resource not found')
                    }
                    break

                case 500:
                    // Server error
                    ElMessage.error('Server error. Please try again later.')
                    break

                default:
                    // Other errors
                    if (!error.config.skipErrorMessage) {
                        const message = error.response.data?.message || 'An error occurred'
                        ElMessage.error(message)
                    }
            }
        } else if (error.request) {
            // Request made but no response
            ElMessage.error('Network error. Please check your connection.')
        } else {
            // Something else happened
            ElMessage.error('An error occurred')
        }

        return Promise.reject(error)
    }
)

export default http