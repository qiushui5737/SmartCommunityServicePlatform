<template>
  <div class="owner-building-3d">
    <!-- 楼栋选择 -->
    <div class="building-selector">
      <el-select v-model="buildingId" placeholder="选择楼栋" @change="loadMap">
        <el-option v-for="b in buildings" :key="b.id" :label="`${b.buildingNo} - ${b.name}`" :value="b.id" />
      </el-select>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-item" v-for="s in stats" :key="s.label">
        <div class="stat-val" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-lbl">{{ s.label }}</div>
      </div>
    </div>

    <!-- 3D 模型 -->
    <div class="model-card">
      <div class="model-header">
        <span>3D 楼栋入住模型</span>
        <div class="occ-badge" v-if="houses.length">
          入住率 <strong>{{ occupancyRate }}%</strong>
        </div>
      </div>
      <Building3D :houses="houses" :unit-list="unitList" :floor-list="floorList" @room-click="onRoomClick" />
    </div>

    <!-- 图例 -->
    <div class="legend-row">
      <div class="legend-item"><span class="dot" style="background:#22c55e"></span>已入住</div>
      <div class="legend-item"><span class="dot" style="background:#94a3b8"></span>空置</div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetail" title="房屋详情" width="360px">
      <div class="detail-grid">
        <div class="dg-item"><span class="dg-l">房间号</span><span class="dg-v">{{ detail.roomNo }}</span></div>
        <div class="dg-item"><span class="dg-l">面积</span><span class="dg-v">{{ detail.area }} ㎡</span></div>
        <div class="dg-item"><span class="dg-l">状态</span><el-tag :type="statusTag[detail.status]?.type">{{ statusTag[detail.status]?.text }}</el-tag></div>
        <div class="dg-item"><span class="dg-l">楼层</span><span class="dg-v">{{ detail.floorNum }}F</span></div>
      </div>
      <template #footer><el-button @click="showDetail = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
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
const stats = computed(() => {
  const t = houses.value.length
  const o = houses.value.filter(h => h.status === 'OCCUPIED').length
  const v = houses.value.filter(h => h.status === 'VACANT').length
  return [
    { label: '总户数', value: t, color: '#10b981' },
    { label: '已入住', value: o, color: '#059669' },
    { label: '空置',   value: v, color: '#64748b' }
  ]
})

const onRoomClick = (room) => { detail.value = room; showDetail.value = true }

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
.owner-building-3d {
  padding: 20px;
  min-height: 100%;
  background: #f5f7fa;
}

.building-selector {
  margin-bottom: 16px;
}

.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px;
  .stat-item {
    background: white; border-radius: 12px; padding: 16px; text-align: center;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
    .stat-val { font-size: 24px; font-weight: 800; }
    .stat-lbl { font-size: 12px; color: #94a3b8; margin-top: 4px; }
  }
}

.model-card {
  background: white; border-radius: 14px; overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  .model-header {
    padding: 14px 20px; border-bottom: 1px solid #e2e8f0;
    display: flex; justify-content: space-between; align-items: center;
    font-weight: 600; color: #334155;
    .occ-badge {
      font-size: 13px; font-weight: 400; color: #64748b;
      strong { color: #10b981; }
    }
  }
}

.legend-row {
  margin-top: 14px; display: flex; justify-content: center; gap: 16px;
  .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #64748b; padding: 6px 14px; background: white; border-radius: 8px; }
  .dot { width: 12px; height: 12px; border-radius: 3px; }
}

.detail-grid {
  .dg-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f1f5f9;
    &:last-child { border-bottom: none; }
    .dg-l { color: #94a3b8; font-size: 14px; }
    .dg-v { font-weight: 600; color: #334155; font-size: 14px; }
  }
}
</style>
