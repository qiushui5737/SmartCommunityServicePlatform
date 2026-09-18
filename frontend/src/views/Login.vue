<template>
  <div class="auth-page">
    <!-- ===== SVG Architectural Blueprint ===== -->
    <svg class="blueprint" viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <!-- Outer boundary -->
      <path class="bp bp-1" d="M180,120 L1200,120 L1200,860 L180,860 Z" fill="none" stroke="rgba(255,255,255,0.14)" stroke-width="1.5"/>
      <!-- Main roads -->
      <path class="bp bp-2" d="M690,120 L690,860" fill="none" stroke="rgba(255,255,255,0.10)" stroke-width="1" stroke-dasharray="12,6"/>
      <path class="bp bp-2" d="M180,490 L1200,490" fill="none" stroke="rgba(255,255,255,0.10)" stroke-width="1" stroke-dasharray="12,6"/>
      <!-- Secondary paths -->
      <path class="bp bp-3" d="M420,120 L420,490" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
      <path class="bp bp-3" d="M950,490 L950,860" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="0.8"/>
      <!-- Building footprints -->
      <rect class="bp bp-4" x="240" y="200" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-5" x="240" y="340" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-4" x="490" y="200" width="140" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-5" x="490" y="340" width="140" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-6" x="750" y="200" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-7" x="750" y="340" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-4" x="1020" y="200" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-5" x="1020" y="340" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-6" x="240" y="560" width="130" height="90" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-7" x="490" y="560" width="130" height="90" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-4" x="750" y="560" width="120" height="90" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-5" x="1020" y="560" width="120" height="90" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-6" x="240" y="720" width="130" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <rect class="bp bp-7" x="750" y="720" width="120" height="80" rx="3" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
      <!-- Entrance arc -->
      <path class="bp bp-3" d="M640,860 Q690,810 740,860" fill="none" stroke="rgba(255,255,255,0.10)" stroke-width="1"/>
      <!-- Crosshair markers at intersections -->
      <g class="bp-node" style="--nd:1.8s">
        <circle cx="690" cy="490" r="3.5" fill="rgba(94,234,212,0.5)"/>
        <circle class="pulse" cx="690" cy="490" r="8" fill="none" stroke="rgba(94,234,212,0.4)" stroke-width="1.2"/>
      </g>
      <g class="bp-node" style="--nd:2.2s">
        <circle cx="420" cy="490" r="3.5" fill="rgba(94,234,212,0.45)"/>
        <circle class="pulse" cx="420" cy="490" r="8" fill="none" stroke="rgba(94,234,212,0.35)" stroke-width="1.2"/>
      </g>
      <g class="bp-node" style="--nd:2.6s">
        <circle cx="950" cy="490" r="3.5" fill="rgba(94,234,212,0.45)"/>
        <circle class="pulse" cx="950" cy="490" r="8" fill="none" stroke="rgba(94,234,212,0.35)" stroke-width="1.2"/>
      </g>
      <g class="bp-node" style="--nd:3s">
        <circle cx="690" cy="120" r="3" fill="rgba(94,234,212,0.4)"/>
        <circle class="pulse" cx="690" cy="120" r="7" fill="none" stroke="rgba(94,234,212,0.3)" stroke-width="1.2"/>
      </g>
      <!-- Dimension labels -->
      <text class="bp bp-8 dim" x="440" y="110" fill="rgba(255,255,255,0.12)" font-size="11" font-family="ui-monospace, monospace">240m</text>
      <text class="bp bp-8 dim" x="170" y="485" fill="rgba(255,255,255,0.12)" font-size="11" font-family="ui-monospace, monospace">180m</text>
      <text class="bp bp-9 dim" x="1210" y="500" fill="rgba(255,255,255,0.1)" font-size="10" font-family="ui-monospace, monospace" transform="rotate(90,1210,500)">SITE PLAN 1:500</text>
    </svg>

    <!-- ===== Full-screen Showcase Background ===== -->
    <div class="showcase">
      <!-- Brand -->
      <div class="showcase-brand">
        <div class="brand-mark"><el-icon :size="22"><HomeFilled /></el-icon></div>
        <span class="brand-label">智慧社区</span>
      </div>

      <!-- Hero content -->
      <div class="showcase-hero">
        <h1 class="hero-headline">让社区生活<br/>更智慧、更温暖</h1>
        <p class="hero-sub">一站式社区服务平台 · 连接物业与每一位业主</p>

        <div class="feature-row">
          <div v-for="(feat, i) in features" :key="feat.title" class="feature-card" :style="{ '--delay': i * 120 + 'ms' }">
            <div class="feat-icon" :style="{ background: feat.bg }">
              <el-icon :size="22"><component :is="feat.icon" /></el-icon>
            </div>
            <div class="feat-title">{{ feat.title }}</div>
            <div class="feat-desc">{{ feat.desc }}</div>
          </div>
        </div>
      </div>

      <!-- Bottom stats -->
      <div class="showcase-bottom">
        <div class="stat-item" v-for="s in stats" :key="s.label">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-lbl">{{ s.label }}</div>
        </div>
        <div class="bottom-copy">智慧社区管理系统 v1.0</div>
      </div>
    </div>

    <!-- ===== Floating Auth Card — Top Right ===== -->
    <div class="auth-card">
      <div class="auth-card-head">
        <h2 class="auth-title">欢迎回来</h2>
        <p class="auth-sub">登录以继续使用</p>
      </div>
      <el-form :model="form" @submit.prevent="handleLogin" class="auth-form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" @click="handleLogin" class="auth-btn" :loading="loading">
          <span class="btn-ripple"></span>登录
        </el-button>
        <div class="auth-links">
          <el-link type="primary" @click="toggleRole">切换为{{ isOwner ? '管理员' : '业主' }}登录</el-link>
          <el-link type="info" @click="$router.push('/register')">没有账号？立即注册</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const isOwner = ref(true)
