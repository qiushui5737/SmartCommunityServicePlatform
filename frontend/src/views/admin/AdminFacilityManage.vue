<template>
  <div class="admin-facility">
    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <!-- ====== Tab 1：设施管理 ====== -->
      <el-tab-pane label="设施管理" name="facilities">
        <div class="toolbar">
          <el-input v-model="fKeyword" placeholder="搜索设施名称..." clearable style="width:220px" @clear="loadFacilities" @keyup.enter="loadFacilities" />
          <el-button type="primary" @click="openAdd">+ 新增设施</el-button>
        </div>

        <el-table :data="facilities" v-loading="fLoading" stripe border>
          <el-table-column label="图片" width="80" align="center">
            <template #default="{row}">
              <el-image :src="row.imageUrl" fit="cover" style="width:50px;height:50px;border-radius:8px" :preview-src-list="[row.imageUrl]" preview-teleported @error="handleImgError($event, row.name, row.category)" />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column prop="category" label="分类" width="100" align="center" />
          <el-table-column prop="location" label="存放位置" width="120" />
          <el-table-column prop="deposit" label="押金" width="80" align="center">
            <template #default="{row}">{{ row.deposit > 0 ? '¥' + row.deposit : '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{row}"><el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="130" align="center">
            <template #default="{row}">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)" v-if="row.status !== 'RETIRED'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="fPage.current" v-model:page-size="fPage.size" :total="fPage.total" @current-change="loadFacilities" layout="total, prev, pager, next" />
        </div>
      </el-tab-pane>

      <!-- ====== Tab 2：借用审批 ====== -->
      <el-tab-pane label="借用审批" name="bookings">
        <div class="toolbar">
          <el-input v-model="bKeyword" placeholder="搜索设施名称..." clearable style="width:220px" @clear="loadBookings" @keyup.enter="loadBookings" />
          <el-select v-model="bStatus" placeholder="按状态筛选" clearable style="width:150px" @change="loadBookings">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已归还" value="RETURNED" />
          </el-select>
        </div>

        <el-table :data="bookings" v-loading="bLoading" stripe border>
          <el-table-column prop="id" label="单号" width="70" align="center" />
          <el-table-column label="设施" min-width="130">
            <template #default="{row}">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image :src="row.facilityImage" fit="cover" style="width:36px;height:36px;border-radius:6px;flex-shrink:0" @error="handleImgError($event, row.facilityName, row.facilityCategory)" />
                <span>{{ row.facilityName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="申请人" width="100">
            <template #default="{row}">{{ row.ownerName || '未知' }}</template>
          </el-table-column>
          <el-table-column prop="ownerPhone" label="联系电话" width="120" />
          <el-table-column prop="purpose" label="借用事由" min-width="150" show-overflow-tooltip />
          <el-table-column prop="durationHours" label="时长" width="70" align="center">
            <template #default="{row}">{{ row.durationHours }}h</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{row}"><el-tag :type="bookingStatusMap[row.status]?.type">{{ bookingStatusMap[row.status]?.text }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="replyContent" label="审批备注" min-width="140" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="160" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{row}">
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openHandle(row)">审批</el-button>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="bPage.current" v-model:page-size="bPage.size" :total="bPage.total" @current-change="loadBookings" layout="total, prev, pager, next" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ====== 新增/编辑设施弹窗 ====== -->
    <el-dialog v-model="facilityDialogVisible" :title="isEdit ? '编辑设施' : '新增设施'" width="560px" destroy-on-close>
      <el-form :model="facilityForm" label-width="90px">
        <el-form-item label="设施名称" required>
          <el-input v-model="facilityForm.name" placeholder="如：电钻套装" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="facilityForm.category" style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="facilityForm.description" type="textarea" :rows="3" placeholder="设施详细描述..." />
        </el-form-item>
        <el-form-item label="存放位置">
          <el-input v-model="facilityForm.location" placeholder="如：物业前台" />
        </el-form-item>
        <el-form-item label="借用押金">
          <el-input-number v-model="facilityForm.deposit" :min="0" :step="10" style="width:100%" />
        </el-form-item>
        <el-form-item label="设施图片">
          <el-upload
            action="/api/upload/facility"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
            :limit="1"
            accept="image/jpeg,image/png,image/gif,image/webp"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-select v-model="facilityForm.status" style="width:100%">
            <el-option label="可借用" value="AVAILABLE" />
            <el-option label="已借出" value="BOOKED" />
            <el-option label="维护中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="facilityDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFacility">{{ isEdit ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>

    <!-- ====== 审批弹窗 ====== -->
    <el-dialog v-model="handleDialogVisible" title="审批借用申请" width="500px" destroy-on-close>
      <div class="handle-info">
        <el-image :src="handleTarget?.facilityImage" fit="cover" style="width:60px;height:60px;border-radius:10px" @error="handleImgError($event, handleTarget?.facilityName, handleTarget?.facilityCategory)" />
        <div>
          <h4>{{ handleTarget?.facilityName }}</h4>
          <p class="text-muted">申请人：{{ handleTarget?.ownerName }}（{{ handleTarget?.ownerPhone }}）</p>
          <p class="text-muted">借用时长：{{ handleTarget?.durationHours }}小时</p>
          <p>事由：{{ handleTarget?.purpose }}</p>
        </div>
      </div>
      <el-form :model="handleForm" label-width="80px" style="margin-top:16px">
        <el-form-item label="审批结果" required>
          <el-radio-group v-model="handleForm.status">
            <el-radio value="APPROVED">✅ 通过</el-radio>
            <el-radio value="REJECTED">❌ 驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input v-model="handleForm.replyContent" type="textarea" :rows="2" :placeholder="handleForm.status === 'REJECTED' ? '请填写驳回原因...' : '可选填写备注...'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const catColors = { '运动器材': '#3b82f6', '工具设备': '#f97316', '文娱用品': '#8b5cf6', '清洁工具': '#10b981', '其他': '#64748b' }
const handleImgError = (e, name, category) => {
  const bg = catColors[category] || '#64748b'
  const ch = (name || '?')[0]
  e.target.src = `data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect fill="${bg}" width="400" height="300"/><text x="200" y="175" text-anchor="middle" fill="white" font-size="72" font-weight="bold" font-family="system-ui,sans-serif">${ch}</text></svg>`)}`
}
const categories = ['运动器材', '工具设备', '文娱用品', '清洁工具', '其他']

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

const activeTab = ref('facilities')

// ========== 设施管理 ==========
const facilities = ref([])
const fLoading = ref(false)
const fKeyword = ref('')
const fPage = reactive({ current: 1, size: 10, total: 0 })
const facilityDialogVisible = ref(false)
const isEdit = ref(false)
const facilityForm = ref({})

const uploadHeaders = ref({ Authorization: 'Bearer ' + sessionStorage.getItem('token') })

const loadFacilities = async () => {
  fLoading.value = true
  try {
    const res = await request.get('/facility/page', { params: { current: fPage.current, size: fPage.size, keyword: fKeyword.value || undefined } })
    facilities.value = res.data?.records || []
    fPage.total = res.data?.total || 0
  } catch (e) { console.error(e) } finally { fLoading.value = false }
}

const openAdd = () => {
  isEdit.value = false
  facilityForm.value = { name: '', category: '', description: '', location: '', deposit: 0, imageUrl: '' }
  facilityDialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  facilityForm.value = { ...row }
  facilityDialogVisible.value = true
}

const submitFacility = async () => {
  if (!facilityForm.value.name || !facilityForm.value.category) return ElMessage.warning('请填写名称和分类')
  if (isEdit.value) {
    await request.put(`/facility/${facilityForm.value.id}`, facilityForm.value)
    ElMessage.success('修改成功')
  } else {
    await request.post('/facility', facilityForm.value)
    ElMessage.success('新增成功')
  }
  facilityDialogVisible.value = false
  loadFacilities()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除设施「${row.name}」？`, '删除确认', { type: 'warning' })
    await request.delete(`/facility/${row.id}`)
    ElMessage.success('删除成功')
    loadFacilities()
  } catch (e) { if (e !== 'cancel') console.error(e) }
}

