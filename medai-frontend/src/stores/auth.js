import { defineStore } from 'pinia'
import http from '../utils/http'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user') || 'null'),
        accessToken: localStorage.getItem('accessToken') || null,
        refreshToken: localStorage.getItem('refreshToken') || null
    }),

    getters: {
        isAuthenticated: (state) => !!state.accessToken,
        currentUser: (state) => state.user,
        userRole: (state) => state.user?.role || null
    },

    actions: {
        async login(username, password) {
            try {
                const response = await http.post('/auth/login', { username, password })
                const data = response.data

                this.accessToken = data.accessToken
                this.refreshToken = data.refreshToken
                this.user = data.user

                localStorage.setItem('accessToken', data.accessToken)
                localStorage.setItem('refreshToken', data.refreshToken)
                localStorage.setItem('user', JSON.stringify(data.user))

                return { success: true }
            } catch (error) {
                console.error('Login failed:', error)
                return {
                    success: false,
                    message: error.response?.data?.message || 'Login failed'
                }
            }
        },

        async register(userData) {
            try {
                const response = await http.post('/auth/register', userData)
                const data = response.data

                this.accessToken = data.accessToken
                this.refreshToken = data.refreshToken
                this.user = data.user

                localStorage.setItem('accessToken', data.accessToken)
                localStorage.setItem('refreshToken', data.refreshToken)
                localStorage.setItem('user', JSON.stringify(data.user))

                return { success: true }
            } catch (error) {
                return {
                    success: false,
                    message: error.response?.data?.message || 'Registration failed'
                }
            }
        },

        logout() {
            this.accessToken = null
            this.refreshToken = null
            this.user = null

            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('user')
        },

        async refreshAccessToken() {
            try {
                const response = await http.post('/auth/refresh', {
                    refreshToken: this.refreshToken
                })

                this.accessToken = response.data.accessToken
                localStorage.setItem('accessToken', response.data.accessToken)

                return true
            } catch (error) {
                this.logout()
                return false
            }
        }
    }
})