import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue')
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/Register.vue')
    },
    {
        path: '/owner',
        component: () => import('@/views/owner/Layout.vue'),
        meta: { requiresAuth: true },
        children: [
            {   path: '',
                redirect: '/owner/home'
            },
            {   path: 'home',
                component: () => import('@/views/owner/OwnerHome.vue'),
                meta: { title: '业主首页' }
            },
            {
                path: 'bills',
                name: 'OwnerBills',
                component: () => import('@/views/owner/OwnerBills.vue'),
                meta: { title: '在线缴费' }
            },
            {   path: 'repairs',
                component: () => import('@/views/owner/OwnerRepair.vue'),
                meta: { title: '设施报修' }
            },
            { path: 'parking',
                component: () => import('@/views/owner/OwnerParking.vue'),
                meta: { title: '车位管理' }
            },
            {
                path: 'announcements',
                name: 'OwnerAnnouncement',
                component: () => import('@/views/owner/OwnerAnnouncement.vue'),
                meta: { title: '社区公告' }
            },

            {
                path: 'facility',
                component: () => import('@/views/owner/OwnerFacility.vue'),
                meta: { title: '设施借用' }
            },
            {
                path: 'feedback',
                component: () => import('@/views/owner/OwnerFeedback.vue'),
                meta: { title: '留言反馈' }
            },
            {
                path: 'access-card',
                component: () => import('@/views/owner/OwnerAccessCard.vue'),
                meta: { title: '门禁卡' }
            },
            {
                path: 'profile',
                component: () => import('@/views/owner/OwnerProfile.vue'),
                meta: { title: '个人中心' }
            }
        ]
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/admin/Layout.vue'),
        meta: { requiresAuth: true }, // 需要登录
        children: [
            {
                path: '', // 默认跳转
                redirect: '/admin/dashboard'
            },
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/admin/Dashboard.vue'),
                meta: { title: '数据概览' }
            },
            {
                path: 'users',
                name: 'UserManage',
                component: () => import('@/views/admin/UserManage.vue'),
                meta: { title: '用户管理' }
            },
            {
                path: 'fee',
                name: 'FeeManage',
                component: () => import('@/views/admin/FeeManage.vue'),
                meta: { title: '费用管理' }
            },
            {
                path: 'building-house',
                name: 'BuildingHouseManage',
                component: () => import('@/views/admin/BuildingHouseManage.vue'),
                meta: { title: '楼栋房屋管理' }
            },
            {
                path: 'building-visual',
                name: 'BuildingVisual',
                component: () => import('@/views/admin/BuildingVisual.vue'),
                meta: { title: '楼栋平面图' }
            },
            {   path: 'repair',
                component: () => import('@/views/admin/RepairManage.vue'),
                meta: { title: '报修管理' }
            },
            {
                path: 'parking',
                name: 'AdminParkingManage',
                component: () => import('@/views/admin/AdminParkingManage.vue'),
                meta: { title: '车位管理' }
            },
            {
                path: 'announcement',
                name: 'AnnouncementManage',
                component: () => import('@/views/admin/AnnouncementManage.vue'),
                meta: { title: '公告管理' }
            },
            {
                path: 'facility',
                component: () => import('@/views/admin/AdminFacilityManage.vue'),
                meta: { title: '设施借用管理' }
            },
            {
                path: 'feedback',
                name: 'FeedbackManage',
                component: () => import('@/views/admin/AdminFeedbackManage.vue'),
                meta: { title: '留言反馈管理' }
            },
            {
                path: 'access-card',
                name: 'AccessCardManage',
                component: () => import('@/views/admin/AdminAccessCardManage.vue'),
                meta: { title: '门禁卡管理' }
            },
            {
                path: 'access-log',
                name: 'AccessLog',
                component: () => import('@/views/admin/AdminAccessLog.vue'),
                meta: { title: '进出记录追踪' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const token = sessionStorage.getItem('token')
    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router
