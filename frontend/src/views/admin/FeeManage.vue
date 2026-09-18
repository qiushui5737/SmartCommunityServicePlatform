<template>
  <div class="fee-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><Money /></el-icon>
            </div>
            <div>
              <div class="header-title">费用管理</div>
              <div class="header-desc">管理收费项目及业主账单</div>
            </div>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="manage-tabs">
        <el-tab-pane label="收费项目配置" name="items">
          <div class="toolbar">
            <el-input v-model="itemKeyword" placeholder="搜索项目名称" clearable @clear="loadItems" @keyup.enter="loadItems" style="width: 220px">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="loadItems"><el-icon><Search /></el-icon>查询</el-button>
            <el-button type="success" @click="openItemDialog()"><el-icon><Plus /></el-icon>新增项目</el-button>
          </div>
          <el-table :data="itemList" v-loading="itemLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="itemName" label="项目名称" />
            <el-table-column prop="amount" label="标准金额(元)" width="120" align="center">
              <template #default="{row}"><span class="amount">¥{{ row.amount }}</span></template>
            </el-table-column>
            <el-table-column prop="cycle" label="缴费周期" width="100" align="center">
              <template #default="{row}"><el-tag effect="light">{{ cycleMap[row.cycle] || row.cycle }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{row}">
                <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="updateItemStatus(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="{row}">
                <el-button link type="primary" @click="openItemDialog(row)" class="op-btn"><el-icon><Edit /></el-icon>编辑</el-button>
                <el-popconfirm title="确定删除?" @confirm="deleteItem(row.id)">
                  <template #reference><el-button link type="danger" class="op-btn"><el-icon><Delete /></el-icon>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt-4" v-model:current-page="itemPage.current" v-model:page-size="itemPage.size" :total="itemPage.total" @current-change="loadItems" layout="total, prev, pager, next" />
        </el-tab-pane>

        <el-tab-pane label="账单生成与管理" name="bills">
          <div class="toolbar">
            <el-select v-model="billFilter.feeItemId" placeholder="选择收费项目" clearable style="width: 180px">
              <el-option v-for="item in allFeeItems" :key="item.id" :label="item.itemName" :value="item.id" />
            </el-select>
            <el-select v-model="billFilter.status" placeholder="账单状态" clearable style="width: 120px">
              <el-option label="待缴费" value="PENDING" />
              <el-option label="已缴费" value="PAID" />
              <el-option label="已逾期" value="OVERDUE" />
            </el-select>
            <el-button type="primary" @click="loadBills"><el-icon><Search /></el-icon>查询</el-button>
            <el-button type="warning" @click="generateDialogVisible = true" class="generate-btn"><el-icon><Document /></el-icon>批量生成账单</el-button>
          </div>
          <el-table :data="billList" v-loading="billLoading" stripe row-key="id">
            <el-table-column prop="billNo" label="账单编号" width="170" align="center" />
            <el-table-column label="关联对象" width="160" align="center">
              <template #default="{row}">{{ row.parkingLabel || row.houseLabel || '—' }}</template>
            </el-table-column>
            <el-table-column label="业主姓名" width="110" align="center">
              <template #default="{row}">{{ ownerNames[row.ownerId] || `ID:${row.ownerId}` }}</template>
            </el-table-column>
            <el-table-column label="费用类型" width="120" align="center">
              <template #default="{row}">{{ feeItemNames[row.feeItemId] || `ID:${row.feeItemId}` }}</template>
            </el-table-column>
            <el-table-column prop="amount" label="金额(元)" width="100" align="center">
              <template #default="{row}"><span class="amount">¥{{ row.amount?.toFixed(2) || '0.00' }}</span></template>
            </el-table-column>
            <el-table-column prop="dueDate" label="到期日" width="110" align="center" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{row}">
                <el-tag :type="billStatusMap[row.status]?.type || 'info'" effect="light">{{ billStatusMap[row.status]?.text || row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="{row}">
                <el-select v-model="row.status" size="small" @change="updateBillStatus(row)" style="width: 90px">
                  <el-option label="待缴" value="PENDING" />
                  <el-option label="已缴" value="PAID" />
                  <el-option label="逾期" value="OVERDUE" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt-4" v-model:current-page="billPage.current" v-model:page-size="billPage.size" :total="billPage.total" @current-change="loadBills" layout="total, prev, pager, next" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 收费项目弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑项目' : '新增项目'" width="500px" class="dialog">
      <el-form :model="itemForm" label-width="80px">
        <el-form-item label="项目名称"><el-input v-model="itemForm.itemName" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="itemForm.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="周期">
          <el-select v-model="itemForm.cycle">
            <el-option label="月缴" value="MONTH" />
            <el-option label="季缴" value="QUARTER" />
            <el-option label="年缴" value="YEAR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="itemDialogVisible=false">取消</el-button><el-button type="primary" @click="saveItem">保存</el-button></template>
    </el-dialog>

    <!-- 生成账单弹窗 -->
    <el-dialog v-model="generateDialogVisible" title="批量生成账单" width="620px" class="dialog" @open="loadEligibleHouses">
      <el-form>
        <el-form-item label="选择项目">
          <el-select v-model="generateFeeId" style="width: 100%" @change="onFeeItemChange">
            <el-option v-for="item in allFeeItems" :key="item.id" :label="`${item.itemName} (¥${item.amount})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="generateDueDate" type="date" placeholder="选择截止日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="缴费对象">
          <div class="house-select-area">
            <div class="house-select-header">
              <el-checkbox v-model="houseAllChecked" @change="toggleAllHouses" :indeterminate="houseIndeterminate">全选</el-checkbox>
              <span class="house-count">已选 {{ selectedHouseIds.length }} / {{ eligibleHouses.length }} {{ eligibleType === 'parking' ? '个车位' : '套房屋' }}</span>
            </div>
            <el-table :data="eligibleHouses" size="small" max-height="240" @selection-change="onHouseSelectionChange" ref="houseTableRef" row-key="id" stripe>
              <el-table-column type="selection" width="42" />
              <el-table-column prop="label" :label="eligibleType === 'parking' ? '车位编号' : '房屋位置'" />
              <el-table-column prop="ownerName" label="业主姓名" width="110" align="center" />
            </el-table>
          </div>
        </el-form-item>
        <el-alert :title="eligibleType === 'parking' ? '仅为勾选的车位生成车位管理费账单' : '仅为勾选的房屋生成该项目的待缴账单'" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer><el-button @click="generateDialogVisible=false">取消</el-button><el-button type="warning" @click="handleGenerate">确认生成</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed, nextTick } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Money, Search, Plus, Edit, Delete, Document } from '@element-plus/icons-vue'

