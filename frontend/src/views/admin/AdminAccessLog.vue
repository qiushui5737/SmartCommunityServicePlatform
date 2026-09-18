<template>
  <div class="admin-access-log">
    <!-- Stats Cards -->
    <div class="stat-row">
      <div class="stat-box s-total">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-lbl">总记录数</div>
      </div>
      <div class="stat-box s-today">
        <div class="stat-num">{{ stats.today }}</div>
        <div class="stat-lbl">今日通行</div>
      </div>
      <div class="stat-box s-denied">
        <div class="stat-num">{{ stats.denied }}</div>
        <div class="stat-lbl">拒绝通行</div>
      </div>
      <div class="stat-box s-locations">
        <div class="stat-num">{{ stats.locationCount }}</div>
        <div class="stat-lbl">监控点位</div>
      </div>
    </div>

    <!-- Main Content -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon"><el-icon :size="20"><Monitor /></el-icon></div>
            <div>
              <div class="header-title">门禁通行记录</div>
              <div class="header-desc">追踪社区人员进出动态，监控异常通行事件</div>
            </div>
          </div>
        </div>
      </template>

      <!-- Filters -->
      <div class="filter-row">
        <el-input v-model="filters.userName" placeholder="搜索姓名" clearable style="width:140px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
        <el-input v-model="filters.cardNo" placeholder="搜索卡号" clearable style="width:170px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.direction" placeholder="方向" clearable style="width:100px" @change="loadData">
          <el-option label="进入" value="IN" /><el-option label="离开" value="OUT" />
        </el-select>
        <el-select v-model="filters.accessStatus" placeholder="状态" clearable style="width:100px" @change="loadData">
          <el-option label="放行" value="SUCCESS" /><el-option label="拒绝" value="DENIED" />
        </el-select>
        <el-select v-model="filters.buildingId" placeholder="楼栋" clearable style="width:100px" @change="loadData">
          <el-option v-for="i in 6" :key="i" :label="i + '栋'" :value="i" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <!-- Table -->
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="accessTime" label="时间" width="170" />
        <el-table-column prop="userName" label="人员" width="120" />
        <el-table-column prop="cardNo" label="卡号" width="155">
          <template #default="{ row }"><span class="mono">{{ row.cardNo }}</span></template>
        </el-table-column>
        <el-table-column label="方向" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'info'" size="small" effect="plain">
              <el-icon :size="12"><component :is="row.direction === 'IN' ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
              {{ row.direction === 'IN' ? '进入' : '离开' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gateLocation" label="通行位置" width="130" />
        <el-table-column label="楼栋" width="80" align="center">
          <template #default="{ row }">{{ row.buildingId ? row.buildingId + '栋' : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.accessStatus === 'SUCCESS' ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.accessStatus === 'SUCCESS' ? '放行' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="拒绝原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.denyReason" class="deny-text">{{ row.denyReason }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="page.total > page.size">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadData" layout="total, prev, pager, next" />
      </div>
    </el-card>

    <!-- Location Stats -->
    <el-card shadow="never" class="stats-card" v-if="locationStats.length">
      <template #header><div class="sub-header">各点位通行统计</div></template>
      <div class="location-grid">
        <div v-for="loc in locationStats" :key="loc.location" class="loc-item">
          <div class="loc-name">{{ loc.location }}</div>
          <div class="loc-bar">
            <div class="bar-fill" :style="{ width: locBarWidth(loc.total) + '%' }"></div>
          </div>
          <div class="loc-counts">
            <span class="total">{{ loc.total }}次</span>
            <span v-if="loc.denied > 0" class="denied">{{ loc.denied }}次拒绝</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { Monitor, User, Search, DArrowRight, DArrowLeft } from '@element-plus/icons-vue'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 15, total: 0 })
const filters = reactive({ cardNo: '', userName: '', direction: '', accessStatus: '', buildingId: '' })
const stats = reactive({ total: 0, today: 0, denied: 0, locationCount: 0 })
const locationStats = ref([])

const maxLocTotal = computed(() => locationStats.value.length ? Math.max(...locationStats.value.map(l => l.total)) : 1)
const locBarWidth = (total) => Math.round((total / maxLocTotal.value) * 100)

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: page.current, size: page.size }
    if (filters.cardNo) params.cardNo = filters.cardNo
    if (filters.userName) params.userName = filters.userName
    if (filters.direction) params.direction = filters.direction
    if (filters.accessStatus) params.accessStatus = filters.accessStatus
    if (filters.buildingId) params.buildingId = filters.buildingId
    const res = await request.get('/access-log/page', { params })
    list.value = res.data.records || []
    page.total = res.data.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const loadStats = async () => {
  try {
    const res = await request.get('/access-log/stats')
    const d = res.data
    stats.total = d.total || 0
    stats.today = d.today || 0
    stats.denied = d.denied || 0
    locationStats.value = d.locationStats || []
    stats.locationCount = locationStats.value.length
  } catch (e) {}
}

onMounted(() => { loadData(); loadStats() })
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.admin-access-log {
  .stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px;
    .stat-box { padding: 18px; border-radius: 12px; text-align: center; background: $gray-50; border: 1px solid $border-color-light;
      .stat-num { font-size: 28px; font-weight: 700; letter-spacing: -0.02em; }
      .stat-lbl { font-size: 12px; color: $text-secondary; margin-top: 4px; }
      &.s-total .stat-num { color: $text-primary; }
      &.s-today .stat-num { color: #10b981; }
      &.s-denied .stat-num { color: #ef4444; }
      &.s-locations .stat-num { color: #6366f1; }
    }
  }

  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    .header-left { display: flex; align-items: center; gap: 14px;
      .header-icon { width: 48px; height: 48px; background: linear-gradient(135deg, #6366f1, #4f46e5); border-radius: 14px; display: flex; align-items: center; justify-content: center; color: white; box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25); }
      .header-title { font-size: 18px; font-weight: 700; color: $text-primary; }
      .header-desc { font-size: 13px; color: $text-secondary; margin-top: 2px; }
    }
  }

  .filter-row { display: flex; gap: 8px; margin-bottom: 16px; align-items: center; }
  .mono { font-family: $font-mono; font-size: 13px; letter-spacing: 0.03em; }
  .deny-text { color: #ef4444; font-size: 13px; }
  .text-muted { color: $text-tertiary; }
  .pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

  .stats-card { margin-top: 18px;
    .sub-header { font-size: 16px; font-weight: 600; color: $text-primary; }
  }

  .location-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;
    .loc-item { display: flex; align-items: center; gap: 12px; padding: 10px 14px; background: $gray-50; border-radius: 10px; border: 1px solid $border-color-light;
      .loc-name { font-size: 13px; font-weight: 600; color: $text-primary; min-width: 90px; white-space: nowrap; }
      .loc-bar { flex: 1; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden;
        .bar-fill { height: 100%; background: linear-gradient(90deg, #6366f1, #818cf8); border-radius: 4px; transition: width 0.5s ease; }
      }
      .loc-counts { min-width: 80px; text-align: right;
        .total { font-size: 13px; font-weight: 600; color: $text-primary; }
        .denied { font-size: 11px; color: #ef4444; margin-left: 6px; }
      }
    }
  }
}
</style>
