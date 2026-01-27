// src/router/index.js (or src/router/index.ts)
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/LoginView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/RegisterView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        component: () => import('../layout/MainLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            { path: '', redirect: '/dashboard' },

            // Role-specific dashboards
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/DashboardView.vue'),
                meta: { title: 'Dashboard' }
            },
            {
                path: 'admin-dashboard',
                name: 'AdminDashboard',
                component: () => import('../views/dashboards/AdminDashboard.vue'),
                meta: { title: 'Admin Dashboard', roles: ['ADMIN'] }
            },
            {
                path: 'doctor-dashboard',
                name: 'DoctorDashboard',
                component: () => import('../views/dashboards/DoctorDashboard.vue'),
                meta: { title: 'Doctor Dashboard', roles: ['DOCTOR'] }
            },
            {
                path: 'nurse-dashboard',
                name: 'NurseDashboard',
                component: () => import('../views/dashboards/NurseDashboard.vue'),
                meta: { title: 'Nurse Dashboard', roles: ['NURSE', 'TECHNICIAN'] }
            },
            {
                path: 'patient-dashboard',
                name: 'PatientDashboard',
                component: () => import('../views/dashboards/PatientDashboard.vue'),
                meta: { title: 'My Health', roles: ['PATIENT'] }
            },

            // ADMIN ONLY routes
            {
                path: 'admin/users',
                name: 'UserManagement',
                component: () => import('../views/admin/UserManagementView.vue'),
                meta: { title: 'User Management', roles: ['ADMIN'] }
            },
            {
                path: 'admin/system',
                name: 'SystemMonitoring',
                component: () => import('../views/admin/SystemMonitoringView.vue'),
                meta: { title: 'System Monitoring', roles: ['ADMIN'] }
            },
            {
                path: 'audit',
                name: 'AuditLogs',
                component: () => import('../views/AuditLogView.vue'),
                meta: { title: 'Audit Logs', roles: ['ADMIN'] }
            },

            // DOCTOR routes (also accessible by ADMIN for oversight)
            {
                path: 'worklist',
                name: 'Worklist',
                component: () => import('../views/doctor/WorklistView.vue'),
                meta: { title: 'Worklist', roles: ['DOCTOR'] }
            },
            {
                path: 'reports',
                name: 'Reports',
                component: () => import('../views/ReportListView.vue'),
                meta: { title: 'Reports', roles: ['DOCTOR', 'RESEARCHER'] }
            },
            {
                path: 'reports/new/:studyId?',
                name: 'NewReport',
                component: () => import('../views/ReportEditorView.vue'),
                meta: { title: 'New Report', roles: ['DOCTOR'] }
            },
            {
                path: 'reports/:id/edit',  // ← CHANGED ORDER
                name: 'EditReport',
                component: () => import('../views/ReportEditorView.vue'),
                meta: { title: 'Edit Report', roles: ['DOCTOR'] }
            },
            {
                path: 'my-patients',
                name: 'MyPatients',
                component: () => import('../views/doctor/MyPatientsView.vue'),
                meta: { title: 'My Patients', roles: ['DOCTOR'] }
            },

            // NURSE/TECHNICIAN routes
            {
                path: 'upload',
                name: 'Upload',
                component: () => import('../views/UploadView.vue'),
                meta: { title: 'Upload Images', roles: ['DOCTOR', 'NURSE', 'TECHNICIAN'] }
            },
            {
                path: 'nurse/schedule',
                name: 'NurseSchedule',
                component: () => import('../views/nurse/ScheduleView.vue'),
                meta: { title: 'Today\'s Schedule', roles: ['NURSE', 'TECHNICIAN'] }
            },

            // SHARED routes (multiple roles)
            {
                path: 'studies',
                name: 'Studies',
                component: () => import('../views/StudyListView.vue'),
                meta: { title: 'Studies', roles: ['DOCTOR', 'NURSE', 'TECHNICIAN', 'RESEARCHER'] }
            },
            {
                path: 'studies/:id',
                name: 'StudyDetail',
                component: () => import('../views/StudyDetailView.vue'),
                meta: { title: 'Study Detail', roles: ['DOCTOR', 'NURSE', 'TECHNICIAN', 'RESEARCHER'] }
            },
            {
                path: 'viewer/:id',
                name: 'ImageViewer',
                component: () => import('../views/ImageViewerView.vue'),
                meta: { title: 'Image Viewer', roles: ['DOCTOR', 'RESEARCHER'] }
            },
            {
                path: 'appointments',
                name: 'Appointments',
                component: () => import('../views/doctor/AppointmentsView.vue'),
                meta: { title: 'Appointments', roles: ['DOCTOR', 'NURSE'] }
            },
            {
                path: 'pacs',
                name: 'PACS',
                component: () => import('../views/PacsView.vue'),
                meta: { title: 'PACS', roles: ['ADMIN', 'DOCTOR'] }
            },

            // PATIENT ONLY routes
            {
                path: 'my-health',
                name: 'MyHealth',
                component: () => import('../views/patient/HealthOverviewView.vue'),
                meta: { title: 'My Health', roles: ['PATIENT'] }
            },
            {
                path: 'my-appointments',
                name: 'MyAppointments',
                component: () => import('../views/patient/MyAppointmentsView.vue'),
                meta: { title: 'My Appointments', roles: ['PATIENT'] }
            },
            {
                path: 'my-reports',
                name: 'MyReports',
                component: () => import('../views/patient/MyReportsView.vue'),
                meta: { title: 'My Reports', roles: ['PATIENT'] }
            }
        ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('accessToken')
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const userRole = user.role || ''

    // Public routes
    if (to.meta.requiresAuth === false) {
        if (token && (to.path === '/login' || to.path === '/register')) {
            next(getDashboardForRole(userRole))
            return
        }
        next()
        return
    }

    // Check authentication
    if (!token) {
        next('/login')
        return
    }

    // Redirect /dashboard to role-specific dashboard
    if (to.path === '/dashboard') {
        next(getDashboardForRole(userRole))
        return
    }

    // Check role-based access
    if (to.meta.roles && to.meta.roles.length > 0) {
        if (!to.meta.roles.includes(userRole)) {
            next(getDashboardForRole(userRole))
            return
        }
    }

    next()
})

function getDashboardForRole(role) {
    const dashboards = {
        ADMIN: '/admin-dashboard',
        DOCTOR: '/doctor-dashboard',
        NURSE: '/nurse-dashboard',
        TECHNICIAN: '/nurse-dashboard',
        RESEARCHER: '/studies',
        PATIENT: '/patient-dashboard'
    }
    return dashboards[role] || '/login'
}

export default router
