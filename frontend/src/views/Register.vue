<template>
  <div class="auth-page">
    <!-- ===== Full-screen Community Showcase Background ===== -->
    <div class="showcase">
      <div class="showcase-decor">
        <div class="decor-circle c1"></div>
        <div class="decor-circle c2"></div>
        <div class="decor-circle c3"></div>
        <div class="decor-grid"></div>
      </div>

      <div class="showcase-brand">
        <div class="brand-mark"><el-icon :size="22"><HomeFilled /></el-icon></div>
        <span class="brand-label">智慧社区</span>
      </div>

      <div class="showcase-hero">
        <h1 class="hero-headline">加入我们<br/>开启智慧社区生活</h1>
        <p class="hero-sub">注册成为业主，享受全方位的社区服务</p>

        <div class="feature-row">
          <div v-for="(feat, i) in features" :key="feat.title" class="feature-card" :style="{ '--delay': i * 100 + 'ms' }">
            <div class="feat-icon" :style="{ background: feat.bg }">
              <el-icon :size="22"><component :is="feat.icon" /></el-icon>
            </div>
            <div class="feat-title">{{ feat.title }}</div>
            <div class="feat-desc">{{ feat.desc }}</div>
          </div>
        </div>
      </div>

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
        <h2 class="auth-title">业主注册</h2>
        <p class="auth-sub">创建您的社区服务账号</p>
      </div>
      <el-form :model="form" @submit.prevent="handleRegister" class="auth-form">
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
        <el-form-item>
          <el-input v-model="form.realName" placeholder="真实姓名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.phone" placeholder="手机号" size="large">
            <template #prefix><el-icon><Phone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" @click="handleRegister" class="auth-btn">注册</el-button>
        <div class="auth-links">
          <el-link type="primary" @click="$router.push('/login')">已有账号？返回登录</el-link>
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
const form = ref({ username: '', password: '', realName: '', phone: '' })

const features = [
  { title: '便捷缴费', desc: '物业停车费在线缴纳', icon: 'Money', bg: 'rgba(37,99,235,0.18)' },
  { title: '快速报修', desc: '拍照跟踪维修进度', icon: 'Tools', bg: 'rgba(245,158,11,0.18)' },
  { title: '社区动态', desc: '公告活动即时掌握', icon: 'Bell', bg: 'rgba(16,185,129,0.18)' },
  { title: '设施借用', desc: '公共设施在线申请', icon: 'Suitcase', bg: 'rgba(139,92,246,0.18)' }
]

const stats = [
  { value: '2,400+', label: '服务住户' },
  { value: '15 项', label: '社区服务' },
  { value: '7×24h', label: '在线响应' }
]

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请填写完整')
  try {
    await request.post('/auth/register', form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {}
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.auth-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
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
}

.showcase-decor {
  position: absolute; inset: 0; pointer-events: none;
  .decor-circle { position: absolute; border-radius: 50%; border: 1px solid rgba(255,255,255,0.06); }
  .c1 { width: 520px; height: 520px; top: -200px; right: -100px; }
  .c2 { width: 360px; height: 360px; bottom: -100px; left: -80px; border-color: rgba(255,255,255,0.04); }
  .c3 { width: 180px; height: 180px; top: 50%; left: 55%; border-color: rgba(255,255,255,0.07); }
  .decor-grid {
    position: absolute; inset: 0;
    background-image: linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
    background-size: 60px 60px;
  }
}

.showcase-brand {
  display: flex; align-items: center; gap: 12px; position: relative; z-index: 1;
  .brand-mark {
    width: 40px; height: 40px; border-radius: 10px;
    background: rgba(255,255,255,0.12); display: flex; align-items: center; justify-content: center;
    border: 1px solid rgba(255,255,255,0.08);
  }
  .brand-label { font-size: 16px; font-weight: 700; letter-spacing: 0.02em; }
}

.showcase-hero {
  flex: 1; display: flex; flex-direction: column; justify-content: center;
  position: relative; z-index: 1; max-width: 560px;
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
  padding: 16px 18px; background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.06); border-radius: 14px;
  animation: card-in 0.5s cubic-bezier(0.16,1,0.3,1) both;
  animation-delay: var(--delay, 0ms);
  transition: background 0.2s;
  &:hover { background: rgba(255,255,255,0.1); }
  .feat-icon {
    width: 40px; height: 40px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: white; margin-bottom: 10px;
  }
  .feat-title { font-size: 15px; font-weight: 700; color: white; margin-bottom: 2px; }
  .feat-desc { font-size: 12px; color: rgba(255,255,255,0.45); }
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.showcase-bottom {
  display: flex; align-items: flex-end; justify-content: space-between;
  position: relative; z-index: 1;
  padding-top: 28px; border-top: 1px solid rgba(255,255,255,0.08);
  .stat-item {
    .stat-num { font-size: 26px; font-weight: 700; letter-spacing: -0.02em; }
    .stat-lbl { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 2px; font-weight: 500; }
  }
  .bottom-copy { font-size: 12px; color: rgba(255,255,255,0.3); }
}

/* ==============================
   FLOATING AUTH CARD — top-right
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
}

.auth-card-head {
  margin-bottom: 18px;
  .auth-title { font-size: 22px; font-weight: 700; color: $text-primary; margin: 0 0 4px; letter-spacing: -0.02em; }
  .auth-sub { font-size: 13px; color: $text-secondary; margin: 0; }
}

.auth-form {
  :deep(.el-form-item) { margin-bottom: 12px; }
  :deep(.el-input__wrapper) {
    border-radius: 10px !important; padding: 4px 14px !important;
    box-shadow: 0 0 0 1px $border-color inset !important; transition: all 0.2s;
    &:hover, &.is-focus { box-shadow: 0 0 0 1px $primary inset !important; }
  }
  :deep(.el-input__inner) { height: 42px; font-size: 14px; }
  :deep(.el-input__prefix) { color: $text-tertiary; margin-right: 8px; }
}

.auth-btn {
  width: 100%; height: 44px; border-radius: 10px !important;
  font-size: 15px; font-weight: 700; margin-top: 4px;
  background: $primary !important; border: none !important;
  box-shadow: 0 2px 8px rgba(37,99,235,0.25) !important;
  transition: all 0.25s cubic-bezier(0.16,1,0.3,1) !important;
  &:hover { background: $primary-dark !important; transform: translateY(-1px); box-shadow: 0 4px 14px rgba(37,99,235,0.35) !important; }
}

.auth-links {
  display: flex; justify-content: center; margin-top: 14px;
  :deep(.el-link) { font-size: 12px; }
}

/* ==============================
   Responsive
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
}

@media (prefers-reduced-motion: reduce) {
  .feature-card { animation: none; }
}
</style>
