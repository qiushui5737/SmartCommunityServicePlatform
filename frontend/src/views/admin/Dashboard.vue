<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="(item, index) in statsList" :key="item.label">
        <div class="stat-card" :class="`stat-card-${index}`">
          <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ item.value.toLocaleString() }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
          <div class="stat-trend" v-if="item.trend">
            <el-icon><ArrowUp /></el-icon>
            <span>{{ item.trend }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <div class="chart-title">
                <el-icon><Histogram /></el-icon>
                <span>缴费状态统计</span>
              </div>
            </div>
          </template>
          <div ref="feeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <div class="chart-title">
                <el-icon><PieChart /></el-icon>
                <span>房屋入住统计</span>
              </div>
            </div>
          </template>
          <div ref="houseChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="20" class="quick-actions-row">
      <el-col :span="24">
        <el-card shadow="never" class="quick-actions">
          <template #header>
            <div class="chart-header">
              <div class="chart-title">
                <el-icon><Document /></el-icon>
                <span>快捷操作</span>
              </div>
            </div>
          </template>
          <div class="actions-grid">
            <div
              v-for="action in quickActions"
              :key="action.name"
              class="action-item"
              @click="$router.push(action.path)"
            >
              <div class="action-icon" :style="{ background: action.bg, color: action.color }">
                <el-icon :size="24"><component :is="action.icon" /></el-icon>
              </div>
              <div class="action-name">{{ action.name }}</div>
              <div class="action-desc">{{ action.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  UserFilled, OfficeBuilding, Money, Van,
  Histogram, PieChart,
  ArrowUp, User, Bell
} from '@element-plus/icons-vue'

const statsList = ref([
  { label: '用户总数', value: 1280, icon: 'UserFilled', bg: '#dbeafe', color: '#2563eb', trend: '12%' },
  { label: '待办报修', value: 15, icon: 'Bell', bg: '#fef3c7', color: '#d97706', trend: '5%' },
  { label: '房屋总数', value: 856, icon: 'OfficeBuilding', bg: '#d1fae5', color: '#059669', trend: '8%' },
  { label: '空闲车位', value: 120, icon: 'Van', bg: '#ede9fe', color: '#7c3aed', trend: '3%' }
])

const quickActions = ref([
  { name: '新增用户', desc: '添加新业主或员工', icon: 'User', bg: '#dbeafe', color: '#2563eb', path: '/admin/users' },
  { name: '费用管理', desc: '查看收费项目', icon: 'Money', bg: '#d1fae5', color: '#059669', path: '/admin/fee' },
  { name: '楼栋管理', desc: '管理楼栋房屋', icon: 'OfficeBuilding', bg: '#fef3c7', color: '#d97706', path: '/admin/building-house' },
  { name: '发布公告', desc: '发布社区通知', icon: 'Bell', bg: '#fce7f3', color: '#db2777', path: '/admin/announcements' }
])

let feeChart, houseChart
const feeChartRef = ref(null)
const houseChartRef = ref(null)

onMounted(() => {
  // 缴费状态图表
  feeChart = echarts.init(feeChartRef.value)
  feeChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' }
    },
    grid: { left: '8%', right: '5%', top: '10%', bottom: '15%' },
    xAxis: {
      type: 'category',
      data: ['已缴', '待缴', '逾期'],
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 13 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#94a3b8' }
    },
    series: [{
      type: 'bar',
      data: [
        { value: 320, itemStyle: { color: '#10b981', borderRadius: [8, 8, 0, 0] } },
        { value: 150, itemStyle: { color: '#f59e0b', borderRadius: [8, 8, 0, 0] } },
        { value: 45, itemStyle: { color: '#ef4444', borderRadius: [8, 8, 0, 0] } }
      ],
      barWidth: '50%',
      label: { show: true, position: 'top', color: '#64748b', fontSize: 12 }
    }]
  })

  // 房屋入住图表
  houseChart = echarts.init(houseChartRef.value)
  houseChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' }
    },
    legend: { bottom: '5%', textStyle: { color: '#64748b' } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', color: '#64748b' },
      data: [
        { value: 780, name: '已入住', itemStyle: { color: '#3b82f6' } },
        { value: 76, name: '空置', itemStyle: { color: '#94a3b8' } }
      ]
    }]
  })

  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  feeChart?.resize()
  houseChart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  feeChart?.dispose()
  houseChart?.dispose()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.dashboard { padding: 0; }

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border: 1px solid $border-color;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-2px);
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 80px;
    height: 80px;
    border-radius: 0 0 0 80px;
    opacity: 0.05;
    background: currentColor;
  }

  .stat-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-content {
    flex: 1;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: $text-primary;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 4px;
    }
  }

  .stat-trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: $success;
    font-weight: 600;
    background: $success-light;
    padding: 4px 8px;
    border-radius: 6px;
  }
}

.charts-row { margin-top: 20px; }

.chart-card {
  :deep(.el-card__header) {
    padding: 16px 20px;
  }

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .chart-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      font-size: 15px;
      color: $text-primary;

      .el-icon { color: $primary; }
    }
  }

  .chart-container {
    height: 320px;
  }
}

.quick-actions-row { margin-top: 20px; }

.quick-actions {
  :deep(.el-card__header) {
    padding: 16px 20px;
  }

  .actions-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;

    .action-item {
      text-align: center;
      padding: 24px;
      border-radius: 16px;
      border: 1px solid $border-color;
      cursor: pointer;
      transition: all 0.3s ease;
      background: white;

      &:hover {
        box-shadow: $shadow-md;
        transform: translateY(-4px);
        border-color: $primary-200;
      }

      .action-icon {
        width: 56px;
        height: 56px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 12px;
      }

      .action-name {
        font-weight: 600;
        font-size: 15px;
        color: $text-primary;
        margin-bottom: 4px;
      }

      .action-desc {
        font-size: 12px;
        color: $text-secondary;
      }
    }
  }
}

@media (max-width: 1024px) {
  .quick-actions .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