const beforeUpload = (file) => {
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('图片不超过5MB'); return false }
  return true
}
const handleUploadSuccess = (res) => {
  if (res.code === 200) { facilityForm.value.imageUrl = res.data.url; ElMessage.success('图片上传成功') }
  else ElMessage.error(res.msg || '上传失败')
}

// ========== 借用审批 ==========
const bookings = ref([])
const bLoading = ref(false)
const bKeyword = ref('')
const bStatus = ref('')
const bPage = reactive({ current: 1, size: 10, total: 0 })
const handleDialogVisible = ref(false)
const handleTarget = ref(null)
const handleForm = ref({ status: 'APPROVED', replyContent: '' })

const loadBookings = async () => {
  bLoading.value = true
  try {
    const res = await request.get('/facility-booking/page', {
      params: { current: bPage.current, size: bPage.size, status: bStatus.value || undefined, keyword: bKeyword.value || undefined }
    })
    bookings.value = res.data?.records || []
    bPage.total = res.data?.total || 0
  } catch (e) { console.error(e) } finally { bLoading.value = false }
}

const openHandle = (row) => {
  handleTarget.value = row
  handleForm.value = { status: 'APPROVED', replyContent: '' }
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  await request.put(`/facility-booking/${handleTarget.value.id}/handle`, handleForm.value)
  ElMessage.success('审批完成')
  handleDialogVisible.value = false
  loadBookings()
}

// ========== Tab 切换 ==========
const onTabChange = () => {
  if (activeTab.value === 'facilities') loadFacilities()
  else loadBookings()
}

onMounted(() => {
  loadFacilities()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.admin-facility {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    gap: 12px;
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .text-muted {
    color: $text-tertiary;
    font-size: 12px;
  }

  .handle-info {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: $bg-tertiary;
    border-radius: 12px;
    align-items: center;

    h4 { margin: 0 0 4px; color: $text-primary; font-size: 15px; }
    p { margin: 2px 0; font-size: 13px; color: $text-secondary; }
  }
}
</style>