const loading = ref(false)
const form = ref({ username: 'testuser', password: '123456' })

const features = [
  { title: '在线缴费', desc: '账单一键缴纳', icon: 'Money', bg: 'rgba(37,99,235,0.18)' },
  { title: '设施报修', desc: '拍照实时跟踪', icon: 'Tools', bg: 'rgba(245,158,11,0.18)' },
  { title: '社区公告', desc: '通知即时触达', icon: 'Bell', bg: 'rgba(16,185,129,0.18)' },
  { title: '设施借用', desc: '在线申请借用', icon: 'Suitcase', bg: 'rgba(139,92,246,0.18)' }
]

const stats = [
  { value: '2,400+', label: '服务住户' },
  { value: '12 栋', label: '社区楼栋' },
  { value: '98.5%', label: '满意度' }
]

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const payload = { username: form.value.username, password: form.value.password, role: isOwner.value ? 'OWNER' : 'ADMIN' }
    const res = await request.post('/auth/login', payload)
    if (!res || !res.data || !res.data.token || !res.data.user) { ElMessage.error('登录响应异常'); return }
    sessionStorage.setItem('token', res.data.token)
    sessionStorage.setItem('userInfo', JSON.stringify(res.data.user))
    ElMessage.success('登录成功')
    const role = res.data.user.role
    await router.push((role === 'ADMIN' || role === 'STAFF') ? '/admin' : '/owner')
  } catch (e) {} finally { loading.value = false }
}

const toggleRole = () => { isOwner.value = !isOwner.value; form.value.password = '' }
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

/* ==============================
   PAGE CONTAINER
   ============================== */
.auth-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

/* ==============================
   SVG BLUEPRINT OVERLAY
   ============================== */
