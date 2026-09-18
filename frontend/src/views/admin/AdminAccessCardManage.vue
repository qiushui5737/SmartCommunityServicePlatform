<template>
  <div class="admin-access-card">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon"><el-icon :size="20"><Key /></el-icon></div>
            <div>
              <div class="header-title">门禁卡管理</div>
              <div class="header-desc">发行、授权、挂失、注销全生命周期管理</div>
            </div>
          </div>
          <el-button type="primary" @click="openIssue"><el-icon><Plus /></el-icon>发行新卡</el-button>
        </div>
      </template>

      <!-- Stats -->
      <div class="stat-row">
        <div v-for="s in stats" :key="s.label" class="stat-box" :class="s.cls">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-lbl">{{ s.label }}</div>
        </div>
      </div>

      <!-- Filters -->
      <div class="filter-row">
        <el-select v-model="filterType" placeholder="卡片类型" clearable style="width:130px" @change="loadData">
          <el-option label="业主卡" value="OWNER" /><el-option label="家庭卡" value="FAMILY" />
          <el-option label="访客卡" value="VISITOR" /><el-option label="临时卡" value="TEMPORARY" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px" @change="loadData">
          <el-option label="正常" value="ACTIVE" /><el-option label="已挂失" value="SUSPENDED" /><el-option label="已注销" value="CANCELLED" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索卡号/姓名" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="loadData">查询</el-button>
      </div>

      <!-- Table -->
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="cardNo" label="卡号" width="160">
          <template #default="{ row }"><span class="mono">{{ row.cardNo }}</span></template>
        </el-table-column>
        <el-table-column label="持卡人" width="100">
          <template #default="{ row }">{{ row.ownerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }"><el-tag :type="cardTypeMap[row.cardType]?.tag" size="small" effect="plain">{{ cardTypeMap[row.cardType]?.text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type" size="small" effect="dark">{{ statusMap[row.status]?.text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="通行楼栋" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.buildingIds || '全部楼栋' }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="180">
          <template #default="{ row }">{{ formatValidity(row) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发行时间" width="160" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status==='ACTIVE'" link type="warning" @click="handleSuspend(row)">挂失</el-button>
            <el-button v-if="row.status==='SUSPENDED'" link type="success" @click="handleResume(row)">恢复</el-button>
            <el-button v-if="row.status!=='CANCELLED'" link type="danger" @click="handleCancel(row)">注销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="page.total > page.size">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadData" layout="total, prev, pager, next" />
      </div>
    </el-card>

    <!-- ============ 发行 Dialog ============ -->
    <el-dialog v-model="showIssue" title="发行门禁卡" width="520px" :close-on-click-modal="false">
      <el-form :model="issueForm" label-width="90px">
        <el-form-item label="卡片编号"><el-input v-model="issueForm.cardNo" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="卡片类型" required>
          <el-select v-model="issueForm.cardType" style="width:100%">
            <el-option label="业主卡" value="OWNER" /><el-option label="家庭卡" value="FAMILY" />
            <el-option label="访客卡" value="VISITOR" /><el-option label="临时卡" value="TEMPORARY" />
          </el-select>
        </el-form-item>
        <el-form-item label="持卡人ID" required><el-input v-model.number="issueForm.ownerId" placeholder="业主用户ID" /></el-form-item>
        <el-form-item label="通行楼栋"><el-input v-model="issueForm.buildingIds" placeholder="楼栋ID逗号分隔，留空=全部" /></el-form-item>
        <el-form-item label="有效期起"><el-date-picker v-model="issueForm.validFrom" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="有效期止"><el-date-picker v-model="issueForm.validTo" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="留空=永久" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="issueForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIssue = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitIssue">发行</el-button>
      </template>
    </el-dialog>

    <!-- ============ 编辑权限 Dialog ============ -->
    <el-dialog v-model="showEdit" title="编辑卡片权限" width="520px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="卡号"><span class="mono">{{ editForm.cardNo }}</span></el-form-item>
        <el-form-item label="卡片类型">
          <el-select v-model="editForm.cardType" style="width:100%">
            <el-option label="业主卡" value="OWNER" /><el-option label="家庭卡" value="FAMILY" />
            <el-option label="访客卡" value="VISITOR" /><el-option label="临时卡" value="TEMPORARY" />
          </el-select>
        </el-form-item>
        <el-form-item label="通行楼栋"><el-input v-model="editForm.buildingIds" placeholder="楼栋ID逗号分隔，留空=全部" /></el-form-item>
        <el-form-item label="有效期起"><el-date-picker v-model="editForm.validFrom" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="有效期止"><el-date-picker v-model="editForm.validTo" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="留空=永久" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Plus, Search } from '@element-plus/icons-vue'

const cardTypeMap = { OWNER:{text:'业主卡',tag:'primary'}, FAMILY:{text:'家庭卡',tag:'success'}, VISITOR:{text:'访客卡',tag:'warning'}, TEMPORARY:{text:'临时卡',tag:'info'} }
const statusMap = { ACTIVE:{text:'正常',type:'success'}, SUSPENDED:{text:'已挂失',type:'danger'}, CANCELLED:{text:'已注销',type:'info'} }

