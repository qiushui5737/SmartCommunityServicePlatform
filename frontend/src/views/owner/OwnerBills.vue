<template>
  <div class="owner-bills">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><Money /></el-icon>
            </div>
            <div>
              <div class="header-title">在线缴费</div>
              <div class="header-desc">查看并缴纳您的各项费用</div>
            </div>
          </div>
          <div class="header-right">
            <el-tag type="info" effect="light" class="user-tag">
              <el-icon><User /></el-icon>
              <span>当前业主: {{ userInfo?.realName || '未知' }}</span>
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 账单统计 -->
      <el-row :gutter="20" class="bill-stats">
        <el-col :span="6">
          <div class="bill-stat-card pending">
            <div class="stat-icon-bg"><el-icon><Warning /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">待缴费</div>
              <div class="stat-value">{{ billStats.pending }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="bill-stat-card paid">
            <div class="stat-icon-bg"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">已缴费</div>
              <div class="stat-value">{{ billStats.paid }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="bill-stat-card overdue">
            <div class="stat-icon-bg"><el-icon><Clock /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">已逾期</div>
              <div class="stat-value">{{ billStats.overdue }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="bill-stat-card total">
            <div class="stat-icon-bg"><el-icon><Wallet /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">总计金额</div>
              <div class="stat-value">¥{{ billStats.totalAmount.toFixed(2) }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 状态筛选 -->
      <el-tabs v-model="activeStatus" @tab-click="handleTabChange" class="status-tabs">
        <el-tab-pane label="全部账单" name="ALL"></el-tab-pane>
        <el-tab-pane label="待缴费" name="PENDING"></el-tab-pane>
        <el-tab-pane label="已缴费" name="PAID"></el-tab-pane>
        <el-tab-pane label="已逾期" name="OVERDUE"></el-tab-pane>
      </el-tabs>

      <!-- 账单表格 -->
      <el-table :data="bills" v-loading="loading" stripe class="bills-table">
        <el-table-column prop="billNo" label="账单编号" width="170" align="center" />
        <el-table-column label="关联对象" width="160" align="center">
          <template #default="{row}">{{ row.parkingLabel || row.houseLabel || '—' }}</template>
        </el-table-column>
        <el-table-column label="费用类型" width="150">
          <template #default="{row}">
            <el-tag effect="light" size="small">{{ getFeeTypeName(row.feeItemId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额(元)" width="120" align="center">
          <template #default="{row}">
            <span class="bill-amount">¥{{ row.amount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日期" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="statusMap[row.status]?.type" effect="light">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" align="center">
          <template #default="{row}">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              size="small"
              @click="handlePay(row)"
              class="pay-btn"
            >
              <el-icon><Wallet /></el-icon>立即缴费
            </el-button>
            <span v-else class="text-gray">--</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && bills.length === 0" description="暂无账单记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Money, User, Warning, CircleCheck, Clock, Wallet } from '@element-plus/icons-vue'

const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || '{}'))
const activeStatus = ref('ALL')
const bills = ref([])
const loading = ref(false)
const billStats = ref({ pending: 0, paid: 0, overdue: 0, totalAmount: 0 })

const statusMap = {
  PENDING: { text: '待缴费', type: 'warning' },
  PAID:    { text: '已缴费', type: 'success' },
  OVERDUE: { text: '已逾期', type: 'danger' }
}

const getFeeTypeName = (id) => {
  const map = { 1: '物业管理费', 2: '垃圾清运费', 3: '车位管理费', 4: '公摊水电费' }
  return map[id] || `费用项目-${id}`
}

const handleTabChange = async (pane) => {
  const status = pane.props.name
  activeStatus.value = status
  loading.value = true
  try {
    const params = status === 'ALL' ? {} : { status: status }
    const res = await request.get('/owner/bills', { params })
    bills.value = res.data || []
    updateStats()
  } finally {
    loading.value = false
  }
}

const updateStats = () => {
  const pending = bills.value.filter(b => b.status === 'PENDING').length
  const paid = bills.value.filter(b => b.status === 'PAID').length
  const overdue = bills.value.filter(b => b.status === 'OVERDUE').length
  const totalAmount = bills.value.filter(b => b.status === 'PENDING').reduce((sum, b) => sum + b.amount, 0)
  billStats.value = { pending, paid, overdue, totalAmount }
}

const handlePay = async (bill) => {
  ElMessageBox.confirm(`确认支付 ¥${bill.amount.toFixed(2)} 吗？`, '缴费确认', {
    confirmButtonText: '确认支付',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/owner/bills/${bill.id}/pay`)
      ElMessage.success('支付成功！')
      handleTabChange({ props: { name: activeStatus.value } })
    } catch (e) {}
  }).catch(() => {})
}

onMounted(() => {
  handleTabChange({ props: { name: 'ALL' } })
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-bills {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 14px;

      .header-icon {
        width: 48px;
        height: 48px;
        background: linear-gradient(135deg, #f59e0b, #d97706);
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
      }

      .header-title {
        font-size: 18px;
        font-weight: 700;
        color: $text-primary;
        letter-spacing: -0.01em;
      }

      .header-desc {
        font-size: 13px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }

    .user-tag {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      font-weight: 500;
    }
  }

  .bill-stats {
    margin-bottom: 24px;

    .bill-stat-card {
      border-radius: 16px;
      padding: 22px;
      display: flex;
      align-items: center;
      gap: 14px;
      border: 1px solid $border-color;
      background: white;
      transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
      overflow: hidden;

      &:hover {
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
        transform: translateY(-4px);
      }

      .stat-icon-bg {
        width: 48px;
        height: 48px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      &.pending .stat-icon-bg { background: #fef3c7; color: #d97706; }
      &.paid .stat-icon-bg { background: #d1fae5; color: #059669; }
      &.overdue .stat-icon-bg { background: #fee2e2; color: #ef4444; }
      &.total .stat-icon-bg { background: #dbeafe; color: #2563eb; }

      .stat-info {
        .stat-label {
          font-size: 12px;
          color: $text-secondary;
          font-weight: 500;
        }

        .stat-value {
          font-size: 26px;
          font-weight: 700;
          color: $text-primary;
          margin-top: 2px;
          letter-spacing: -0.02em;
          line-height: 1.2;
        }
      }
    }
  }

  .status-tabs {
    margin-bottom: 16px;
  }

  .bills-table {
    .bill-amount {
      font-weight: 700;
      font-size: 15px;
      color: $text-primary;
    }
  }

  .pay-btn {
    font-weight: 600;
    padding: 8px 20px !important;
    height: 34px;

    .el-icon { margin-right: 4px; }
  }

  .text-gray { color: $text-tertiary; font-size: 12px; }
}
</style>