.blueprint {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

/* Stroke draw animation */
.bp {
  stroke-dasharray: 4000;
  stroke-dashoffset: 4000;
  animation: draw-line 2.8s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}
.bp-1 { animation-delay: 0.1s; }
.bp-2 { animation-delay: 0.3s; }
.bp-3 { animation-delay: 0.6s; }
.bp-4 { animation-delay: 0.9s; }
.bp-5 { animation-delay: 1.1s; }
.bp-6 { animation-delay: 1.3s; }
.bp-7 { animation-delay: 1.5s; }
.bp-8 { animation-delay: 2.0s; }
.bp-9 { animation-delay: 2.3s; }

@keyframes draw-line {
  to { stroke-dashoffset: 0; }
}

/* Dimension text fade */
.dim {
  opacity: 0;
  animation: dim-in 0.8s ease forwards;
}
@keyframes dim-in {
  to { opacity: 1; }
}

/* Breathing node pulse */
.bp-node {
  opacity: 0;
  animation: node-in 0.6s ease forwards;
  animation-delay: var(--nd, 2s);
}
@keyframes node-in {
  to { opacity: 1; }
}

.pulse {
  transform-box: fill-box;
  transform-origin: center;
  animation: pulse-breathe 3.5s ease-in-out infinite;
}
@keyframes pulse-breathe {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(2.5); opacity: 0; }
}

/* ==============================
   FULL-SCREEN SHOWCASE
   ============================== */
.showcase {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #0f766e 0%, #115e59 40%, #134e4a 100%);
  display: flex;
  flex-direction: column;
  padding: 36px 56px;
  color: white;
  z-index: 0;
}

.showcase-brand {
  display: flex; align-items: center; gap: 12px; position: relative; z-index: 2;
  opacity: 0;
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s forwards;
  .brand-mark {
    width: 40px; height: 40px; border-radius: 10px;
    background: rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center;
    border: 1px solid rgba(255,255,255,0.08);
    animation: brand-pulse 4s ease-in-out infinite;
  }
  .brand-label { font-size: 16px; font-weight: 700; letter-spacing: 0.02em; }
}

@keyframes brand-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(94,234,212,0); }
  50% { box-shadow: 0 0 0 6px rgba(94,234,212,0.08); }
}

.showcase-hero {
  flex: 1; display: flex; flex-direction: column; justify-content: center;
  position: relative; z-index: 2; max-width: 560px;
  opacity: 0;
  animation: fade-up 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.6s forwards;
}

.hero-headline {
  font-size: 44px; font-weight: 700; line-height: 1.25; letter-spacing: -0.03em;
  margin: 0 0 14px; color: white; text-wrap: balance;
}
.hero-sub {
  font-size: 15px; color: rgba(255,255,255,0.5); margin: 0 0 44px; letter-spacing: 0.01em;
}

.feature-row {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px;
}
.feature-card {
  padding: 16px 18px; background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.06); border-radius: 14px;
  opacity: 0;
  animation: card-in 0.5s cubic-bezier(0.16,1,0.3,1) forwards;
  animation-delay: calc(1200ms + var(--delay, 0ms));
  transition: background 0.25s, border-color 0.25s;
  &:hover { background: rgba(255,255,255,0.1); border-color: rgba(255,255,255,0.12); }
  .feat-icon {
    width: 40px; height: 40px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: white; margin-bottom: 10px;
  }
  .feat-title { font-size: 15px; font-weight: 700; color: white; margin-bottom: 2px; }
  .feat-desc { font-size: 12px; color: rgba(255,255,255,0.4); }
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.showcase-bottom {
  display: flex; align-items: flex-end; justify-content: space-between;
  position: relative; z-index: 2;
  padding-top: 28px; border-top: 1px solid rgba(255,255,255,0.08);
  opacity: 0;
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 1.8s forwards;
  .stat-item {
    .stat-num { font-size: 26px; font-weight: 700; letter-spacing: -0.02em; }
    .stat-lbl { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 2px; font-weight: 500; }
  }
  .bottom-copy { font-size: 12px; color: rgba(255,255,255,0.25); }
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ==============================
   FLOATING AUTH CARD
   ============================== */
.auth-card {
  position: absolute;
  top: 32px;
  right: 32px;
  z-index: 10;
  width: 340px;
  background: white;
  border-radius: 18px;
  padding: 28px 26px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25), 0 0 0 1px rgba(255,255,255,0.1);
  opacity: 0;
  transform: translateX(24px);
  animation: card-slide 0.65s cubic-bezier(0.16, 1, 0.3, 1) 0.5s forwards;
}