const activeTab = ref('items')
const cycleMap = { MONTH: '月缴', QUARTER: '季缴', YEAR: '年缴' }
const billStatusMap = { PENDING: { text: '待缴', type: 'warning' }, PAID: { text: '已缴', type: 'success' }, OVERDUE: { text: '逾期', type: 'danger' } }

const itemKeyword = ref('')
const itemList = ref([])
const itemLoading = ref(false)
const itemPage = reactive({ current: 1, size: 10, total: 0 })
const itemDialogVisible = ref(false)
const itemForm = ref({ id: null, itemName: '', amount: 0, cycle: 'MONTH' })

const loadItems = async () => {
  itemLoading.value = true
  const res = await request.get('/admin/fee-items/page', { params: { ...itemPage, keyword: itemKeyword.value } })
  itemList.value = res.data.records
  itemPage.total = res.data.total
  itemLoading.value = false
}
const openItemDialog = (row = null) => {
  itemForm.value = row ? { ...row } : { id: null, itemName: '', amount: 0, cycle: 'MONTH' }
  itemDialogVisible.value = true
}
const saveItem = async () => {
  if (itemForm.value.id) await request.put(`/admin/fee-items/${itemForm.value.id}`, itemForm.value)
  else await request.post('/admin/fee-items', itemForm.value)
  itemDialogVisible.value = false; loadItems()
}
const deleteItem = async (id) => { await request.delete(`/admin/fee-items/${id}`); loadItems() }
const updateItemStatus = async (row) => { await request.put(`/admin/fee-items/${row.id}`, row) }

const billFilter = reactive({ feeItemId: null, status: null })
const billList = ref([])
const billLoading = ref(false)
const billPage = reactive({ current: 1, size: 10, total: 0 })
const allFeeItems = ref([])
const ownerNames = ref({})
const feeItemNames = ref({})
const generateDialogVisible = ref(false)
const generateFeeId = ref(null)
const generateDueDate = ref('')
const eligibleHouses = ref([])
const selectedHouseIds = ref([])
const houseTableRef = ref(null)
const eligibleType = ref('house')  // 'house' or 'parking'

const houseAllChecked = ref(false)
const houseIndeterminate = computed(() => selectedHouseIds.value.length > 0 && selectedHouseIds.value.length < eligibleHouses.value.length)

