<template>
  <el-container class="main-layout">
    <!-- Sidebar -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!isCollapsed">🏥 MedAI</span>
        <span v-else>🏥</span>
      </div>

      <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          router
          class="sidebar-menu"
          background-color="#001529"
          text-color="#fff"
          active-text-color="#409eff"
      >
        <!-- ADMIN MENU - Administrative functions only -->
        <template v-if="userRole === 'ADMIN'">
          <el-menu-item index="/admin-dashboard">
            <el-icon><Odometer /></el-icon>
            <span>Dashboard</span>
          </el-menu-item>

          <el-sub-menu index="admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>Administration</span>
            </template>
            <el-menu-item index="/admin/users">
              <el-icon><User /></el-icon>
              <span>User Management</span>
            </el-menu-item>
            <el-menu-item index="/admin/system">
              <el-icon><Monitor /></el-icon>
              <span>System Monitoring</span>
            </el-menu-item>
            <el-menu-item index="/audit">
              <el-icon><List /></el-icon>
              <span>Audit Logs</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/clinical-notes">
            <el-icon><Document /></el-icon>
            <span>Clinical Notes</span>
          </el-menu-item>

          <el-menu-item index="/pacs">
            <el-icon><Connection /></el-icon>
            <span>PACS Management</span>
          </el-menu-item>
        </template>

        <!-- DOCTOR MENU - Clinical functions -->
        <template v-else-if="userRole === 'DOCTOR'">
          <el-menu-item index="/doctor-dashboard">
            <el-icon><Odometer /></el-icon>
            <span>Dashboard</span>
          </el-menu-item>

          <el-menu-item index="/worklist">
            <el-icon><Tickets /></el-icon>
            <span>My Worklist</span>
          </el-menu-item>

          <el-menu-item index="/my-patients">
            <el-icon><User /></el-icon>
            <span>My Patients</span>
          </el-menu-item>

          <el-menu-item index="/studies">
            <el-icon><FolderOpened /></el-icon>
            <span>Studies</span>
          </el-menu-item>

          <el-menu-item index="/clinical-notes">
            <el-icon><Document /></el-icon>
            <span>Clinical Notes</span>
          </el-menu-item>

          <el-menu-item index="/reports">
            <el-icon><Document /></el-icon>
            <span>Reports</span>
          </el-menu-item>

          <el-menu-item index="/appointments">
            <el-icon><Calendar /></el-icon>
            <span>Appointments</span>
          </el-menu-item>

          <el-menu-item index="/pacs">
            <el-icon><Connection /></el-icon>
            <span>PACS</span>
          </el-menu-item>
        </template>

        <!-- NURSE/TECHNICIAN MENU - Upload and scheduling -->
        <!-- Schedule/Appointments - Role-based -->
        <el-menu-item
            v-if="userRole === 'NURSE' || userRole === 'TECHNICIAN'"
            index="/nurse/schedule"
        >
          <el-icon><Calendar /></el-icon>
          <span>Schedule</span>
        </el-menu-item>





        <!-- Studies - for medical staff -->
        <el-menu-item
            v-if="userRole === 'NURSE' || userRole === 'TECHNICIAN'"
            index="/studies"
        >
          <el-icon><FolderOpened /></el-icon>
          <span>Studies</span>
        </el-menu-item>

        <!-- RESEARCHER MENU - Data and AI -->
        <template v-else-if="userRole === 'RESEARCHER'">
          <el-menu-item index="/researcher-dashboard">
            <el-icon><Odometer /></el-icon>
            <span>Dashboard</span>
          </el-menu-item>

          <el-menu-item index="/studies">
            <el-icon><FolderOpened /></el-icon>
            <span>Studies</span>
          </el-menu-item>

          <el-menu-item index="/reports">
            <el-icon><Document /></el-icon>
            <span>Reports</span>
          </el-menu-item>

          <el-menu-item index="/ai-tasks">
            <el-icon><Cpu /></el-icon>
            <span>AI Tasks</span>
          </el-menu-item>
        </template>

        <!-- PATIENT MENU - Personal health data only -->
        <template v-else-if="userRole === 'PATIENT'">
          <el-menu-item index="/patient-dashboard">
            <el-icon><Odometer /></el-icon>
            <span>My Health</span>
          </el-menu-item>

          <el-menu-item index="/my-appointments">
            <el-icon><Calendar /></el-icon>
            <span>My Appointments</span>
          </el-menu-item>

          <el-menu-item index="/my-reports">
            <el-icon><Document /></el-icon>
            <span>My Reports</span>
          </el-menu-item>

          <el-menu-item index="/my-prescriptions">
            <el-icon><FirstAidKit /></el-icon>
            <span>My Prescriptions</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-footer">
        <el-button text @click="toggleCollapse" class="collapse-btn">
          <el-icon v-if="isCollapsed"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <!-- Main Content -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: dashboardPath }">Home</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">{{ userInitial }}</el-avatar>
              <div class="user-details">
                <span class="user-name">{{ userName }}</span>
                <span class="user-role-badge">{{ userRole }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  {{ userEmail }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  Logout
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>


<script>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { FirstAidKit } from '@element-plus/icons-vue'
import {
  Odometer, FolderOpened, Upload, Calendar, Tickets, Document, Connection,
  Setting, User, Monitor, List, Cpu, ArrowDown, SwitchButton, Expand, Fold
} from '@element-plus/icons-vue'

export default {
  name: 'MainLayout',
  components: {
    Odometer, FolderOpened, Upload, Calendar, Tickets, Document, Connection,
    Setting, User, Monitor, List, Cpu, ArrowDown, SwitchButton, Expand, Fold, FirstAidKit
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()
    const isCollapsed = ref(false)

    const userRole = computed(() => {
      const user = authStore.user || JSON.parse(localStorage.getItem('user') || '{}')
      return user.role || ''
    })

    const userName = computed(() => {
      const user = authStore.user || JSON.parse(localStorage.getItem('user') || '{}')
      return user.fullName || user.username || 'User'
    })

    const userEmail = computed(() => {
      const user = authStore.user || JSON.parse(localStorage.getItem('user') || '{}')
      return user.email || ''
    })

    const userInitial = computed(() => userName.value.charAt(0).toUpperCase())

    const dashboardPath = computed(() => {
      const paths = {
        'ADMIN': '/admin-dashboard',
        'DOCTOR': '/doctor-dashboard',
        'NURSE': '/nurse-dashboard',
        'TECHNICIAN': '/nurse-dashboard',
        'RESEARCHER': '/researcher-dashboard',
        'PATIENT': '/patient-dashboard'
      }
      return paths[userRole.value] || '/dashboard'
    })

    const activeMenu = computed(() => route.path)
    const currentPageTitle = computed(() => route.meta?.title || 'Page')

    const toggleCollapse = () => { isCollapsed.value = !isCollapsed.value }

    const handleCommand = (command) => {
      if (command === 'logout') {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        authStore.logout()
        router.push('/login')
      }
    }

    return {
      isCollapsed, userRole, userName, userEmail, userInitial,
      dashboardPath, activeMenu, currentPageTitle, toggleCollapse, handleCommand
    }
  }
}
</script>

<style scoped>
.main-layout { height: 100vh; }

.sidebar {
  background-color: #001529;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid #0c2135;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.sidebar-menu:not(.el-menu--collapse) { width: 100%; }

.sidebar-footer {
  padding: 10px;
  border-top: 1px solid #0c2135;
}

.collapse-btn { width: 100%; color: #fff; }

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-right { display: flex; align-items: center; gap: 20px; }

.user-info { display: flex; align-items: center; gap: 10px; cursor: pointer; }

.user-avatar {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
}

.user-details { display: flex; flex-direction: column; }
.user-name { font-weight: 500; font-size: 14px; }
.user-role-badge { font-size: 10px; color: #909399; text-transform: uppercase; }

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>