<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex-between">
        <span class="title">🔧 设备报修管理</span>
        <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width: 150px" @change="loadData">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="repairNo" label="报修单号" width="170" align="center" />
      <el-table-column label="业主" width="120">
        <template #default="{row}">
          {{ row.ownerName || '未知' }}
        </template>
      </el-table-column>

      <el-table-column prop="category" label="类型" width="90" align="center" />
      <el-table-column prop="title" label="报修标题" min-width="150" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{row}">
          <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="现场照片" width="120" align="center">
        <template #default="{row}">
          <el-image
              v-if="row.imageUrl"
              style="width: 60px; height: 60px; border-radius: 4px"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              preview-teleported
          />
          <span v-else class="no-img">无照片</span>
        </template>
      </el-table-column>
      <el-table-column prop="replyContent" label="处理反馈" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="100" align="center">
        <template #default="{row}">
          <el-button link type="primary" @click="openHandle(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="mt-4 flex-end">
      <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadData" layout="total, prev, pager, next" />
    </div>
  </el-card>

  <!-- 处理弹窗 -->
  <el-dialog v-model="dialogVisible" title="处理工单" width="500px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="当前状态">
        <el-tag :type="statusMap[form.status]?.type">{{ statusMap[form.status]?.text }}</el-tag>
      </el-form-item>
      <el-form-item label="更新状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理反馈">
        <el-input v-model="form.replyContent" type="textarea" :rows="3" placeholder="填写维修结果或驳回原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="submitHandle">确认处理</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const statusMap = { PENDING:{text:'待处理',type:'warning'}, PROCESSING:{text:'处理中',type:'primary'}, COMPLETED:{text:'已完成',type:'success'}, REJECTED:{text:'已驳回',type:'danger'} }
const filterStatus = ref('')
const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const form = ref({ id: null, status: '', replyContent: '' })
const ownerNames = ref({})

const loadData = async () => {
  loading.value = true
  const res = await request.get('/repair/page', { params: { ...page, status: filterStatus.value } })
  list.value = res.data.records
  page.total = res.data.total
  // 缓存业主名（实际可联表查询，此处简化展示）
  list.value.forEach(r => { if(!ownerNames.value[r.ownerId]) ownerNames.value[r.ownerId] = `业主${r.ownerId}` })
  loading.value = false
}

const openHandle = (row) => {
  form.value = { id: row.id, status: row.status, replyContent: row.replyContent || '' }
  dialogVisible.value = true
}

const submitHandle = async () => {
  await request.put(`/repair/${form.value.id}/handle`, form.value)
  ElMessage.success('处理成功')
  dialogVisible.value = false
  loadData()
}

onMounted(loadData)
</script>
<style scoped>
.flex-between { display:flex; justify-content:space-between; align-items:center }
.title { font-weight:bold; font-size:16px }
.mt-4 { margin-top:16px }
.flex-end { display:flex; justify-content:flex-end }
.no-img { color: #909399; font-size: 12px; }
</style>
