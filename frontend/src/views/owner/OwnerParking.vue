<template>
  <div class="owner-parking">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: 车位选购地图 -->
        <el-tab-pane label="车位选购" name="visual">
          <!-- 楼栋选择 -->
          <div class="building-select">
            <span class="select-label">选择楼栋：</span>
            <el-select v-model="selectedBuildingId" placeholder="全部楼栋" clearable @change="onBuildingChange" style="width: 200px">
              <el-option v-for="b in parkingBuildings" :key="b.buildingId"
                :label="`${b.buildingNo} - ${b.buildingName}`"
                :value="b.buildingId">
                <span>{{ b.buildingNo }} - {{ b.buildingName }}</span>
                <span style="float: right; color: #94a3b8; font-size: 12px">{{ b.free }}空闲/{{ b.total }}总</span>
              </el-option>
            </el-select>
          </div>

          <!-- 楼栋统计卡片 -->
          <el-row :gutter="12" class="summary-cards" v-if="parkingBuildings.length">
            <el-col :span="4" v-for="b in parkingBuildings" :key="b.buildingId">
              <div class="summary-item" :class="{ active: selectedBuildingId === b.buildingId }" @click="onBuildingClick(b.buildingId)">
                <div class="summary-zone">{{ b.buildingName }}</div>
                <div class="summary-stats">
                  <span class="stat-free">{{ b.free }} 空闲</span>
                  <span class="stat-sep">/</span>
                  <span class="stat-total">{{ b.total }} 总计</span>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 3D 车位模型 -->
          <Parking3D
            :zones="gridZones"
            :selected-zone="currentZoneName"
            :status-colors="statusColorMap"
            :status-texts="statusLabel"
            :type-texts="typeMap"
            @space-click="onSpaceClick"
          />

          <!-- 图例 -->
          <div class="legend">
            <span><span class="dot" style="background:#52c41a"></span> 空闲 (可购买)</span>
            <span><span class="dot" style="background:#bfbfbf"></span> 已售</span>
            <span><span class="dot" style="background:#ff4d4f"></span> 锁定</span>
            <span><span class="dot" style="background:#faad14"></span> 已预订</span>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 我的车位 -->
        <el-tab-pane label="我的车位" name="mine">
          <el-table :data="mySpaces" stripe border>
            <el-table-column prop="spaceNo" label="车位编号" width="120" />
            <el-table-column prop="zone" label="所属区域" width="100" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{row}">{{ typeMap[row.type] }}</template>
            </el-table-column>
            <el-table-column prop="area" label="面积(m²)" width="100" align="center" />
            <el-table-column prop="purchasePrice" label="成交价" width="120">
              <template #default="{row}">¥{{ row.purchasePrice?.toFixed(2) || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="purchaseTime" label="购买时间" width="160" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{row}">
                <el-popconfirm title="确定要释放该车位吗？" @confirm="handleRelease(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">释放</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="mySpaces.length===0" description="暂无已购车位" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 购买确认弹窗 -->
    <el-dialog v-model="buyDialog" title="确认购买车位" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="车位编号">{{ targetSpace.spaceNo }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ targetSpace.zone }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeMap[targetSpace.type] }}</el-descriptions-item>
        <el-descriptions-item label="面积(m²)">{{ targetSpace.area }}</el-descriptions-item>
        <el-descriptions-item label="售价">
          <span style="color:#F56C6C; font-weight:bold">¥{{ targetSpace.price?.toFixed(2) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="buyDialog=false">取消</el-button>
        <el-button type="primary" @click="confirmPurchase" :loading="buying">确认购买</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import Parking3D from '@/components/Parking3D.vue'

const activeTab = ref('visual')
const mySpaces = ref([])
const buyDialog = ref(false)
const buying = ref(false)
const targetSpace = ref({})
const gridLoading = ref(false)

// 楼栋车位数据
const parkingBuildings = ref([])
const selectedBuildingId = ref(null)

// 网格数据
const gridZones = ref([])
const selectedZone = ref('')

const typeMap = {
  STANDARD: '标准车位',
  COMPACT: '微型车位',
  LARGE: '大型车位',
  VIP: 'VIP车位'
}

const statusLabel = {
  FREE: '空闲',
  SOLD: '已售',
  LOCKED: '锁定',
  RESERVED: '预订'
}

const statusColorMap = {
  FREE: 0x52c41a,
  SOLD: 0xbfbfbf,
  LOCKED: 0xff4d4f,
  RESERVED: 0xfaad14
}

// 当前选中楼栋对应的zone名称
const currentZoneName = computed(() => {
  if (!selectedBuildingId.value && gridZones.value.length > 0) {
    return gridZones.value[0].zone
  }
  const zone = gridZones.value.find(z => z.buildingId === selectedBuildingId.value)
  return zone ? zone.zone : (gridZones.value[0]?.zone || '')
})

// 当前选中区域数据
const currentZone = computed(() => {
  return gridZones.value.find(z => z.zone === currentZoneName.value) || null
})

// 点击3D车位
const onSpaceClick = (space) => {
  if (space.status === 'FREE') {
    openBuyDialog(space)
  }
}

// 切换楼栋
const onBuildingChange = (bid) => {
  selectedBuildingId.value = bid
  loadGrid()
}
const onBuildingClick = (bid) => {
  selectedBuildingId.value = bid
  loadGrid()
}

// ====== 数据加载 ======

// 加载楼栋列表
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

// 加载网格数据（核心接口）
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

// 加载统计概览
const loadSummary = async () => {
  // 统计由楼栋列表提供，不再单独加载
}

// 加载我的车位
const loadMySpaces = async () => {
  try {
    const res = await request.get('/parking/my')
    mySpaces.value = res.data
  } catch (error) {
    console.error('加载我的车位失败:', error)
    ElMessage.warning('后端服务未连接，显示模拟数据')
    mySpaces.value = [
      { id: 1, spaceNo: 'A-001', zone: 'A区', type: 'STANDARD', area: 12.5, purchasePrice: 80000, purchaseTime: '2024-01-15 10:30:00' },
      { id: 2, spaceNo: 'B-003', zone: 'B区', type: 'COMPACT', area: 10.0, purchasePrice: 60000, purchaseTime: '2024-02-20 14:20:00' }
    ]
  }
}

// 释放车位
const handleRelease = async (row) => {
  try {
    await request.put(`/parking/release/${row.id}`)
    ElMessage.success('释放成功')
    loadMySpaces()
    loadGrid()
    loadSummary()
  } catch (error) {
    console.error('释放车位失败:', error)
    ElMessage.warning('后端服务未连接，模拟释放成功')
    mySpaces.value = mySpaces.value.filter(s => s.id !== row.id)
    loadGrid()
    loadSummary()
  }
}

// 打开购买弹窗
const openBuyDialog = (cell) => {
  targetSpace.value = cell
  buyDialog.value = true
}

// 确认购买
const confirmPurchase = async () => {
  buying.value = true
  try {
    await request.post(`/parking/purchase/${targetSpace.value.id}`)
    ElMessage.success('购买成功！')
    buyDialog.value = false
    loadGrid()
    loadSummary()
    loadMySpaces()
  } catch (error) {
    console.error('购买失败:', error)
    ElMessage.warning('后端服务未连接，模拟购买成功')
    buyDialog.value = false
    loadGrid()
    loadSummary()
    loadMySpaces()
  } finally {
    buying.value = false
  }
}

// ====== 模拟数据 ======
const generateMockGrid = () => {
  const zonesData = []
  const zoneNames = ['A区', 'B区']
  const types = ['STANDARD', 'STANDARD', 'STANDARD', 'COMPACT', 'LARGE']
  const statuses = ['FREE', 'FREE', 'FREE', 'SOLD', 'SOLD', 'LOCKED', 'RESERVED']

  zoneNames.forEach((zoneName, zi) => {
    const maxRow = zi === 0 ? 4 : 3
    const maxCol = 8
    const rows = []
    let total = 0, free = 0, sold = 0, locked = 0, reserved = 0
    let idCounter = zi * 100 + 1

    for (let r = 0; r < maxRow; r++) {
      const row = []
      for (let c = 0; c < maxCol; c++) {
        // 每行第三个位置设为过道（null），模拟过道效果
        if (c === 3 && r % 2 === 0) {
          row.push(null)
          continue
        }
        const status = statuses[Math.floor(Math.random() * statuses.length)]
        const type = types[Math.floor(Math.random() * types.length)]
        const spaceNo = `${zoneName[0]}-${String(r+1).padStart(2,'0')}${String(c+1).padStart(2,'0')}`
        total++
        if (status === 'FREE') free++
        else if (status === 'SOLD') sold++
        else if (status === 'LOCKED') locked++
        else if (status === 'RESERVED') reserved++
        row.push({
          id: idCounter++,
          spaceNo,
          zone: zoneName,
          type,
          area: type === 'COMPACT' ? 10.0 : type === 'LARGE' ? 15.0 : 12.5,
          price: type === 'VIP' ? 150000 : type === 'LARGE' ? 100000 : type === 'COMPACT' ? 60000 : 80000,
          rowNo: r + 1,
          colNo: c + 1,
          status
        })
      }
      rows.push(row)
    }
    zonesData.push({ zone: zoneName, maxRow, maxCol, total, free, sold, locked, reserved, rows })
  })
  return zonesData
}

onMounted(() => {
  loadParkingBuildings().then(() => loadGrid())
  loadMySpaces()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-parking {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 楼栋选择 */
.building-select {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  .select-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }
}

/* 统计概览卡片 */
.summary-cards {
  margin-bottom: 20px;
}

.summary-item {
  background: $bg-tertiary;
  border: 2px solid transparent;
  border-radius: $radius;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;

  &:hover {
    border-color: $primary-200;
    background: $primary-50;
  }

  &.active {
    border-color: $primary;
    background: $primary-50;
    box-shadow: 0 0 0 3px rgba($primary, 0.1);
  }

  .summary-zone {
    font-size: 15px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 6px;
  }

  .summary-stats {
    font-size: 13px;
    color: $text-secondary;

    .stat-free { color: #52c41a; font-weight: 600; }
    .stat-sep { color: $text-tertiary; margin: 0 2px; }
    .stat-total { font-weight: 600; }
  }
}

/* 图例 */
.legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
  font-size: 13px;
  color: $text-secondary;
  flex-wrap: wrap;

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .dot {
    width: 14px;
    height: 14px;
    border-radius: 4px;
    display: inline-block;
    flex-shrink: 0;
  }
}
</style>
