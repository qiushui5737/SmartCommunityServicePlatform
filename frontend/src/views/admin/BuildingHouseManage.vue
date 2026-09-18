<template>
  <div class="building-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><OfficeBuilding /></el-icon>
            </div>
            <div>
              <div class="header-title">楼栋房屋管理</div>
              <div class="header-desc">管理社区楼栋、单元及房屋信息</div>
            </div>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="manage-tabs">
        <el-tab-pane label="楼栋管理" name="buildings">
          <div class="toolbar">
            <el-input v-model="buildingKeyword" placeholder="搜索楼栋名称/编号" clearable @clear="loadBuildings" @keyup.enter="loadBuildings" style="width: 220px">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="loadBuildings"><el-icon><Search /></el-icon>查询</el-button>
            <el-button type="success" @click="openBuildingDialog()"><el-icon><Plus /></el-icon>新增楼栋</el-button>
          </div>
          <el-table :data="buildingList" v-loading="buildingLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="buildingNo" label="楼栋编号" width="120" />
            <el-table-column prop="name" label="楼栋名称" />
            <el-table-column prop="totalFloors" label="总层数" width="100" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{row}">
                <el-button link type="primary" @click="openBuildingDialog(row)" class="op-btn"><el-icon><Edit /></el-icon>编辑</el-button>
                <el-popconfirm title="删除楼栋将同时删除关联单元和房屋，确认删除?" @confirm="deleteBuilding(row.id)">
                  <template #reference><el-button link type="danger" class="op-btn"><el-icon><Delete /></el-icon>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt-4" v-model:current-page="buildingPage.current" v-model:page-size="buildingPage.size" :total="buildingPage.total" @current-change="loadBuildings" layout="total, prev, pager, next" />
        </el-tab-pane>

        <el-tab-pane label="单元管理" name="units">
          <div class="toolbar">
            <el-select v-model="buildingFilter" placeholder="选择楼栋" clearable style="width: 180px">
              <el-option v-for="b in buildingList" :key="b.id" :label="`${b.buildingNo}-${b.name}`" :value="b.id" />
            </el-select>
            <el-input v-model="unitKeyword" placeholder="搜索单元号" clearable @clear="loadUnits" @keyup.enter="loadUnits" style="width: 150px">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="loadUnits"><el-icon><Search /></el-icon>查询</el-button>
            <el-button type="success" @click="openUnitDialog()"><el-icon><Plus /></el-icon>新增单元</el-button>
          </div>
          <el-table :data="unitList" v-loading="unitLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="unitNo" label="单元号" />
            <el-table-column label="所属楼栋" width="150">
              <template #default="{row}">{{ getBuildingName(row.buildingId) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="{row}">
                <el-button link type="primary" @click="openUnitDialog(row)" class="op-btn"><el-icon><Edit /></el-icon>编辑</el-button>
                <el-popconfirm title="删除单元将同时删除关联房屋，确认删除?" @confirm="deleteUnit(row.id)">
                  <template #reference><el-button link type="danger" class="op-btn"><el-icon><Delete /></el-icon>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt-4" v-model:current-page="unitPage.current" v-model:page-size="unitPage.size" :total="unitPage.total" @current-change="loadUnits" layout="total, prev, pager, next" />
        </el-tab-pane>

        <el-tab-pane label="房屋管理" name="houses">
          <div class="toolbar">
            <el-input v-model="houseKeyword" placeholder="搜索房间号" clearable @clear="loadHouses" @keyup.enter="loadHouses" style="width: 160px">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="unitFilter" placeholder="选择单元" clearable style="width: 180px">
              <el-option v-for="u in unitList" :key="u.id" :label="`${getBuildingName(u.buildingId)}-${u.unitNo}`" :value="u.id" />
            </el-select>
            <el-select v-model="statusFilter" placeholder="房屋状态" clearable style="width: 120px">
              <el-option label="空置" value="VACANT" />
              <el-option label="已入住" value="OCCUPIED" />
            </el-select>
            <el-button type="primary" @click="loadHouses"><el-icon><Search /></el-icon>查询</el-button>
            <el-button type="success" @click="openHouseDialog()"><el-icon><Plus /></el-icon>新增房屋</el-button>
          </div>
          <el-table :data="houseList" v-loading="houseLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="roomNo" label="房间号" />
            <el-table-column prop="area" label="面积(㎡)" width="100" align="center">
              <template #default="{row}">{{ row.area?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{row}">
                <el-tag :type="statusMap[row.status]?.type" effect="light">{{ statusMap[row.status]?.text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="业主ID" width="90" align="center">
              <template #default="{row}">{{ row.ownerId || '—' }}</template>
            </el-table-column>
            <el-table-column label="所属单元" width="150">
              <template #default="{row}">{{ getUnitLabel(row.unitId) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="{row}">
                <el-button link type="primary" @click="openHouseDialog(row)" class="op-btn"><el-icon><Edit /></el-icon>编辑</el-button>
                <el-popconfirm title="确认删除该房屋?" @confirm="deleteHouse(row.id)">
                  <template #reference><el-button link type="danger" class="op-btn"><el-icon><Delete /></el-icon>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt-4" v-model:current-page="housePage.current" v-model:page-size="housePage.size" :total="housePage.total" @current-change="loadHouses" layout="total, prev, pager, next" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 楼栋弹窗 -->
    <el-dialog v-model="buildingDialogVisible" :title="buildingForm.id ? '编辑楼栋' : '新增楼栋'" width="500px" class="dialog">
      <el-form :model="buildingForm" label-width="80px">
        <el-form-item label="楼栋编号"><el-input v-model="buildingForm.buildingNo" /></el-form-item>
        <el-form-item label="楼栋名称"><el-input v-model="buildingForm.name" /></el-form-item>
        <el-form-item label="总层数"><el-input-number v-model="buildingForm.totalFloors" :min="1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="buildingDialogVisible=false">取消</el-button><el-button type="primary" @click="saveBuilding">保存</el-button></template>
    </el-dialog>

    <!-- 单元弹窗 -->
    <el-dialog v-model="unitDialogVisible" :title="unitForm.id ? '编辑单元' : '新增单元'" width="500px" class="dialog">
      <el-form :model="unitForm" label-width="80px">
        <el-form-item label="所属楼栋">
          <el-select v-model="unitForm.buildingId" style="width: 100%">
            <el-option v-for="b in buildingList" :key="b.id" :label="`${b.buildingNo}-${b.name}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单元号"><el-input v-model="unitForm.unitNo" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="unitDialogVisible=false">取消</el-button><el-button type="primary" @click="saveUnit">保存</el-button></template>
    </el-dialog>

    <!-- 房屋弹窗 -->
    <el-dialog v-model="houseDialogVisible" :title="houseForm.id ? '编辑房屋' : '新增房屋'" width="500px" class="dialog">
      <el-form :model="houseForm" label-width="80px">
        <el-form-item label="所属单元">
          <el-select v-model="houseForm.unitId" style="width: 100%">
            <el-option v-for="u in unitList" :key="u.id" :label="`${getBuildingName(u.buildingId)}-${u.unitNo}`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号"><el-input v-model="houseForm.roomNo" /></el-form-item>
        <el-form-item label="面积"><el-input-number v-model="houseForm.area" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="houseForm.status" style="width: 100%">
            <el-option label="空置" value="VACANT" />
            <el-option label="已入住" value="OCCUPIED" />
          </el-select>
        </el-form-item>

        <!-- 空置 → 办理入住 -->
        <template v-if="houseForm.status === 'VACANT' && houseForm.id">
          <el-divider />
          <el-form-item label="办理入住">
            <el-input v-model.number="checkinOwnerId" placeholder="输入业主ID" clearable>
              <template #prepend>业主ID</template>
            </el-input>
          </el-form-item>
          <div class="action-row">
            <el-button type="primary" :loading="checkinLoading" :disabled="!checkinOwnerId" @click="doHouseCheckin">
              <el-icon><Check /></el-icon>确认入住
            </el-button>
          </div>
        </template>

        <!-- 已入住 → 办理退房 -->
        <template v-if="houseForm.status === 'OCCUPIED' && houseForm.id">
          <el-divider />
          <div class="checkout-section">
            <div class="checkout-info">当前业主ID：<strong>{{ houseForm.ownerId || '—' }}</strong></div>
            <div class="action-row">
              <el-popconfirm title="确认办理退房？将清空业主绑定并设为空置" @confirm="doHouseCheckout">
                <template #reference>
                  <el-button type="danger" :loading="checkoutLoading">
                    <el-icon><CircleClose /></el-icon>办理退房
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="houseDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveHouse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, Search, Plus, Edit, Delete, Check, CircleClose } from '@element-plus/icons-vue'

const statusMap = { VACANT: { text: '空置', type: 'info' }, OCCUPIED: { text: '已入住', type: 'success' } }
const buildingKeyword = ref('')
const buildingList = ref([])
const buildingLoading = ref(false)
const buildingPage = reactive({ current: 1, size: 10, total: 0 })
const buildingDialogVisible = ref(false)
const buildingForm = ref({ id: null, buildingNo: '', name: '', totalFloors: 10 })

const loadBuildings = async () => {
  buildingLoading.value = true
  const res = await request.get('/buildings/page', { params: { ...buildingPage, keyword: buildingKeyword.value } })
  buildingList.value = res.data.records
  buildingPage.total = res.data.total
  buildingLoading.value = false
}
const openBuildingDialog = (row = null) => {
  buildingForm.value = row ? { ...row } : { id: null, buildingNo: '', name: '', totalFloors: 10 }
  buildingDialogVisible.value = true
}
const saveBuilding = async () => {
  if (buildingForm.value.id) await request.put(`/buildings/${buildingForm.value.id}`, buildingForm.value)
  else await request.post('/buildings', buildingForm.value)
  buildingDialogVisible.value = false; loadBuildings()
}
const deleteBuilding = async (id) => { await request.delete(`/buildings/${id}`); loadBuildings(); loadUnits(); loadHouses() }

const buildingFilter = ref(null)
const unitKeyword = ref('')
const unitList = ref([])
const unitLoading = ref(false)
const unitPage = reactive({ current: 1, size: 10, total: 0 })
const unitDialogVisible = ref(false)
const unitForm = ref({ id: null, buildingId: null, unitNo: '' })

const loadUnits = async () => {
  unitLoading.value = true
  const params = { ...unitPage, keyword: unitKeyword.value }
  if (buildingFilter.value) params.buildingId = buildingFilter.value
  const res = await request.get('/units/page', { params })
  unitList.value = res.data.records
  unitPage.total = res.data.total
  unitLoading.value = false
}
const openUnitDialog = (row = null) => {
  unitForm.value = row ? { ...row } : { id: null, buildingId: buildingFilter.value || null, unitNo: '' }
  unitDialogVisible.value = true
}
const saveUnit = async () => {
  if (unitForm.value.id) await request.put(`/units/${unitForm.value.id}`, unitForm.value)
  else await request.post('/units', unitForm.value)
  unitDialogVisible.value = false; loadUnits(); loadHouses()
}
const deleteUnit = async (id) => { await request.delete(`/units/${id}`); loadUnits(); loadHouses() }

const unitFilter = ref(null)
const statusFilter = ref(null)
const houseKeyword = ref('')
const houseList = ref([])
const houseLoading = ref(false)
const housePage = reactive({ current: 1, size: 10, total: 0 })
const houseDialogVisible = ref(false)
const houseForm = ref({ id: null, unitId: null, roomNo: '', area: 0, status: 'VACANT', ownerId: null })
const checkinOwnerId = ref(null)
const checkinLoading = ref(false)
const checkoutLoading = ref(false)

const loadHouses = async () => {
  houseLoading.value = true
  const params = { ...housePage }
  if (unitFilter.value) params.unitId = unitFilter.value
  if (statusFilter.value) params.status = statusFilter.value
  if (houseKeyword.value) params.keyword = houseKeyword.value
  const res = await request.get('/houses/page', { params })
  houseList.value = res.data.records
  housePage.total = res.data.total
  houseLoading.value = false
}
const openHouseDialog = (row = null) => {
  houseForm.value = row ? { ...row } : { id: null, unitId: unitFilter.value || null, roomNo: '', area: 0, status: 'VACANT', ownerId: null }
  checkinOwnerId.value = null
  houseDialogVisible.value = true
}
const saveHouse = async () => {
  if (houseForm.value.id) await request.put(`/houses/${houseForm.value.id}`, houseForm.value)
  else await request.post('/houses', houseForm.value)
  houseDialogVisible.value = false; loadHouses()
}
const deleteHouse = async (id) => { await request.delete(`/houses/${id}`); loadHouses() }

const doHouseCheckin = async () => {
  if (!checkinOwnerId.value) { ElMessage.warning('请输入业主ID'); return }
  checkinLoading.value = true
  try {
    await request.post('/houses/checkin', null, { params: { houseId: houseForm.value.id, ownerId: checkinOwnerId.value } })
    ElMessage.success('入住办理成功')
    houseDialogVisible.value = false
    loadHouses()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '办理失败')
  } finally { checkinLoading.value = false }
}

const doHouseCheckout = async () => {
  checkoutLoading.value = true
  try {
    await request.post('/houses/checkout', null, { params: { houseId: houseForm.value.id } })
    ElMessage.success('退房办理成功')
    houseDialogVisible.value = false
    loadHouses()
  } catch (e) {
    ElMessage.error('退房失败')
  } finally { checkoutLoading.value = false }
}

const getBuildingName = (id) => { const b = buildingList.value.find(b => b.id === id); return b ? `${b.buildingNo}-${b.name}` : '-' }
const getUnitLabel = (id) => { const u = unitList.value.find(u => u.id === id); return u ? `${getBuildingName(u.buildingId)}-${u.unitNo}` : '-' }

const activeTab = ref('buildings')
onMounted(() => { loadBuildings(); loadUnits(); loadHouses() })
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.building-manage {
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
        background: linear-gradient(135deg, #f59e0b, #d97706);
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
  .form-hint { font-size: 12px; color: $text-secondary; margin-top: 4px; line-height: 1.4; }
  .action-row { display: flex; justify-content: flex-end; margin-bottom: 8px; }
  .checkout-section {
    .checkout-info { font-size: 14px; color: $text-secondary; margin-bottom: 12px;
      strong { color: $text-primary; font-weight: 700; }
    }
  }
}
</style>
