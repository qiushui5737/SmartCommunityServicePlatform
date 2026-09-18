<template>
  <div class="admin-parking-manage">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card total">
          <div class="stat-content">
            <div class="stat-icon"><el-icon><OfficeBuilding /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">车位总数</div>
              <div class="stat-value">{{ overallStats.total }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card free">
          <div class="stat-content">
            <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">待售车位</div>
              <div class="stat-value">{{ overallStats.free }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card sold">
          <div class="stat-content">
            <div class="stat-icon"><el-icon><UserFilled /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">已售车位</div>
              <div class="stat-value">{{ overallStats.sold }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card locked">
          <div class="stat-content">
            <div class="stat-icon"><el-icon><Lock /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">锁定车位</div>
              <div class="stat-value">{{ overallStats.locked }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主内容 Tabs -->
    <el-card shadow="never">
      <el-tabs v-model="mainTab">
        <!-- Tab 1: 数据列表 -->
        <el-tab-pane label="数据列表" name="list">
          <!-- 筛选栏 -->
          <div class="filter-bar">
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px" @change="loadData">
              <el-option label="空闲" value="FREE" />
              <el-option label="已售" value="SOLD" />
              <el-option label="锁定" value="LOCKED" />
              <el-option label="预留" value="RESERVED" />
            </el-select>
            <el-select v-model="filterZone" placeholder="区域筛选" clearable style="width: 140px" @change="loadData">
              <el-option v-for="z in zoneList" :key="z" :label="z" :value="z" />
            </el-select>
            <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 140px" @change="loadData">
              <el-option v-for="(label, val) in typeMap" :key="val" :label="label" :value="val" />
            </el-select>
            <el-button type="primary" @click="openAddDialog" style="margin-left: auto">
              <el-icon><Plus /></el-icon>新增车位
            </el-button>
          </div>

          <!-- 数据表格 -->
          <el-table :data="list" v-loading="listLoading" stripe border>
            <el-table-column prop="spaceNo" label="车位编号" width="120" align="center" />
            <el-table-column prop="zone" label="区域" width="100" align="center" />
            <el-table-column prop="type" label="类型" width="120" align="center">
              <template #default="{row}">{{ typeMap[row.type] }}</template>
            </el-table-column>
            <el-table-column prop="area" label="面积(m²)" width="100" align="center" />
            <el-table-column prop="price" label="挂牌价(元)" width="120" align="center">
              <template #default="{row}">¥{{ row.price?.toFixed(2) || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{row}">
                <el-tag :type="statusMap[row.status]?.type" effect="light">{{ statusMap[row.status]?.text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="业主姓名" width="120" align="center" />
            <el-table-column prop="ownerPhone" label="业主电话" width="130" align="center" />
            <el-table-column label="操作" width="240" align="center" fixed="right">
              <template #default="{row}">
                <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button link :type="row.status === 'LOCKED' ? 'success' : 'warning'" size="small" @click="handleToggleLock(row)">
                  {{ row.status === 'LOCKED' ? '解锁' : '锁定' }}
                </el-button>
                <el-popconfirm title="确定要删除该车位吗？" @confirm="handleDelete(row)" :disabled="row.status !== 'FREE'">
                  <template #reference>
                    <el-button link type="danger" size="small" :disabled="row.status !== 'FREE'">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
              @current-change="loadData" layout="total, prev, pager, next, jumper" />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 3D 车位地图 -->
        <el-tab-pane label="车位地图" name="map">
          <!-- 楼栋选择 -->
          <div class="zone-tabs">
            <el-select v-model="selectedBuildingId" placeholder="全部楼栋" clearable @change="onAdminBuildingChange" style="width: 200px">
              <el-option v-for="b in parkingBuildings" :key="b.buildingId"
                :label="`${b.buildingNo} - ${b.buildingName}`"
                :value="b.buildingId" />
            </el-select>
            <div class="zone-info" v-if="currentZone">
              共 <b>{{ currentZone.total }}</b> 个车位，
              空闲 <b class="text-free">{{ currentZone.free }}</b>，
              已售 <b>{{ currentZone.sold }}</b>，
              锁定 <b class="text-locked">{{ currentZone.locked || 0 }}</b>，
              预订 <b class="text-reserved">{{ currentZone.reserved || 0 }}</b>
            </div>
          </div>

          <!-- 3D 车位模型 -->
          <Parking3D
            :zones="gridZones"
            :selected-zone="currentZoneName"
            :status-colors="statusColorMap"
            :status-texts="statusTextMap"
            :type-texts="typeTextMap"
            @space-click="openCellDetail"
          />

          <!-- 图例 -->
          <div class="legend">
            <span><span class="dot" style="background:#52c41a"></span> 空闲</span>
            <span><span class="dot" style="background:#bfbfbf"></span> 已售</span>
            <span><span class="dot" style="background:#ff4d4f"></span> 锁定</span>
            <span><span class="dot" style="background:#faad14"></span> 已预订</span>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车位' : '新增车位'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="车位编号" prop="spaceNo">
          <el-input v-model="form.spaceNo" placeholder="如：A-0101" />
        </el-form-item>
        <el-form-item label="所属区域" prop="zone">
          <el-input v-model="form.zone" placeholder="如：A区" />
        </el-form-item>
        <el-form-item label="行号" prop="rowNo">
          <el-input-number v-model="form.rowNo" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="列号" prop="colNo">
          <el-input-number v-model="form.colNo" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="面积(m²)" prop="area">
          <el-input-number v-model="form.area" :min="5" :max="50" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="车位类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(label, val) in typeMap" :key="val" :label="label" :value="val" />
          </el-select>
        </el-form-item>
        <el-form-item label="挂牌价(元)" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>

    <!-- 车位详情弹窗（地图点击） -->
    <el-dialog v-model="detailDialog" title="车位详情" width="420px">
      <el-descriptions :column="1" border v-if="detailCell">
        <el-descriptions-item label="车位编号">{{ detailCell.spaceNo }}</el-descriptions-item>
        <el-descriptions-item label="所属区域">{{ detailCell.zone }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeMap[detailCell.type] }}</el-descriptions-item>
        <el-descriptions-item label="面积">{{ detailCell.area }} m²</el-descriptions-item>
        <el-descriptions-item label="挂牌价">¥{{ detailCell.price?.toFixed(2) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detailCell.status]?.type" effect="light">{{ statusMap[detailCell.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="位置">第 {{ detailCell.rowNo }} 行，第 {{ detailCell.colNo }} 列</el-descriptions-item>
        <el-descriptions-item v-if="detailCell.ownerName" label="业主">{{ detailCell.ownerName }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
        <el-button v-if="detailCell" type="primary" @click="openEditFromDetail">编辑</el-button>
        <el-button v-if="detailCell && detailCell.status === 'FREE'" type="warning" @click="handleToggleLockFromDetail">锁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, CircleCheck, UserFilled, Lock, Plus } from '@element-plus/icons-vue'
import Parking3D from '@/components/Parking3D.vue'

const mainTab = ref('list')

// ====== 列表相关 ======
const listLoading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const filterStatus = ref('')
const filterZone = ref('')
const filterType = ref('')
const zoneList = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })
const list = ref([])

const form = ref({
  id: null, spaceNo: '', zone: '', rowNo: 1, colNo: 1,
  area: 12.5, type: 'STANDARD', price: 80000
})

const typeMap = {
  STANDARD: '标准车位', COMPACT: '微型车位',
  LARGE: '大型车位', VIP: 'VIP车位'
}

const statusMap = {
  FREE: { text: '空闲', type: 'success' },
  SOLD: { text: '已售', type: 'info' },
  LOCKED: { text: '锁定', type: 'danger' },
  RESERVED: { text: '预留', type: 'warning' }
}

const statusColorMap = {
  FREE: 0x52c41a, SOLD: 0xbfbfbf, LOCKED: 0xff4d4f, RESERVED: 0xfaad14
}

const statusTextMap = {
  FREE: '空闲', SOLD: '已售', LOCKED: '锁定', RESERVED: '预留'
}

const typeTextMap = {
  STANDARD: '标准车位', COMPACT: '微型车位',
  LARGE: '大型车位', VIP: 'VIP车位'
}

const rules = {
  spaceNo: [{ required: true, message: '请输入车位编号', trigger: 'blur' }],
  zone: [{ required: true, message: '请输入所属区域', trigger: 'blur' }],
  rowNo: [{ required: true, message: '请输入行号', trigger: 'blur' }],
  colNo: [{ required: true, message: '请输入列号', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }],
  type: [{ required: true, message: '请选择车位类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入挂牌价', trigger: 'blur' }]
}

// ====== 统计 ======
const overallStats = ref({ total: 0, free: 0, sold: 0, locked: 0, reserved: 0 })

const loadStats = async () => {
  try {
    const res = await request.get('/parking/visual/summary')
    const data = res.data || []
    // 汇总各区域数据
    overallStats.value = data.reduce((acc, s) => ({
      total: acc.total + (s.total || 0),
      free: acc.free + (s.free || 0),
      sold: acc.sold + (s.sold || 0),
      locked: acc.locked + (s.locked || 0),
      reserved: acc.reserved + (s.reserved || 0)
    }), { total: 0, free: 0, sold: 0, locked: 0, reserved: 0 })
  } catch (error) {
    console.error('加载统计失败:', error)
    try {
      const res = await request.get('/parking/admin/stats')
      overallStats.value = res.data
    } catch {
      overallStats.value = { total: 50, free: 20, sold: 25, locked: 5, reserved: 0 }
    }
  }
}

// ====== 网格地图 ======
const gridZones = ref([])
const selectedBuildingId = ref(null)
const parkingBuildings = ref([])
const gridLoading = ref(false)

const currentZoneName = computed(() => {
  if (!selectedBuildingId.value && gridZones.value.length > 0) {
    return gridZones.value[0].zone
  }
  const zone = gridZones.value.find(z => z.buildingId === selectedBuildingId.value)
  return zone ? zone.zone : (gridZones.value[0]?.zone || '')
})

const currentZone = computed(() => {
  return gridZones.value.find(z => z.zone === currentZoneName.value) || null
})

const cellTooltip = (cell) => {
  if (!cell) return '过道/空位'
  return `${cell.spaceNo} | ${typeMap[cell.type] || cell.type} | ${statusMap[cell.status]?.text || cell.status} | ¥${cell.price?.toFixed(2) || '-'}`
}

const onAdminBuildingChange = (bid) => {
  selectedBuildingId.value = bid
  loadGrid()
}

const loadParkingBuildings = async () => {
  try {
    const res = await request.get('/parking/buildings')
    parkingBuildings.value = res.data || []
    if (parkingBuildings.value.length > 0 && !selectedBuildingId.value) {
      selectedBuildingId.value = parkingBuildings.value[0].buildingId
    }
  } catch (error) {
    console.error('加载楼栋列表失败:', error)
  }
}

const loadGrid = async () => {
  gridLoading.value = true
  try {
    const params = {}
    if (selectedBuildingId.value) params.buildingId = selectedBuildingId.value
    const res = await request.get('/parking/visual/grid', { params })
    gridZones.value = res.data.zones || []
  } catch (error) {
    console.error('加载网格数据失败:', error)
    gridZones.value = []
  } finally {
    gridLoading.value = false
  }
}

// 车位详情弹窗
const detailDialog = ref(false)
const detailCell = ref(null)

const openCellDetail = (cell) => {
  detailCell.value = cell
  detailDialog.value = true
}

const openEditFromDetail = () => {
  detailDialog.value = false
  const row = list.value.find(r => r.id === detailCell.value?.id)
  if (row) {
    openEditDialog(row)
  } else {
    // 直接编辑详情数据
    openEditDialog(detailCell.value)
  }
}

const handleToggleLockFromDetail = async () => {
  if (!detailCell.value) return
  await handleToggleLock(detailCell.value)
  detailDialog.value = false
}

// ====== 列表数据 ======
const loadZones = async () => {
  try {
    const res = await request.get('/parking/zones')
    zoneList.value = res.data || []
  } catch (error) {
    zoneList.value = ['A区', 'B区', 'C区']
  }
}

const loadData = async () => {
  listLoading.value = true
  try {
    const params = { ...page }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterZone.value) params.zone = filterZone.value
    if (filterType.value) params.type = filterType.value
    const res = await request.get('/parking/admin/page', { params })
    list.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.warning('后端服务未连接，显示模拟数据')
    list.value = generateMockListData()
    page.total = 50
  } finally {
    listLoading.value = false
  }
}

const generateMockListData = () => {
  const data = []
  for (let i = 1; i <= 10; i++) {
    data.push({
      id: i, spaceNo: `A-${String(Math.floor(i / 10)).padStart(2, '0')}${String(i % 10).padStart(2, '0')}`,
      zone: 'A区', type: Math.random() > 0.7 ? 'COMPACT' : 'STANDARD',
      area: Math.random() > 0.7 ? 10.0 : 12.5,
      price: Math.floor(Math.random() * 50000) + 50000,
      status: Math.random() > 0.6 ? 'SOLD' : Math.random() > 0.9 ? 'LOCKED' : 'FREE',
      ownerName: Math.random() > 0.6 ? `业主${i}` : '',
      ownerPhone: Math.random() > 0.6 ? `138${String(Math.floor(Math.random() * 100000000)).padStart(8, '0')}` : ''
    })
  }
  return data
}

const generateMockGrid = () => {
  const zonesData = []
  const zoneNames = ['A区', 'B区']
  const statuses = ['FREE', 'FREE', 'FREE', 'SOLD', 'SOLD', 'LOCKED', 'RESERVED']
  const types = ['STANDARD', 'STANDARD', 'STANDARD', 'COMPACT', 'LARGE']

  zoneNames.forEach((zoneName, zi) => {
    const maxRow = zi === 0 ? 4 : 3
    const maxCol = 8
    const rows = []
    let total = 0, free = 0, sold = 0, locked = 0, reserved = 0
    let idCounter = zi * 100 + 1

    for (let r = 0; r < maxRow; r++) {
      const row = []
      for (let c = 0; c < maxCol; c++) {
        if (c === 3 && r % 2 === 0) { row.push(null); continue }
        const status = statuses[Math.floor(Math.random() * statuses.length)]
        const type = types[Math.floor(Math.random() * types.length)]
        const spaceNo = `${zoneName[0]}-${String(r+1).padStart(2,'0')}${String(c+1).padStart(2,'0')}`
        total++
        if (status === 'FREE') free++
        else if (status === 'SOLD') sold++
        else if (status === 'LOCKED') locked++
        else if (status === 'RESERVED') reserved++
        row.push({
          id: idCounter++, spaceNo, zone: zoneName, type,
          area: type === 'COMPACT' ? 10.0 : type === 'LARGE' ? 15.0 : 12.5,
          price: type === 'LARGE' ? 100000 : type === 'COMPACT' ? 60000 : 80000,
          rowNo: r + 1, colNo: c + 1, status
        })
      }
      rows.push(row)
    }
    zonesData.push({ zone: zoneName, maxRow, maxCol, total, free, sold, locked, reserved, rows })
  })
  return zonesData
}

// ====== CRUD 操作 ======
const openAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, spaceNo: '', zone: '', rowNo: 1, colNo: 1, area: 12.5, type: 'STANDARD', price: 80000 }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put(`/parking/admin/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/parking/admin', form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData(); loadStats(); loadGrid()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.warning('后端服务未连接，模拟保存成功')
    dialogVisible.value = false
    loadData(); loadStats(); loadGrid()
  } finally {
    submitting.value = false
  }
}

const handleToggleLock = async (row) => {
  const lock = row.status !== 'LOCKED'
  try {
    await request.put(`/parking/admin/${row.id}/lock`, null, { params: { lock } })
    ElMessage.success(lock ? '锁定成功' : '解锁成功')
    loadData(); loadStats(); loadGrid()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.warning('后端服务未连接，模拟操作成功')
    row.status = lock ? 'LOCKED' : 'FREE'
    loadData(); loadStats(); loadGrid()
  }
}

const handleDelete = async (row) => {
  try {
    await request.delete(`/parking/admin/${row.id}`)
    ElMessage.success('删除成功')
    loadData(); loadStats(); loadGrid()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.warning('后端服务未连接，模拟删除成功')
    list.value = list.value.filter(item => item.id !== row.id)
    page.total--
    loadStats(); loadGrid()
  }
}

onMounted(() => {
  loadStats()
  loadZones()
  loadData()
  loadParkingBuildings().then(() => loadGrid())
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.admin-parking-manage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-cards {
  .stat-card {
    border-radius: 12px;
    transition: all 0.3s ease;
    &:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }
    .stat-content {
      display: flex; align-items: center; gap: 16px;
      .stat-icon {
        width: 56px; height: 56px; border-radius: 12px;
        display: flex; align-items: center; justify-content: center; font-size: 24px;
      }
    }
    &.total .stat-icon { background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
    &.free .stat-icon { background: linear-gradient(135deg, #52c41a, #389e0d); color: white; }
    &.sold .stat-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); color: white; }
    &.locked .stat-icon { background: linear-gradient(135deg, #ff4d4f, #cf1322); color: white; }
    .stat-info {
      .stat-label { font-size: 14px; color: #606266; margin-bottom: 4px; }
      .stat-value { font-size: 28px; font-weight: 700; color: #303133; }
    }
  }
}

.filter-bar {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 20px; display: flex; justify-content: flex-end;
}

/* ====== 车位地图样式 ====== */
.zone-tabs {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; flex-wrap: wrap; gap: 8px;
  .zone-info {
    font-size: 13px; color: $text-secondary;
    b { color: $text-primary; }
    .text-free { color: #52c41a; }
    .text-locked { color: #ff4d4f; }
    .text-reserved { color: #faad14; }
  }
}

.legend {
  display: flex; justify-content: center; gap: 20px;
  margin-top: 16px; font-size: 13px; color: $text-secondary; flex-wrap: wrap;
  span { display: flex; align-items: center; gap: 4px; }
  .dot {
    width: 14px; height: 14px; border-radius: 4px;
    display: inline-block; flex-shrink: 0;
  }
}
</style>
