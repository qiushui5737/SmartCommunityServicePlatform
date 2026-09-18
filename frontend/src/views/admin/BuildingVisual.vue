<template>
  <div class="building-visual-3d">
    <!-- 顶部统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="8" v-for="s in summaryCards" :key="s.label">
        <div class="summary-card" :style="{ borderLeftColor: s.color }">
          <div class="summary-value" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="summary-label">{{ s.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 3D 模型 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#8b5cf6"><Location /></el-icon>
            <div>
              <span class="header-title">3D 楼栋模型</span>
              <span class="header-desc">拖拽旋转查看楼栋入住情况</span>
            </div>
          </div>
          <el-select v-model="buildingId" placeholder="选择楼栋" style="width: 220px" @change="loadMap">
            <el-option v-for="b in buildings" :key="b.id" :label="`${b.buildingNo} - ${b.name}`" :value="b.id" />
          </el-select>
        </div>
      </template>

      <!-- 入住率 -->
      <div class="occupancy-bar" v-if="houses.length">
        <span class="occ-label">入住率</span>
        <el-progress :percentage="occupancyRate" :stroke-width="12" :show-text="false" style="flex:1; margin: 0 16px;"
          :color="[{ color: '#ef4444', percentage: 30 }, { color: '#f59e0b', percentage: 60 }, { color: '#22c55e', percentage: 100 }]" />
        <span class="occ-pct">{{ occupancyRate }}%</span>
      </div>

      <!-- 3D 场景 -->
      <Building3D :houses="houses" :unit-list="unitList" :floor-list="floorList" @room-click="onRoomClick" />

      <!-- 图例 -->
      <div class="legend">
        <div class="legend-item"><span class="dot" style="background:#22c55e"></span>已入住</div>
        <div class="legend-item"><span class="dot" style="background:#94a3b8"></span>空置</div>
      </div>
    </el-card>

    <!-- 详情 + 办理入住弹窗 -->
    <el-dialog v-model="showDetail" :title="detail.status === 'VACANT' ? '办理入住' : '房屋详情'" width="420px">
      <div class="detail-content">
        <div class="detail-row"><span class="dl">房间号</span><span class="dv">{{ detail.roomNo }}</span></div>
        <div class="detail-row"><span class="dl">面积</span><span class="dv">{{ detail.area }} ㎡</span></div>
        <div class="detail-row"><span class="dl">状态</span><el-tag :type="statusTag[detail.status]?.type">{{ statusTag[detail.status]?.text }}</el-tag></div>
        <div class="detail-row"><span class="dl">所属单元</span><span class="dv">{{ detail.unitNo }}</span></div>
        <div class="detail-row"><span class="dl">楼层</span><span class="dv">{{ detail.floorNum }}F</span></div>
        <template v-if="detail.ownerId">
          <div class="detail-row"><span class="dl">当前业主ID</span><span class="dv">{{ detail.ownerId }}</span></div>
        </template>
        <el-divider v-if="detail.status === 'VACANT'" />
        <div v-if="detail.status === 'VACANT'" class="checkin-form">
          <div class="checkin-title">办理入住</div>
          <el-input v-model.number="checkinOwnerId" placeholder="请输入业主ID" clearable>
            <template #prepend>业主ID</template>
          </el-input>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button v-if="detail.status === 'VACANT'" type="primary" :loading="checkinLoading" @click="doCheckin">确认入住</el-button>
        <el-popconfirm v-else title="确认办理退租？清空业主绑定？" @confirm="doCheckout">
          <template #reference><el-button type="danger">办理退租</el-button></template>
        </el-popconfirm>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import Building3D from '@/components/Building3D.vue'

const buildings = ref([])
const buildingId = ref(null)
const houses = ref([])
const showDetail = ref(false)
const detail = ref({})

const statusTag = {
  OCCUPIED: { text: '已入住', type: 'success' },
  VACANT:   { text: '空置',   type: 'info' }
}

const unitList = computed(() => [...new Set(houses.value.map(h => h.unitNo))])
const floorList = computed(() => [...new Set(houses.value.map(h => h.floorNum))].sort((a, b) => b - a))
const occupancyRate = computed(() => {
  if (!houses.value.length) return 0
  const occ = houses.value.filter(h => h.status === 'OCCUPIED').length
  return Math.round((occ / houses.value.length) * 100)
})
const summaryCards = computed(() => {
  const t = houses.value.length
  const o = houses.value.filter(h => h.status === 'OCCUPIED').length
  const v = houses.value.filter(h => h.status === 'VACANT').length
  return [
    { label: '总户数', value: t, color: '#6366f1' },
    { label: '已入住', value: o, color: '#22c55e' },
    { label: '空置',   value: v, color: '#94a3b8' }
  ]
})

const checkinOwnerId = ref(null)
const checkinLoading = ref(false)

const onRoomClick = (room) => {
  detail.value = room
  checkinOwnerId.value = null
  showDetail.value = true
}

const doCheckin = async () => {
  if (!checkinOwnerId.value) { ElMessage.warning('请输入业主ID'); return }
  checkinLoading.value = true
  try {
    const res = await request.post('/houses/checkin', null, { params: { houseId: detail.value.id, ownerId: checkinOwnerId.value } })
    ElMessage.success('入住办理成功')
    showDetail.value = false
    loadMap()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '办理失败')
  } finally { checkinLoading.value = false }
}

const doCheckout = async () => {
  try {
    await request.post('/houses/checkout', null, { params: { houseId: detail.value.id } })
    ElMessage.success('退租办理成功')
    showDetail.value = false
    loadMap()
  } catch (e) {
    ElMessage.error('退租失败')
  }
}

const loadBuildings = async () => {
  const res = await request.get('/buildings/page', { params: { current: 1, size: 100 } })
  buildings.value = res.data?.records || []
  if (buildings.value.length) { buildingId.value = buildings.value[0].id; loadMap() }
}
const loadMap = async () => {
  if (!buildingId.value) return
  const res = await request.get('/houses/map-data', { params: { buildingId: buildingId.value } })
  houses.value = (res.data || []).map(h => ({ ...h, floorNum: Number(h.floorNum) }))
}

onMounted(loadBuildings)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.stats-row { margin-bottom: 16px; }
.summary-card {
  background: white; border-radius: 12px; padding: 18px 20px;
  border: 1px solid $border-color; border-left: 4px solid;
  .summary-value { font-size: 28px; font-weight: 800; }
  .summary-label { font-size: 13px; color: $text-secondary; margin-top: 4px; }
}

.main-card {
  .card-header {
    display: flex; justify-content: space-between; align-items: center;
    .header-left { display: flex; align-items: center; gap: 10px;
      .header-title { font-size: 16px; font-weight: 700; margin-right: 8px; }
      .header-desc { font-size: 12px; color: $text-secondary; }
    }
  }
}

.occupancy-bar {
  display: flex; align-items: center; padding: 12px 16px; margin-bottom: 16px;
  background: $gray-50; border-radius: 10px;
  .occ-label { font-size: 13px; color: $text-secondary; white-space: nowrap; }
  .occ-pct { font-weight: 700; font-size: 14px; }
}

.legend {
  margin-top: 16px; display: flex; justify-content: center; gap: 24px;
  .legend-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: $text-secondary; padding: 6px 14px; background: $gray-50; border-radius: 8px; }
  .dot { width: 14px; height: 14px; border-radius: 4px; }
}

.detail-content {
  .detail-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid $border-color;
    &:last-child { border-bottom: none; }
    .dl { color: $text-secondary; font-size: 14px; }
    .dv { font-weight: 600; color: $text-primary; font-size: 14px; }
  }
  .checkin-form {
    margin-top: 4px;
    .checkin-title { font-size: 13px; font-weight: 600; color: $text-primary; margin-bottom: 10px; }
  }
}
</style>
