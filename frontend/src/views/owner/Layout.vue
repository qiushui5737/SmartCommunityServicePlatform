<template>
  <el-container class="owner-layout" direction="vertical">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="nav-left">
        <div class="brand">
          <div class="brand-icon">
            <el-icon :size="18"><HomeFilled /></el-icon>
          </div>
          <span class="brand-text">业主服务</span>
        </div>
      </div>

      <nav class="nav-center">
        <el-menu
          mode="horizontal"
          router
          :default-active="$route.path"
          class="nav-menu"
          :ellipsis="false"
        >
          <el-menu-item index="/owner/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-sub-menu index="group-life">
            <template #title>
              <el-icon><ShoppingCart /></el-icon>
              <span>生活服务</span>
            </template>
            <el-menu-item index="/owner/bills">
              <el-icon><Money /></el-icon>在线缴费
            </el-menu-item>
            <el-menu-item index="/owner/repairs">
              <el-icon><Tools /></el-icon>设施报修
            </el-menu-item>
            <el-menu-item index="/owner/parking">
              <el-icon><Van /></el-icon>车位管理
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="group-community">
            <template #title>
              <el-icon><Bell /></el-icon>
              <span>社区互动</span>
            </template>
            <el-menu-item index="/owner/announcements">
              <el-icon><Bell /></el-icon>社区公告
            </el-menu-item>

            <el-menu-item index="/owner/facility">
              <el-icon><Suitcase /></el-icon>设施借用
            </el-menu-item>
            <el-menu-item index="/owner/feedback">
              <el-icon><ChatDotRound /></el-icon>留言反馈
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/owner/access-card">
            <el-icon><Key /></el-icon>
            <span>门禁卡</span>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="nav-right">
        <el-badge :value="2" class="nav-action">
          <el-icon :size="18"><Bell /></el-icon>
        </el-badge>
        <el-dropdown @command="handleCommand" trigger="click">
          <span class="user-trigger">
            <el-avatar :size="30" class="nav-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="nav-username">{{ userInfo?.realName || '业主' }}</span>
            <el-icon class="arrow-icon"><ArrowDownBold /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 页面标题栏 -->
    <div class="page-bar">
      <span class="page-group" v-if="currentGroup">{{ currentGroup }}</span>
      <span class="page-sep" v-if="currentGroup">/</span>
      <h1 class="page-title">{{ currentPageTitle }}</h1>
    </div>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }" :key="$route.path">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userInfo = ref(null)

onMounted(() => {
  userInfo.value = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
})

const groupMap = {
  '/owner/home': '',
  '/owner/bills': '生活服务',
  '/owner/repairs': '生活服务',
  '/owner/parking': '生活服务',
  '/owner/announcements': '社区互动',
  '/owner/facility': '社区互动',
  '/owner/feedback': '社区互动',
  '/owner/access-card': '',
  '/owner/profile': ''
}

const currentGroup = computed(() => groupMap[route.path] || '')

const currentPageTitle = computed(() => {
  const titles = {
    '/owner/home': '业主首页',
    '/owner/bills': '在线缴费',
    '/owner/repairs': '设施报修',
    '/owner/parking': '车位管理',
    '/owner/announcements': '社区公告',
    '/owner/facility': '设施借用',
    '/owner/feedback': '留言反馈',
    '/owner/access-card': '门禁卡',
    '/owner/profile': '个人中心'
  }
  return titles[route.path] || '业主服务中心'
})

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    sessionStorage.clear()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/owner/profile')
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

$owner-primary: #0d9488;
$owner-light: #14b8a6;
$owner-50: #f0fdfa;

.owner-layout {
  height: 100vh;
  background: $bg-secondary;
}

/* ========== 顶部导航 ========== */
.top-nav {
  height: 56px;
  background: white;
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid $border-color;
  box-shadow: $shadow-sm;
  position: relative;
  z-index: 100;
  flex-shrink: 0;
}

.nav-left {
  flex-shrink: 0;
  margin-right: 32px;

  .brand {
    display: flex;
    align-items: center;
    gap: 10px;

    .brand-icon {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: linear-gradient(135deg, $owner-primary, $owner-light);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      flex-shrink: 0;
    }

    .brand-text {
      font-size: 16px;
      font-weight: 700;
      color: $text-primary;
      white-space: nowrap;
      letter-spacing: -0.02em;
    }
  }
}

.nav-center {
  flex: 1;
  overflow: hidden;

  .nav-menu {
    background: transparent !important;
    border-bottom: none !important;
    height: 56px;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      height: 56px !important;
      line-height: 56px !important;
      border-bottom: 2px solid transparent !important;
      font-size: 13.5px;
      font-weight: 500;
      color: $text-secondary;
      transition: color 0.2s ease, background-color 0.2s ease;
      padding: 0 16px;
      margin: 0 !important;
      border-radius: 0 !important;

      .el-icon {
        font-size: 16px;
        margin-right: 4px;
      }

      &:hover {
        color: $owner-primary;
        background: $owner-50 !important;
      }
    }

    :deep(.el-menu-item.is-active) {
      color: $owner-primary !important;
      border-bottom-color: $owner-primary !important;
      font-weight: 600;
    }

    :deep(.el-sub-menu.is-active .el-sub-menu__title) {
      color: $owner-primary !important;
      border-bottom-color: $owner-primary !important;
      font-weight: 600;
    }

    /* 下拉菜单 */
    :deep(.el-sub-menu .el-menu) {
      min-width: 150px;
      border-radius: 10px;
      padding: 6px;
      box-shadow: $shadow-lg;
      border: 1px solid $border-color-light;

      .el-menu-item {
        height: 40px !important;
        line-height: 40px !important;
        border-radius: 8px;
        margin: 2px 0;
        font-size: 13px;
        font-weight: 500;
        color: $text-secondary;
        border-bottom: none !important;
        padding: 0 12px;

        .el-icon {
          font-size: 15px;
          margin-right: 6px;
          color: $text-tertiary;
        }

        &:hover {
          background: $owner-50 !important;
          color: $owner-primary;

          .el-icon { color: $owner-primary; }
        }

        &.is-active {
          background: $owner-50 !important;
          color: $owner-primary !important;
          font-weight: 600;

          .el-icon { color: $owner-primary; }
        }
      }
    }
  }
}

.nav-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 24px;

  .nav-action {
    cursor: pointer;
    padding: 6px;
    border-radius: 8px;
    color: $text-secondary;
    transition: all 0.2s;

    &:hover {
      color: $owner-primary;
      background: $owner-50;
    }
  }

  .user-trigger {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 10px;
    border-radius: 10px;
    transition: background 0.2s;

    &:hover { background: $gray-100; }

    .nav-avatar {
      background: linear-gradient(135deg, $owner-primary, $owner-light);
      color: white;
      flex-shrink: 0;
    }

    .nav-username {
      font-size: 13px;
      font-weight: 500;
      color: $text-primary;
    }

    .arrow-icon {
      font-size: 10px;
      color: $text-tertiary;
    }
  }
}

/* ========== 页面标题栏 ========== */
.page-bar {
  height: 44px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 28px;
  background: white;
  border-bottom: 1px solid $border-color-light;
  flex-shrink: 0;

  .page-group {
    font-size: 13px;
    font-weight: 500;
    color: $text-tertiary;
  }

  .page-sep {
    font-size: 12px;
    color: $text-tertiary;
    user-select: none;
  }

  .page-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }
}

/* ========== 主内容 ========== */
.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* ========== 过渡 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ========== 响应式 ========== */
@media (prefers-reduced-motion: reduce) {
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
