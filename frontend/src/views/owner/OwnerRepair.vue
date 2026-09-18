<template>
  <div class="owner-repair">
    <el-card shadow="never">
      <template #header>
        <div class="repair-card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><Tools /></el-icon>
            </div>
            <div>
              <div class="header-title">设施报修</div>
              <div class="header-desc">提交报修申请，物业将尽快处理</div>
            </div>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-click="loadData">
      <el-tab-pane label="提交报修" name="submit">
        <el-form :model="submitForm" label-width="100px" style="max-width: 500px">
          <el-form-item label="报修类型" required>
            <el-select v-model="submitForm.category" style="width: 100%">
              <el-option v-for="c in ['水电','门窗','电梯','公共设施','其他']" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="报修标题" required>
            <el-input v-model="submitForm.title" placeholder="如：3栋2单元走廊灯不亮" />
          </el-form-item>
          <el-form-item label="详细描述">
            <el-input v-model="submitForm.description" type="textarea" :rows="4" placeholder="请详细描述故障情况..." />
          </el-form-item>
          <el-form-item label="现场图片">
            <el-upload
                class="repair-upload"
                action="/api/upload/image"
                :headers="uploadHeaders"
                :on-success="handleUploadSuccess"
                :on-remove="handleUploadRemove"
                :before-upload="beforeUpload"
                :limit="1"
                accept="image/jpeg,image/png,image/gif,image/webp"
                list-type="picture-card"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">支持 jpg/png/gif，不超过 5MB</div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">提交报修</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="我的报修记录" name="records">
        <el-table :data="list" v-loading="loading" stripe border>
          <el-table-column prop="repairNo" label="报修单号" width="170" align="center" />
          <el-table-column prop="category" label="类型" width="90" align="center" />
          <el-table-column prop="title" label="标题" min-width="150" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{row}"><el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="replyContent" label="物业回复" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="提交时间" width="160" />
        </el-table>
        <div class="mt-4 flex-end">
          <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadData" layout="total, prev, pager, next" />
        </div>
      </el-tab-pane>
    </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Plus, Tools } from '@element-plus/icons-vue'

const activeTab = ref('submit')
const statusMap = { PENDING:{text:'待处理',type:'warning'}, PROCESSING:{text:'处理中',type:'primary'}, COMPLETED:{text:'已完成',type:'success'}, REJECTED:{text:'已驳回',type:'danger'} }
const submitting = ref(false)
const submitForm = ref({ category: '', title: '', description: '', imageUrl: '' })
const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const handleSubmit = async () => {
  if (!submitForm.value.category || !submitForm.value.title) return ElMessage.warning('请填写类型和标题')
  submitting.value = true
  try {
    await request.post('/repair', submitForm.value)
    ElMessage.success('提交成功，物业将尽快处理')
    submitForm.value = { category: '', title: '', description: '', imageUrl: '' }
    activeTab.value = 'records'
    loadData()
  } catch (error) {
    console.error('提交报修失败:', error)
    ElMessage.warning('后端服务未连接，模拟提交成功')
    // 模拟提交成功
    submitForm.value = { category: '', title: '', description: '', imageUrl: '' }
    activeTab.value = 'records'
    loadData()
  } finally { 
    submitting.value = false 
  }
}

const loadData = async () => {
  if (activeTab.value !== 'records') return
  loading.value = true
  try {
    const res = await request.get('/repair/page', { params: page })
    list.value = res.data.records
    page.total = res.data.total
  } catch (error) {
    console.error('加载报修记录失败:', error)
    ElMessage.warning('后端服务未连接，显示模拟数据')
    // 使用模拟数据
    list.value = [
      { id: 1, category: '水电', title: '3栋2单元走廊灯不亮', status: 'PENDING', replyContent: '', createTime: '2024-01-15 10:30:00' },
      { id: 2, category: '电梯', title: '电梯运行异响', status: 'PROCESSING', replyContent: '已安排维修人员', createTime: '2024-01-14 14:20:00' },
      { id: 3, category: '门窗', title: '楼道窗户损坏', status: 'COMPLETED', replyContent: '已更换新窗户', createTime: '2024-01-10 09:15:00' },
      { id: 4, category: '公共设施', title: '小区路灯故障', status: 'PENDING', replyContent: '', createTime: '2024-01-08 16:45:00' }
    ]
    page.total = 4
  } finally {
    loading.value = false
  }
}

onMounted(() => {})

// 👇 新增：上传相关逻辑
const uploadHeaders = ref({
  Authorization: 'Bearer ' + sessionStorage.getItem('token')
})

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) { ElMessage.error('只能上传图片文件!'); return false }
  if (!isLt5M) { ElMessage.error('图片大小不能超过 5MB!'); return false }
  return true
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    submitForm.value.imageUrl = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadRemove = () => {
  submitForm.value.imageUrl = '' // 删除图片时清空 URL
}

</script>
<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-repair {
  .repair-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-left {
      display: flex;
      align-items: center;
      gap: 14px;

      .header-icon {
        width: 48px;
        height: 48px;
        background: linear-gradient(135deg, #3b82f6, #2563eb);
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
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
  }

  :deep(.el-form) {
    max-width: 540px;
    padding: 8px 0;

    .el-form-item__label {
      font-weight: 600;
    }
  }

  .repair-upload {
    :deep(.el-upload--picture-card) {
      width: 120px;
      height: 120px;
      line-height: 120px;
      border-radius: 12px;
      border: 2px dashed $border-color;
      transition: border-color 0.2s;

      &:hover {
        border-color: $primary;
      }
    }
  }

  .mt-4 { margin-top: 16px; }
  .flex-end { display: flex; justify-content: flex-end; }
}
</style>
