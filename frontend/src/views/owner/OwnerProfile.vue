<template>
  <div class="owner-profile" v-loading="loading">
    <div class="profile-grid">
      <!-- 左侧：个人信息 -->
      <div class="profile-main">
        <!-- 头像区 -->
        <div class="avatar-section">
          <div class="avatar-ring" @click="triggerAvatarUpload" title="点击更换头像">
            <el-avatar :size="80" :src="avatarFullUrl" class="profile-avatar">
              <el-icon :size="36"><User /></el-icon>
            </el-avatar>
            <div class="avatar-overlay"><el-icon :size="18"><Camera /></el-icon></div>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarChange" />
          <div class="avatar-info">
            <h2 class="profile-name">{{ profile.user?.realName || '-' }}</h2>
            <div class="profile-meta">
              <el-tag size="small" type="success" effect="light">{{ roleLabel }}</el-tag>
              <span class="meta-text">{{ profile.user?.username }}</span>
            </div>
          </div>
          <el-button class="edit-btn" type="primary" size="small" @click="openEditDialog">
            <el-icon><Edit /></el-icon>编辑资料
          </el-button>
        </div>

        <!-- 基本信息 -->
        <div class="info-card">
          <h3 class="card-title">基本信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ profile.user?.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">真实姓名</span>
              <span class="info-value">{{ profile.user?.realName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ profile.user?.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">电子邮箱</span>
              <span class="info-value">{{ profile.user?.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">性别</span>
              <span class="info-value">{{ profile.user?.gender || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">出生日期</span>
              <span class="info-value">{{ profile.user?.birthday || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">身份证号</span>
              <span class="info-value">{{ maskIdCard(profile.user?.idCard) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">紧急联系人</span>
              <span class="info-value">{{ profile.user?.emergencyContact || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">紧急电话</span>
              <span class="info-value">{{ profile.user?.emergencyPhone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDate(profile.user?.createTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">账号状态</span>
              <span class="info-value">
                <el-tag :type="profile.user?.status === 1 ? 'success' : 'danger'" size="small" effect="light">
                  {{ profile.user?.status === 1 ? '正常' : '已禁用' }}
                </el-tag>
              </span>
            </div>
          </div>
        </div>

        <!-- 住房信息 -->
        <div class="info-card">
          <h3 class="card-title">住房信息 <el-tag v-if="profile.houses?.length" size="small" type="primary" effect="plain" round>{{ profile.houses.length }}套</el-tag></h3>
          <div class="houses-list" v-if="profile.houses?.length">
            <div class="house-row" v-for="(h, idx) in profile.houses" :key="idx">
              <div class="house-index">{{ idx + 1 }}</div>
              <div class="house-details">
                <div class="house-main">{{ h.buildingName || '—' }} · {{ h.unitName }} · {{ h.roomNo }}</div>
                <div class="house-sub">
                  <span v-if="h.area">面积 {{ Number(h.area).toFixed(1) }}㎡</span>
                </div>
              </div>
            </div>
          </div>
          <div class="empty-hint" v-else>
            <el-icon :size="20"><Warning /></el-icon>
            <span>暂未绑定住房，请联系物业管理员</span>
          </div>
        </div>
      </div>

      <!-- 右侧：统计与操作 -->
      <div class="profile-side">
        <!-- 我的统计 -->
        <div class="stats-card">
          <h3 class="card-title">我的数据</h3>
          <div class="stats-list">
            <div class="stat-row" @click="$router.push('/owner/bills')">
              <div class="stat-icon-wrap" style="background: #fee2e2; color: #ef4444;">
                <el-icon :size="20"><Money /></el-icon>
              </div>
              <div class="stat-detail">
                <span class="stat-num">{{ profile.pendingBills ?? '-' }}</span>
                <span class="stat-desc">待缴账单</span>
              </div>
              <el-icon class="stat-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="stat-row" @click="$router.push('/owner/repairs')">
              <div class="stat-icon-wrap" style="background: #dbeafe; color: #3b82f6;">
                <el-icon :size="20"><Tools /></el-icon>
              </div>
              <div class="stat-detail">
                <span class="stat-num">{{ profile.activeRepairs ?? '-' }}</span>
                <span class="stat-desc">进行中报修</span>
              </div>
              <el-icon class="stat-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="stat-row" @click="$router.push('/owner/parking')">
              <div class="stat-icon-wrap" style="background: #ede9fe; color: #7c3aed;">
                <el-icon :size="20"><Van /></el-icon>
              </div>
              <div class="stat-detail">
                <span class="stat-num">{{ profile.parkingCount ?? '-' }}</span>
                <span class="stat-desc">已购车位</span>
              </div>
              <el-icon class="stat-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <!-- 安全设置 -->
        <div class="info-card">
          <h3 class="card-title">安全设置</h3>
          <div class="action-row" @click="pwdDialogVisible = true">
            <div class="action-left">
              <el-icon :size="18"><Lock /></el-icon>
              <span>修改密码</span>
            </div>
            <el-icon class="stat-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="520px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="真实姓名">
              <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="editForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="电子邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="editForm.gender">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="editForm.birthday" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="身份证号">
          <el-input v-model="editForm.idCard" placeholder="18位身份证号码" maxlength="18" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="editForm.emergencyContact" placeholder="紧急联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急电话">
              <el-input v-model="editForm.emergencyPhone" placeholder="紧急联系人电话" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px" destroy-on-close>
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="不少于6位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword" :loading="pwdLoading">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const profile = ref({})
const avatarInput = ref(null)
const avatarUploading = ref(false)

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', OWNER: '业主', STAFF: '工作人员' }
  return map[profile.value.user?.role] || profile.value.user?.role || '-'
})

const hasHousing = computed(() =>
  profile.value.houses?.length > 0
)

const avatarFullUrl = computed(() => {
  const url = profile.value.user?.avatarUrl
  if (!url) return ''
  // If already absolute URL, return as-is
  if (url.startsWith('http')) return url
  // Prepend backend base URL for relative paths
  return url
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await request.get('/users/owner/profile')
    profile.value = res.data || {}
  } catch (e) {
    ElMessage.error('加载个人信息失败')
  } finally {
    loading.value = false
  }
}

// ========== 编辑资料 ==========
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({ realName: '', phone: '', email: '', gender: '', birthday: '', idCard: '', emergencyContact: '', emergencyPhone: '' })

const maskIdCard = (id) => {
  if (!id) return '未设置'
  if (id.length <= 6) return id
  return id.slice(0, 3) + '***********' + id.slice(-4)
}

const openEditDialog = () => {
  const u = profile.value.user || {}
  editForm.realName = u.realName || ''
  editForm.phone = u.phone || ''
  editForm.email = u.email || ''
  editForm.gender = u.gender || ''
  editForm.birthday = u.birthday || ''
  editForm.idCard = u.idCard || ''
  editForm.emergencyContact = u.emergencyContact || ''
  editForm.emergencyPhone = u.emergencyPhone || ''
  editDialogVisible.value = true
}

const submitEdit = async () => {
  editLoading.value = true
  try {
    await request.put('/users/owner/info', {
      realName: editForm.realName,
      phone: editForm.phone,
      email: editForm.email,
      gender: editForm.gender,
      birthday: editForm.birthday || null,
      idCard: editForm.idCard,
      emergencyContact: editForm.emergencyContact,
      emergencyPhone: editForm.emergencyPhone
    })
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    fetchProfile()
  } catch (e) {
    ElMessage.error('修改失败')
  } finally {
    editLoading.value = false
  }
}

// ========== 修改密码 ==========
const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

const submitPassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) return ElMessage.warning('请填写完整')
  if (pwdForm.newPassword.length < 6) return ElMessage.warning('新密码不少于6位')
  pwdLoading.value = true
  try {
    const res = await request.put('/users/owner/password', pwdForm)
    if (res.code !== 200) return ElMessage.error(res.msg || '修改失败')
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    sessionStorage.clear()
    window.location.href = '/login'
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

// ========== 头像上传 ==========
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const onAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像文件不能超过2MB')
    return
  }
  avatarUploading.value = true
  try {
    // 1. 上传文件
    const formData = new FormData()
    formData.append('file', file)
    const uploadRes = await request.post('/upload/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const avatarUrl = uploadRes.data?.url
    if (!avatarUrl) throw new Error('上传失败')
    // 2. 更新用户头像URL
    await request.put('/users/owner/info', { avatarUrl })
    ElMessage.success('头像更新成功')
    fetchProfile()
  } catch (err) {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
    // 清空 input 以便再次选择同一文件
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

onMounted(fetchProfile)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

$owner: #0d9488;
$owner-light: #14b8a6;
$owner-50: #f0fdfa;

.owner-profile {
  max-width: 960px;
  margin: 0 auto;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 20px;
  align-items: start;
}

/* ========== 头像区 ========== */
.avatar-section {
  background: white;
  border-radius: 16px;
  padding: 28px 24px;
  border: 1px solid $border-color;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;

  .avatar-ring {
    flex-shrink: 0;
    position: relative;
    cursor: pointer;

    .profile-avatar {
      background: linear-gradient(135deg, $owner, $owner-light);
      color: white;
    }

    .avatar-overlay {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      background: rgba(0, 0, 0, 0.4);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.2s;
    }

    &:hover .avatar-overlay {
      opacity: 1;
    }
  }

  .avatar-info {
    flex: 1;

    .profile-name {
      font-size: 22px;
      font-weight: 700;
      color: $text-primary;
      margin: 0 0 8px;
    }

    .profile-meta {
      display: flex;
      align-items: center;
      gap: 10px;

      .meta-text {
        font-size: 13px;
        color: $text-secondary;
        font-family: $font-mono;
      }
    }
  }

  .edit-btn {
    flex-shrink: 0;
  }
}

/* ========== 信息卡片 ========== */
.info-card {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid $border-color;
  margin-bottom: 20px;

  .card-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-color-light;
  }

  .info-grid {
    display: grid;
    gap: 14px;
  }

  .info-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .info-label {
      font-size: 13px;
      color: $text-secondary;
      width: 72px;
      flex-shrink: 0;
    }

    .info-value {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }
  }

  .empty-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: $text-tertiary;
    padding: 8px 0;
  }

  .houses-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .house-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    background: $owner-50;
    border-radius: 10px;
    border: 1px solid lighten($owner, 40%);
  }

  .house-index {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: $owner;
    color: white;
    font-size: 12px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .house-details {
    flex: 1;
    min-width: 0;
  }

  .house-main {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  .house-sub {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 2px;
  }

  .action-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    cursor: pointer;
    border-radius: 8px;
    transition: background 0.15s;

    &:hover {
      background: $gray-50;
      margin: 0 -8px;
      padding: 12px 8px;
    }

    .action-left {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }
  }
}

/* ========== 统计卡片 ========== */
.stats-card {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid $border-color;
  margin-bottom: 20px;

  .card-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-color-light;
  }

  .stats-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .stat-row {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 8px;
    border-radius: 10px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover {
      background: $gray-50;
    }

    .stat-icon-wrap {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-detail {
      flex: 1;
      display: flex;
      flex-direction: column;

      .stat-num {
        font-size: 20px;
        font-weight: 700;
        color: $text-primary;
        line-height: 1.2;
      }

      .stat-desc {
        font-size: 12px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }

    .stat-arrow {
      color: $text-tertiary;
      font-size: 14px;
    }
  }
}

.stat-arrow {
  color: $text-tertiary;
  font-size: 14px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