const loadEligibleHouses = async (feeItemId = null) => {
  const params = feeItemId ? { feeItemId } : {}
  const res = await request.get('/admin/bills/eligible-houses', { params })
  eligibleHouses.value = res.data || []
  // Determine type from first item
  eligibleType.value = eligibleHouses.value.length > 0 ? (eligibleHouses.value[0].type || 'house') : 'house'
  selectedHouseIds.value = []
  houseAllChecked.value = false
  // 等待表格渲染后默认全选
  await nextTick()
  if (houseTableRef.value && eligibleHouses.value.length > 0) {
    houseTableRef.value.toggleAllSelection()
  }
}

const onHouseSelectionChange = (rows) => {
  selectedHouseIds.value = rows.map(r => r.id)
  houseAllChecked.value = rows.length === eligibleHouses.value.length && rows.length > 0
}

const toggleAllHouses = (val) => {
  if (val) {
    houseTableRef.value?.toggleAllSelection()
  } else {
    houseTableRef.value?.clearSelection()
  }
}

const onFeeItemChange = (feeId) => {
  const item = allFeeItems.value.find(i => i.id === feeId)
  if (!item) return
  const d = new Date()
  const months = item.cycle === 'QUARTER' ? 3 : item.cycle === 'YEAR' ? 12 : 1
  d.setMonth(d.getMonth() + months)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  generateDueDate.value = `${yyyy}-${mm}-${dd}`
  // 根据收费项目重新筛选可收费房屋（如车位费只展示有车位的业主）
  loadEligibleHouses(feeId)
}

const loadBills = async () => {
  billLoading.value = true
  try {
    const res = await request.get('/admin/bills/page', { params: { ...billPage, ...billFilter } })
    const pageData = res.data || {}
    billList.value = Array.isArray(pageData.records) ? pageData.records : []
    billPage.total = pageData.total || 0
    billList.value.forEach(b => {
      if (b.ownerId && !ownerNames.value[b.ownerId]) ownerNames.value[b.ownerId] = `业主${b.ownerId}`
      if (b.feeItemId && !feeItemNames.value[b.feeItemId]) feeItemNames.value[b.feeItemId] = `项目${b.feeItemId}`
    })
  } catch (e) { billList.value = [] }
  finally { billLoading.value = false }
}

const updateBillStatus = async (row) => {
  await request.put(`/admin/bills/${row.id}/status?status=${row.status}`)
  ElMessage.success('状态已更新')
}
const handleGenerate = async () => {
  if (!generateFeeId.value) return ElMessage.warning('请选择收费项目')
  if (!generateDueDate.value) return ElMessage.warning('请选择截止日期')
  if (selectedHouseIds.value.length === 0) return ElMessage.warning('请至少选择一个缴费对象')
  await request.post('/admin/bills/generate', null, { params: { feeItemId: generateFeeId.value, dueDate: generateDueDate.value, targetIds: selectedHouseIds.value.join(','), targetType: eligibleType.value } })
  ElMessage.success('账单生成成功')
  generateDialogVisible.value = false
  loadBills()
}

const loadAllFeeItems = async () => {
  const res = await request.get('/admin/fee-items/page', { params: { current: 1, size: 100 } })
  allFeeItems.value = res.data.records
  res.data.records.forEach(item => { feeItemNames.value[item.id] = item.itemName })
}

onMounted(() => { loadItems(); loadAllFeeItems() })
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.fee-manage {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .header-icon {
        width: 40px;
        height: 40px;
        background: linear-gradient(135deg, #10b981, #059669);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
      }

      .header-title { font-size: 16px; font-weight: 700; color: $text-primary; }
      .header-desc { font-size: 12px; color: $text-secondary; margin-top: 2px; }
    }
  }

  .toolbar { margin-bottom: 16px; display: flex; gap: 10px; align-items: center; }
  .mt-4 { margin-top: 16px; }
  .op-btn { font-weight: 500; .el-icon { margin-right: 2px; } }

  .amount {
    font-weight: 600;
    color: $text-primary;
  }

  .generate-btn {
    background: linear-gradient(135deg, #f59e0b, #d97706) !important;
    border: none !important;
  }

  .house-select-area {
    width: 100%;
    border: 1px solid $border-color;
    border-radius: 8px;
    padding: 10px;

    .house-select-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .house-count {
        font-size: 12px;
        color: $text-secondary;
      }
    }
  }
}
</style>