@keyframes card-slide {
  to { opacity: 1; transform: translateX(0); }
}

.auth-card-head {
  margin-bottom: 20px;
  .auth-title { font-size: 22px; font-weight: 700; color: $text-primary; margin: 0 0 4px; letter-spacing: -0.02em; }
  .auth-sub { font-size: 13px; color: $text-secondary; margin: 0; }
}

.auth-form {
  :deep(.el-form-item) { margin-bottom: 14px; }
  :deep(.el-input__wrapper) {
    border-radius: 10px !important; padding: 4px 14px !important;
    box-shadow: 0 0 0 1px $border-color inset !important;
    transition: box-shadow 0.3s, background 0.3s !important;
    &:hover { box-shadow: 0 0 0 1px #94a3b8 inset !important; }
    &.is-focus {
      box-shadow: 0 0 0 1.5px #0d9488 inset, 0 0 0 4px rgba(13,148,136,0.08) !important;
    }
  }
  :deep(.el-input__inner) { height: 42px; font-size: 14px; }
  :deep(.el-input__prefix) { color: $text-tertiary; margin-right: 8px; }
}

.auth-btn {
  position: relative;
  width: 100%; height: 44px; border-radius: 10px !important;
  font-size: 15px; font-weight: 700; margin-top: 4px;
  background: #0d9488 !important; border: none !important;
  box-shadow: 0 2px 8px rgba(13,148,136,0.3) !important;
  transition: all 0.25s cubic-bezier(0.16,1,0.3,1) !important;
  overflow: hidden;
  &:hover {
    background: #0f766e !important;
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(13,148,136,0.4) !important;
  }
  &:active {
    transform: translateY(0) scale(0.98);
  }
  .btn-ripple {
    position: absolute; inset: 0;
    background: radial-gradient(circle at 50% 50%, rgba(255,255,255,0.3) 0%, transparent 60%);
    opacity: 0;
    transition: opacity 0.3s;
    pointer-events: none;
  }
  &:active .btn-ripple {
    opacity: 1;
    animation: ripple-out 0.5s ease-out;
  }
}

@keyframes ripple-out {
  0% { transform: scale(0.5); opacity: 1; }
  100% { transform: scale(2); opacity: 0; }
}

.auth-links {
  display: flex; justify-content: space-between; margin-top: 14px;
  :deep(.el-link) { font-size: 12px; }
}

/* ==============================
   RESPONSIVE
   ============================== */
@media (max-width: 800px) {
  .showcase { padding: 24px; }
  .hero-headline { font-size: 28px; }
  .feature-row { grid-template-columns: 1fr; }
  .auth-card {
    position: relative; top: auto; right: auto;
    width: auto; margin: 24px;
    box-shadow: 0 8px 30px rgba(0,0,0,0.2);
  }
  .auth-page { display: flex; flex-direction: column; }
  .showcase { position: relative; min-height: 60vh; }
  .blueprint { display: none; }
}

/* ==============================
   REDUCED MOTION
   ============================== */
@media (prefers-reduced-motion: reduce) {
  .bp { animation: none; stroke-dashoffset: 0; }
  .dim { animation: none; opacity: 1; }
  .bp-node { animation: none; opacity: 1; }
  .pulse { animation: none; }
  .showcase-brand, .showcase-hero, .showcase-bottom { animation: none; opacity: 1; }
  .showcase-brand .brand-mark { animation: none; }
  .feature-card { animation: none; opacity: 1; }
  .auth-card { animation: none; opacity: 1; transform: none; }
}
</style>
