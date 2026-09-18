<template>
  <div class="owner-facility">
    <!-- 顶部搜索 + 分类筛选 -->
    <div class="facility-toolbar">
      <el-input v-model="keyword" placeholder="搜索设施名称..." clearable style="width: 260px" @clear="loadFacilities" @keyup.enter="loadFacilities">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="category-tabs">
        <span class="cat-tab" :class="{ active: !selectedCategory }" @click="selectedCategory = ''; loadFacilities()">全部</span>
        <span v-for="c in categories" :key="c" class="cat-tab" :class="{ active: selectedCategory === c }" @click="selectedCategory = c; loadFacilities()">{{ c }}</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <!-- ====== Tab 1：浏览设施 ====== -->
      <el-tab-pane label="设施浏览" name="browse">
        <div v-loading="loading" class="facility-grid">
          <div v-for="item in facilities" :key="item.id" class="facility-card" @click="openDetail(item)">
            <div class="facility-img">
              <img :src="item.imageUrl" :alt="item.name" @error="handleImgError($event, item.name, item.category)" />
              <el-tag class="status-badge" :type="statusMap[item.status]?.type" effect="dark" size="small">
                {{ statusMap[item.status]?.text }}
              </el-tag>
            </div>
            <div class="facility-info">
              <h3 class="facility-name">{{ item.name }}</h3>
              <p class="facility-desc">{{ item.description }}</p>
              <div class="facility-meta">
                <span class="meta-location">📍 {{ item.location }}</span>
                <span v-if="item.deposit > 0" class="meta-deposit">💰 押金 ¥{{ item.deposit }}</span>
              </div>
              <el-button v-if="item.status === 'AVAILABLE'" type="primary" size="small" @click.stop="openBooking(item)" class="book-btn">
                申请借用
              </el-button>
              <el-button v-else disabled size="small" class="book-btn">{{ statusMap[item.status]?.text }}</el-button>
            </div>
          </div>
        </div>
        <div v-if="!loading && facilities.length === 0" class="empty-tip">
          <el-empty description="暂无可借用的设施" />
        </div>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadFacilities" layout="total, prev, pager, next" />
        </div>
      </el-tab-pane>

      <!-- ====== Tab 2：我的借用记录 ====== -->
      <el-tab-pane label="我的借用记录" name="records">
        <el-table :data="bookings" v-loading="bookingLoading" stripe border>
          <el-table-column prop="id" label="单号" width="70" align="center" />
          <el-table-column label="设施" min-width="140">
            <template #default="{row}">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image :src="row.facilityImage" :preview-src-list="[row.facilityImage]" fit="cover" style="width:40px;height:40px;border-radius:6px;flex-shrink:0" @error="handleImgError($event, row.facilityName, row.facilityCategory)" />
                <span>{{ row.facilityName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="purpose" label="借用事由" min-width="160" show-overflow-tooltip />
          <el-table-column prop="durationHours" label="借用时长" width="90" align="center">
            <template #default="{row}">{{ row.durationHours }}h</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{row}"><el-tag :type="bookingStatusMap[row.status]?.type">{{ bookingStatusMap[row.status]?.text }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="replyContent" label="物业回复" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="160" />
          <el-table-column label="操作" width="90" align="center">
            <template #default="{row}">
              <el-button v-if="row.status === 'APPROVED'" type="success" size="small" link @click="handleReturn(row)">归还</el-button>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="bookingPage.current" v-model:page-size="bookingPage.size" :total="bookingPage.total" @current-change="loadBookings" layout="total, prev, pager, next" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ====== 借用申请弹窗 ====== -->
    <el-dialog v-model="bookingDialogVisible" title="申请借用" width="480px" destroy-on-close>
      <div class="booking-facility-info">
        <el-image :src="currentFacility?.imageUrl" fit="cover" style="width:80px;height:80px;border-radius:12px" @error="handleImgError($event, currentFacility?.name, currentFacility?.category)" />
        <div>
          <h4>{{ currentFacility?.name }}</h4>
          <p class="text-muted">📍 {{ currentFacility?.location }}</p>
          <p v-if="currentFacility?.deposit > 0" class="text-muted">💰 借用押金 ¥{{ currentFacility?.deposit }}</p>
        </div>
      </div>
      <el-form :model="bookingForm" label-width="90px" style="margin-top:16px">
        <el-form-item label="借用事由" required>
          <el-input v-model="bookingForm.purpose" type="textarea" :rows="3" placeholder="请描述借用原因..." />
        </el-form-item>
        <el-form-item label="借用时长" required>
          <el-select v-model="bookingForm.durationHours" style="width:100%">
            <el-option :value="2" label="2 小时" />
            <el-option :value="4" label="4 小时" />
            <el-option :value="8" label="8 小时（半天）" />
            <el-option :value="24" label="24 小时（1天）" />
            <el-option :value="72" label="3 天" />
            <el-option :value="168" label="7 天（1周）" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const catColors = { '运动器材': '#3b82f6', '工具设备': '#f97316', '文娱用品': '#8b5cf6', '清洁工具': '#10b981', '其他': '#64748b' }
const handleImgError = (e, name, category) => {
  const bg = catColors[category] || '#64748b'
  const ch = (name || '?')[0]
  e.target.src = `data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect fill="${bg}" width="400" height="300"/><text x="200" y="175" text-anchor="middle" fill="white" font-size="72" font-weight="bold" font-family="system-ui,sans-serif">${ch}</text></svg>`)}`
}

// ========== 设施列表 ==========
const categories = ['运动器材', '工具设备', '文娱用品', '清洁工具', '其他']
const selectedCategory = ref('')
const keyword = ref('')
const activeTab = ref('browse')
const loading = ref(false)
const facilities = ref([])
const page = reactive({ current: 1, size: 12, total: 0 })

const statusMap = {
  AVAILABLE:   { text: '可借用', type: 'success' },
  BOOKED:      { text: '已借出', type: 'warning' },
  MAINTENANCE: { text: '维护中', type: 'info' },
  RETIRED:     { text: '已报废', type: 'danger' }
}

const bookingStatusMap = {
  PENDING:  { text: '待审批', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  RETURNED: { text: '已归还', type: 'info' }
}

const loadFacilities = async () => {
  loading.value = true
  try {
    const res = await request.get('/facility/page', {
      params: { current: page.current, size: page.size, category: selectedCategory.value || undefined, keyword: keyword.value || undefined }
    })
    facilities.value = res.data?.records || []
    page.total = res.data?.total || 0
  } catch (e) {
    console.error('加载设施列表失败', e)
  } finally {
    loading.value = false
  }
}

// ========== 借用记录 ==========
const bookings = ref([])
const bookingLoading = ref(false)
const bookingPage = reactive({ current: 1, size: 10, total: 0 })

const loadBookings = async () => {
  bookingLoading.value = true
  try {
    const res = await request.get('/facility-booking/mine', {
      params: { current: bookingPage.current, size: bookingPage.size }
    })
    bookings.value = res.data?.records || []
    bookingPage.total = res.data?.total || 0
  } catch (e) {
    console.error('加载借用记录失败', e)
  } finally {
    bookingLoading.value = false
  }
}

const onTabChange = () => {
  if (activeTab.value === 'records') loadBookings()
  else loadFacilities()
}

// ========== 借用申请 ==========
const bookingDialogVisible = ref(false)
const currentFacility = ref(null)
const submitting = ref(false)
const bookingForm = ref({ purpose: '', durationHours: 24 })

const openBooking = (item) => {
  currentFacility.value = item
  bookingForm.value = { purpose: '', durationHours: 24 }
  bookingDialogVisible.value = true
}

const openDetail = (item) => {
  // 点击卡片：可借用则弹出申请，否则仅查看
  if (item.status === 'AVAILABLE') openBooking(item)
}

const submitBooking = async () => {
  if (!bookingForm.value.purpose.trim()) return ElMessage.warning('请填写借用事由')
  submitting.value = true
  try {
    await request.post('/facility-booking', {
      facilityId: currentFacility.value.id,
      purpose: bookingForm.value.purpose,
      durationHours: bookingForm.value.durationHours
    })
    ElMessage.success('申请已提交，请等待物业审批')
    bookingDialogVisible.value = false
    loadFacilities()
  } catch (e) {
    console.error('提交借用申请失败', e)
  } finally {
    submitting.value = false
  }
}

// ========== 归还 ==========
const handleReturn = async (row) => {
  try {
    await ElMessageBox.confirm('确认已归还该设施？', '归还确认', { type: 'info' })
    await request.put(`/facility-booking/${row.id}/return`)
    ElMessage.success('归还成功')
    loadBookings()
  } catch (e) {
    if (e !== 'cancel') console.error('归还失败', e)
  }
}

onMounted(() => {
  loadFacilities()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-facility {
  .facility-toolbar {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 20px;
    flex-wrap: wrap;

    .category-tabs {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      .cat-tab {
        padding: 7px 18px;
        border-radius: 20px;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        color: $text-secondary;
        background: $bg-tertiary;
        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
        border: 1px solid transparent;

        &:hover {
          color: $primary;
          background: $primary-50;
          border-color: $primary-200;
        }

        &.active {
          color: white;
          background: $primary;
          box-shadow: 0 2px 8px rgba($primary, 0.3);
        }
      }
    }
  }

  .facility-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    min-height: 200px;
  }

  .facility-card {
    background: white;
    border-radius: 16px;
    border: 1px solid $border-color;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
      transform: translateY(-6px);
      border-color: $primary-200;
    }

    .facility-img {
      position: relative;
      height: 200px;
      background: $bg-tertiary;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
      }

      &:hover img {
        transform: scale(1.05);
      }

      .status-badge {
        position: absolute;
        top: 12px;
        right: 12px;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      }
    }

    .facility-info {
      padding: 18px;

      .facility-name {
        font-size: 17px;
        font-weight: 700;
        color: $text-primary;
        margin: 0 0 6px;
        letter-spacing: -0.01em;
      }

      .facility-desc {
        font-size: 13px;
        color: $text-secondary;
        margin: 0 0 12px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        line-height: 1.6;
      }

      .facility-meta {
        display: flex;
        gap: 12px;
        margin-bottom: 14px;
        font-size: 12px;
        color: $text-tertiary;
        font-weight: 500;
      }

      .book-btn {
        width: 100%;
        font-weight: 600;
        height: 36px;
      }
    }
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 24px;
  }

  .text-muted {
    color: $text-tertiary;
    font-size: 12px;
  }

  .booking-facility-info {
    display: flex;
    gap: 16px;
    align-items: center;
    padding: 16px;
    background: $bg-tertiary;
    border-radius: 12px;

    h4 {
      margin: 0 0 4px;
      color: $text-primary;
      font-weight: 700;
    }
  }

  .empty-tip {
    padding: 40px 0;
  }
}
</style>
