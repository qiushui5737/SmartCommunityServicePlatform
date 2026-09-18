<template>
  <div class="owner-home">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
      <div class="welcome-content">
        <p class="welcome-greeting">欢迎回来</p>
        <h1 class="welcome-title">{{ userInfo?.realName || '业主' }}</h1>
        <p class="welcome-desc">智慧社区服务中心 · 让社区生活更便利</p>
      </div>
      <div class="welcome-illustration">
        <div class="illustration-ring">
          <el-icon :size="56" color="rgba(255,255,255,0.9)"><HomeFilled /></el-icon>
        </div>
      </div>
    </div>

    <!-- 主内容双栏：左侧公告+消息+统计 / 右侧个人信息+快捷入口 -->
    <div class="home-body">
      <div class="home-main">
        <!-- 公告区域 -->
        <el-card class="announcement-card" shadow="never" v-loading="announcementLoading">
          <template #header>
            <div class="announcement-header">
              <div class="announcement-title">
                <el-icon><Bell /></el-icon>
                <span>最新公告</span>
              </div>
              <el-link type="primary" @click="$router.push('/owner/announcements')">查看全部</el-link>
            </div>
          </template>
          <div class="announcement-list" v-if="announcements.length > 0">
            <div v-for="item in announcements" :key="item.id" class="announcement-item">
              <div class="announcement-dot" />
              <div class="announcement-content">
                <div class="announcement-text">
                  <el-tag v-if="item.isTop" size="small" type="danger" effect="light" style="margin-right: 6px;">置顶</el-tag>
                  <el-tag size="small" :type="typeTagMap[item.type]?.type || 'info'" effect="plain" style="margin-right: 6px;">{{ typeTagMap[item.type]?.text || item.type }}</el-tag>
                  {{ item.title }}
                </div>
                <div class="announcement-date">{{ formatDate(item.createTime) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else-if="!announcementLoading" description="暂无公告" :image-size="60" />
        </el-card>

        <!-- 消息提醒条（横向） -->
        <div class="notification-bar" v-loading="notificationLoading">
          <div class="notification-bar-header">
            <div class="notification-bar-title">
              <el-icon><Bell /></el-icon>
              <span>消息提醒</span>
              <el-badge :value="notifications.length" :hidden="notifications.length === 0" :max="99" />
            </div>
            <span v-if="notifications.length > 0" class="notification-bar-count">{{ notifications.length }} 条</span>
          </div>
          <div v-if="notifications.length > 0" class="notification-grid">
            <div v-for="(n, idx) in notifications.slice(0, 15)" :key="idx" class="notif-chip" @click="$router.push(n.link)">
              <div class="notif-chip-icon" :class="'level-' + n.level">
                <el-icon :size="15"><component :is="notificationIcon(n.type)" /></el-icon>
              </div>
              <div class="notif-chip-body">
                <div class="notif-chip-title">{{ n.title }}</div>
                <div class="notif-chip-msg">{{ n.message }}</div>
              </div>
              <div class="notif-chip-time">{{ formatRelativeTime(n.time) }}</div>
            </div>
          </div>
          <div v-else-if="!notificationLoading" class="notification-bar-empty">
            <el-icon :size="20" color="#d1d5db"><ChatDotSquare /></el-icon>
            <span>暂无新消息</span>
          </div>
        </div>

        <!-- 快捷统计 -->
        <div class="stats-grid">
          <div v-for="stat in stats" :key="stat.label" class="stat-card">
            <div class="stat-icon" :style="{ background: stat.bg, color: stat.color }">
              <el-icon :size="24"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
            <div class="stat-accent" :style="{ background: stat.color }"></div>
          </div>
        </div>
      </div>

      <!-- 右侧个人信息 -->
      <aside class="home-side">
        <div class="profile-card">
          <div class="profile-avatar-wrap">
            <el-avatar :size="56" class="profile-avatar">
              <el-icon :size="26"><User /></el-icon>
            </el-avatar>
          </div>
          <h3 class="profile-name">{{ userInfo?.realName || '业主' }}</h3>
          <p class="profile-username">{{ userInfo?.username }}</p>
          <div class="profile-divider"></div>
          <div class="profile-info-list">
            <div class="profile-info-row">
              <el-icon :size="15"><Phone /></el-icon>
              <span>{{ userInfo?.phone || '未设置' }}</span>
            </div>
            <template v-if="profileData.houses?.length">
              <div class="profile-info-row" v-for="(h, i) in profileData.houses.slice(0, 3)" :key="i">
                <el-icon :size="15"><OfficeBuilding /></el-icon>
                <span>{{ h.buildingName }} {{ h.unitName }}·{{ h.roomNo }}</span>
              </div>
              <div v-if="profileData.houses.length > 3" class="profile-info-row profile-more-houses">
                <span>还有 {{ profileData.houses.length - 3 }} 套房产...</span>
              </div>
            </template>
            <div class="profile-info-row" v-else>
              <el-icon :size="15"><OfficeBuilding /></el-icon>
              <span>未绑定住房</span>
            </div>
          </div>
          <el-button class="profile-link-btn" text type="primary" @click="$router.push('/owner/profile')">
            查看完整资料 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <!-- 快捷入口（紧凑版） -->
        <div class="side-quick">
          <h4 class="side-quick-title">快捷服务</h4>
          <div class="side-quick-grid">
            <div v-for="card in quickLinks" :key="card.title" class="side-quick-item" @click="$router.push(card.path)">
              <div class="side-quick-icon" :style="{ background: card.bg, color: card.color }">
                <el-icon :size="20"><component :is="card.icon" /></el-icon>
              </div>
              <span class="side-quick-label">{{ card.title }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { HomeFilled, Money, Tools, Bell, ArrowRight, Calendar, Document, CreditCard, Van, User, Phone, OfficeBuilding, House, ChatDotSquare, Memo, Finished, CircleClose, Promotion } from '@element-plus/icons-vue'
import { getAnnouncementPage } from '@/api/announcement'
import request from '@/utils/request'

const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || '{}'))

// ========== 个人中心数据 ==========
const profileData = ref({ buildingName: '', unitName: '', roomNo: '', houses: [] })

const fetchProfile = async () => {
  try {
    const res = await request.get('/users/owner/profile')
    const d = res.data || {}
    profileData.value = {
      buildingName: d.buildingName || '',
      unitName: d.unitName || '',
      roomNo: d.roomNo || '',
      houses: d.houses || []
    }
    // 用后端数据补全 sessionStorage 中可能缺失的字段
    if (d.user) {
      userInfo.value = { ...userInfo.value, ...d.user }
    }
  } catch (e) {
    console.error('获取个人信息失败', e)
  }
}

// 公告类型标签映射
const typeTagMap = {
  NOTICE: { text: '通知', type: 'primary' },
  ACTIVITY: { text: '活动', type: 'success' },
  MAINTENANCE: { text: '维护', type: 'warning' },
  OTHER: { text: '其他', type: 'info' }
}

const quickLinks = [
  { title: '在线缴费', desc: '查看并缴纳物业费', icon: 'Money', bg: '#dbeafe', color: '#2563eb', path: '/owner/bills' },
  { title: '设施报修', desc: '提交家庭/公共报修', icon: 'Tools', bg: '#fef3c7', color: '#d97706', path: '/owner/repairs' },
  { title: '社区公告', desc: '浏览物业最新通知', icon: 'Bell', bg: '#d1fae5', color: '#059669', path: '/owner/announcements' },
  { title: '车位选购', desc: '查看可购车位信息', icon: 'Van', bg: '#ede9fe', color: '#7c3aed', path: '/owner/parking' }
]

// ========== 公告数据（对接 API） ==========
const announcements = ref([])
const announcementLoading = ref(false)

const fetchAnnouncements = async () => {
  announcementLoading.value = true
  try {
    const res = await getAnnouncementPage({ current: 1, size: 3 })
    announcements.value = res.data?.records || []
  } catch (e) {
    console.error('获取公告失败', e)
  } finally {
    announcementLoading.value = false
  }
}

// ========== 统计数据（对接 API） ==========
const stats = ref([
  { label: '待缴账单', value: '加载中...', icon: 'CreditCard', bg: '#fee2e2', color: '#ef4444' },
  { label: '报修中', value: '加载中...', icon: 'Tools', bg: '#dbeafe', color: '#3b82f6' },
  { label: '已购车位', value: '加载中...', icon: 'Van', bg: '#ede9fe', color: '#7c3aed' }
])

const fetchStats = async () => {
  try {
    // 并行请求三个统计接口
    const [billsRes, repairsRes, parkingRes] = await Promise.all([
      request.get('/owner/bills', { params: { status: 'PENDING' } }),
      request.get('/repair/page', { params: { current: 1, size: 1, status: 'PENDING' } }),
      request.get('/parking/my')
    ])
    const pendingBills = billsRes.data || []
    const pendingRepairs = repairsRes.data?.total ?? 0
    const myParking = parkingRes.data || []

    stats.value = [
      { label: '待缴账单', value: `${pendingBills.length} 笔`, icon: 'CreditCard', bg: '#fee2e2', color: '#ef4444' },
      { label: '报修中', value: `${pendingRepairs} 项`, icon: 'Tools', bg: '#dbeafe', color: '#3b82f6' },
      { label: '已购车位', value: `${myParking.length} 个`, icon: 'Van', bg: '#ede9fe', color: '#7c3aed' }
    ]
  } catch (e) {
    console.error('获取统计失败', e)
    stats.value = [
      { label: '待缴账单', value: '-', icon: 'CreditCard', bg: '#fee2e2', color: '#ef4444' },
      { label: '报修中', value: '-', icon: 'Tools', bg: '#dbeafe', color: '#3b82f6' },
      { label: '已购车位', value: '-', icon: 'Van', bg: '#ede9fe', color: '#7c3aed' }
    ]
  }
}

// ========== 消息通知 ==========
const notifications = ref([])
const notificationLoading = ref(false)
const showAllNotifications = ref(false)

const notificationIcon = (type) => {
  const map = { BILL: 'CreditCard', REPAIR: 'Tools', ANNOUNCEMENT: 'Promotion', FEEDBACK: 'ChatDotSquare', BOOKING: 'Memo' }
  return map[type] || 'Bell'
}

const formatRelativeTime = (timeStr) => {
  if (!timeStr) return ''
  const now = new Date()
  const t = new Date(timeStr)
  const diffMs = now - t
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffH = Math.floor(diffMin / 60)
  if (diffH < 24) return `${diffH}小时前`
  const diffD = Math.floor(diffH / 24)
  if (diffD < 7) return `${diffD}天前`
  return `${t.getMonth() + 1}月${t.getDate()}日`
}

const fetchNotifications = async () => {
  notificationLoading.value = true
  try {
    const res = await request.get('/users/owner/notifications')
    notifications.value = res.data?.notifications || []
  } catch (e) {
    console.error('获取消息通知失败', e)
  } finally {
    notificationLoading.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  fetchAnnouncements()
  fetchStats()
  fetchProfile()
  fetchNotifications()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

/* ====== Hero Section ====== */
.owner-home {
  .welcome-section {
    background: linear-gradient(135deg, #0f766e 0%, #115e59 60%, #134e4a 100%);
    border-radius: 20px;
    padding: 40px 48px;
    margin-bottom: 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: relative;
    overflow: hidden;

    .welcome-bg-shapes {
      position: absolute;
      inset: 0;
      pointer-events: none;

      .shape {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.04);
      }
      .shape-1 { width: 260px; height: 260px; top: -80px; right: -40px; }
      .shape-2 { width: 180px; height: 180px; bottom: -60px; right: 120px; background: rgba(255,255,255,0.03); }
      .shape-3 { width: 100px; height: 100px; top: 20px; right: 200px; background: rgba(255,255,255,0.05); }
    }

    .welcome-content {
      position: relative;
      z-index: 1;
    }

    .welcome-greeting {
      font-size: 13px;
      font-weight: 500;
      color: rgba(255, 255, 255, 0.6);
      letter-spacing: 0.04em;
      margin: 0 0 4px;
      text-transform: uppercase;
    }

    .welcome-title {
      font-size: 32px;
      font-weight: 700;
      color: white;
      margin: 0 0 8px;
      letter-spacing: -0.02em;
      line-height: 1.2;
    }

    .welcome-desc {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.65);
      margin: 0;
      letter-spacing: 0.01em;
    }

    .welcome-illustration {
      position: relative;
      z-index: 1;

      .illustration-ring {
        width: 96px;
        height: 96px;
        border-radius: 50%;
        border: 2px solid rgba(255, 255, 255, 0.15);
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(255, 255, 255, 0.06);
      }
    }
  }

  /* ====== Home Body: two-column ====== */
  .home-body {
    display: grid;
    grid-template-columns: 1fr 300px;
    gap: 20px;
    align-items: start;
  }

  /* ====== Announcement ====== */
  .announcement-card {
    margin-top: 0;

    :deep(.el-card__header) {
      padding: 16px 20px;
    }

    .announcement-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .announcement-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 700;
        font-size: 16px;
        color: $text-primary;

        .el-icon { color: $primary; }
      }
    }

    .announcement-list {
      .announcement-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 14px 0;
        border-bottom: 1px solid $border-color;

        &:last-child { border-bottom: none; }

        .announcement-dot {
          width: 8px;
          height: 8px;
          background: $primary;
          border-radius: 50%;
          margin-top: 6px;
          flex-shrink: 0;
        }

        .announcement-content {
          flex: 1;

          .announcement-text {
            font-size: 14px;
            color: $text-primary;
            font-weight: 500;
          }

          .announcement-date {
            font-size: 12px;
            color: $text-tertiary;
            margin-top: 4px;
          }
        }
      }
    }
  }

  /* ====== Stats Cards ====== */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    margin-top: 20px;
  }

  .stat-card {
    background: white;
    border-radius: 16px;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    border: 1px solid $border-color;
    transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
    position: relative;
    overflow: hidden;

    &:hover {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
      transform: translateY(-4px);
    }

    .stat-accent {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3px;
      opacity: 0;
      transition: opacity 0.35s;
    }

    &:hover .stat-accent {
      opacity: 1;
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-info {
      .stat-value {
        font-size: 26px;
        font-weight: 700;
        color: $text-primary;
        letter-spacing: -0.02em;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }
  }

  /* ====== Profile Side Card ====== */
  .home-side {
    position: sticky;
    top: 24px;
  }

  .profile-card {
    background: white;
    border-radius: 16px;
    border: 1px solid $border-color;
    padding: 28px 20px 20px;
    text-align: center;

    .profile-avatar-wrap {
      margin-bottom: 14px;

      .profile-avatar {
        background: linear-gradient(135deg, #0d9488, #14b8a6);
        color: white;
      }
    }

    .profile-name {
      font-size: 18px;
      font-weight: 700;
      color: $text-primary;
      margin: 0 0 4px;
    }

    .profile-username {
      font-size: 13px;
      color: $text-tertiary;
      font-family: $font-mono;
      margin: 0 0 16px;
    }

    .profile-divider {
      height: 1px;
      background: $border-color-light;
      margin: 0 -20px 16px;
    }

    .profile-info-list {
      text-align: left;
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 18px;

      .profile-info-row {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 13px;
        color: $text-secondary;

        .el-icon {
          color: $text-tertiary;
          flex-shrink: 0;
        }
      }

      .profile-more-houses {
        font-size: 12px;
        color: $text-tertiary;
        font-style: italic;
      }
    }

    .profile-link-btn {
      font-size: 13px;
      font-weight: 500;
      padding: 0;
    }
  }

  /* ====== Side Quick Links ====== */
  .side-quick {
    background: white;
    border-radius: 16px;
    border: 1px solid $border-color;
    padding: 16px 18px 14px;
    margin-top: 14px;

    .side-quick-title {
      font-size: 14px;
      font-weight: 700;
      color: $text-primary;
      margin: 0 0 12px;
    }

    .side-quick-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }

    .side-quick-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      border-radius: 10px;
      cursor: pointer;
      background: #f9fafb;
      border: 1px solid transparent;
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

      &:hover {
        background: white;
        border-color: #0d9488;
        box-shadow: 0 2px 6px rgba(13, 148, 136, 0.1);
        transform: translateY(-1px);
      }

      .side-quick-icon {
        width: 34px;
        height: 34px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .side-quick-label {
        font-size: 13px;
        font-weight: 600;
        color: $text-primary;
      }
    }
  }

  /* ====== Notification Horizontal Bar ====== */
  .notification-bar {
    background: white;
    border-radius: 16px;
    border: 1px solid $border-color;
    padding: 16px 20px 14px;
    margin-top: 20px;

    .notification-bar-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .notification-bar-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: 700;
        font-size: 15px;
        color: $text-primary;

        .el-icon { color: #0d9488; }

        :deep(.el-badge) {
          margin-left: 2px;
          .el-badge__content { background-color: #ef4444; }
        }
      }

      .notification-bar-count {
        font-size: 12px;
        color: $text-tertiary;
      }
    }

    .notification-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 10px;
    }

    .notif-chip {
      background: #f9fafb;
      border: 1px solid $border-color-light;
      border-radius: 10px;
      padding: 10px 12px;
      cursor: pointer;
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      display: flex;
      flex-direction: column;
      gap: 6px;
      min-width: 0;

      &:hover {
        background: white;
        border-color: #0d9488;
        box-shadow: 0 2px 8px rgba(13, 148, 136, 0.12);
        transform: translateY(-2px);
      }

      .notif-chip-icon {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        &.level-danger  { background: #fee2e2; color: #ef4444; }
        &.level-warning { background: #fef3c7; color: #d97706; }
        &.level-success { background: #d1fae5; color: #059669; }
        &.level-info    { background: #dbeafe; color: #3b82f6; }
      }

      .notif-chip-body {
        flex: 1;
        min-width: 0;

        .notif-chip-title {
          font-size: 13px;
          font-weight: 600;
          color: $text-primary;
          line-height: 1.3;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .notif-chip-msg {
          font-size: 12px;
          color: $text-secondary;
          margin-top: 2px;
          line-height: 1.4;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .notif-chip-time {
        font-size: 11px;
        color: $text-tertiary;
      }
    }

    .notification-bar-empty {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 16px 0 8px;
      font-size: 13px;
      color: $text-tertiary;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .owner-home .side-quick-item {
    transition: none;
  }
}

@media (max-width: 900px) {
  .owner-home {
    .stats-grid { grid-template-columns: 1fr; }
    .home-body { grid-template-columns: 1fr; }
    .notification-grid { grid-template-columns: repeat(2, 1fr); }
  }
}
</style>