const list = ref([]); const loading = ref(false)
const page = reactive({ current:1, size:10, total:0 })
const filterType = ref(''); const filterStatus = ref(''); const keyword = ref('')
const stats = reactive([
  { label:'总发卡', value:0, cls:'s-total' },
  { label:'正常', value:0, cls:'s-active' },
  { label:'已挂失', value:0, cls:'s-suspended' },
  { label:'已注销', value:0, cls:'s-cancelled' }
])

// Issue
const showIssue = ref(false); const submitting = ref(false)
const issueForm = ref({ cardNo:'', cardType:'OWNER', ownerId:null, buildingIds:'', validFrom:'', validTo:'', remark:'' })

// Edit
const showEdit = ref(false)
const editForm = ref({ id:null, cardNo:'', cardType:'', buildingIds:'', validFrom:'', validTo:'', remark:'' })

const formatValidity = (card) => {
  if (!card.validFrom && !card.validTo) return '永久有效'
  return (card.validFrom||'—') + ' 至 ' + (card.validTo||'永久')
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current:page.current, size:page.size }
    if (filterType.value) params.cardType = filterType.value
    if (filterStatus.value) params.status = filterStatus.value
    if (keyword.value) params.keyword = keyword.value
    const res = await request.get('/access-card/page', { params })
    list.value = res.data.records || []
    page.total = res.data.total || 0
    loadStats()
  } catch(e) { console.error(e) }
  finally { loading.value = false }
}

const loadStats = async () => {
  try {
    const res = await request.get('/access-card/stats')
    const d = res.data
    stats[0].value = d.total||0; stats[1].value = d.active||0; stats[2].value = d.suspended||0; stats[3].value = d.cancelled||0
  } catch(e) {}
}

const openIssue = () => { issueForm.value = { cardNo:'', cardType:'OWNER', ownerId:null, buildingIds:'', validFrom:'', validTo:'', remark:'' }; showIssue.value = true }
const openEdit = (row) => {
  editForm.value = { id:row.id, cardNo:row.cardNo, cardType:row.cardType, buildingIds:row.buildingIds||'', validFrom:row.validFrom, validTo:row.validTo, remark:row.remark||'' }
  showEdit.value = true
}

const handleSubmitIssue = async () => {
  if (!issueForm.value.ownerId) return ElMessage.warning('请填写持卡人ID')
  submitting.value = true
  try {
    await request.post('/access-card', issueForm.value)
    ElMessage.success('卡片发行成功')
    showIssue.value = false; loadData()
  } catch(e) { ElMessage.error('发行失败') }
  finally { submitting.value = false }
}

const handleSubmitEdit = async () => {
  submitting.value = true
  try {
    await request.put(`/access-card/${editForm.value.id}`, editForm.value)
    ElMessage.success('修改成功')
    showEdit.value = false; loadData()
  } catch(e) { ElMessage.error('修改失败') }
  finally { submitting.value = false }
}

const handleSuspend = async (row) => {
  try { await ElMessageBox.confirm('确定挂失该卡片？','提示',{type:'warning'}); await request.put(`/access-card/${row.id}/suspend`); ElMessage.success('已挂失'); loadData() } catch(e) {}
}
const handleResume = async (row) => {
  try { await ElMessageBox.confirm('确定恢复该卡片？','提示',{type:'info'}); await request.put(`/access-card/${row.id}/resume`); ElMessage.success('已恢复'); loadData() } catch(e) {}
}
const handleCancel = async (row) => {
  try { await ElMessageBox.confirm('确定注销该卡片？注销后不可恢复！','警告',{type:'warning'}); await request.put(`/access-card/${row.id}/cancel`); ElMessage.success('已注销'); loadData() } catch(e) {}
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.admin-access-card {
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    .header-left { display: flex; align-items: center; gap: 14px;
      .header-icon { width:48px;height:48px;background:linear-gradient(135deg,#3b82f6,#2563eb);border-radius:14px;display:flex;align-items:center;justify-content:center;color:white;box-shadow:0 4px 12px rgba(37,99,235,0.25); }
      .header-title { font-size:18px;font-weight:700;color:$text-primary; }
      .header-desc { font-size:13px;color:$text-secondary;margin-top:2px; }
    }
  }
}
.stat-row { display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:18px;
  .stat-box { padding:16px;border-radius:12px;text-align:center;background:$gray-50;border:1px solid $border-color-light;
    .stat-num { font-size:26px;font-weight:700;letter-spacing:-0.02em; }
    .stat-lbl { font-size:12px;color:$text-secondary;margin-top:2px; }
    &.s-total .stat-num { color:$text-primary; }
    &.s-active .stat-num { color:#10b981; }
    &.s-suspended .stat-num { color:#f59e0b; }
    &.s-cancelled .stat-num { color:$text-tertiary; }
  }
}
.filter-row { display:flex;gap:8px;margin-bottom:16px;align-items:center; }
.mono { font-family:$font-mono;font-size:13px;letter-spacing:0.03em; }
.pagination-wrap { display:flex;justify-content:flex-end;margin-top:16px; }
</style>
