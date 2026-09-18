<template>
  <div class="announcement-manage">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="query.keyword" placeholder="搜索公告标题" clearable style="width: 220px" @clear="loadData" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.type" placeholder="公告类型" clearable style="width: 140px" @change="loadData">
            <el-option label="通知" value="NOTICE" />
            <el-option label="活动" value="ACTIVITY" />
            <el-option label="维护" value="MAINTENANCE" />
          </el-select>
          <el-select v-model="query.status" placeholder="公告状态" clearable style="width: 140px" @change="loadData">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已撤回" value="WITHDRAWN" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon> 查询
          </el-button>
        </div>
        <div class="filter-right">
          <el-button type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon> 发布公告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 公告表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" stripe border style="width: 100%">
        <el-table-column label="置顶" width="60" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.pinned" color="#f59e0b" :size="18"><Top /></el-icon>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.tagType" effect="light">
              {{ typeMap[row.type]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tagType" effect="dark" size="small">
              {{ statusMap[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="110" align="center">
          <template #default="{ row }">
            {{ row.publisherName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="handleWithdraw(row)">撤回</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="800px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="公告类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                <el-option label="通知" value="NOTICE" />
                <el-option label="活动" value="ACTIVITY" />
                <el-option label="维护" value="MAINTENANCE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否置顶">
              <el-switch v-model="form.pinned" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公告内容" prop="content">
          <div class="editor-wrapper">
            <Toolbar
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              style="border-bottom: 1px solid #e2e8f0"
            />
            <Editor
              v-model="form.content"
              :defaultConfig="editorConfig"
              style="height: 300px; overflow-y: hidden"
              @onCreated="handleEditorCreated"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">
          {{ form.id ? '保存修改' : '发布公告' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, shallowRef, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import {
  getAnnouncementPage,
  getAnnouncementDetail,
  createAnnouncement,
  updateAnnouncement,
  withdrawAnnouncement,
  deleteAnnouncement
} from '@/api/announcement'

// ====== 映射表 ======
const typeMap = {
  NOTICE: { label: '通知', tagType: 'primary' },
  ACTIVITY: { label: '活动', tagType: 'success' },
  MAINTENANCE: { label: '维护', tagType: 'warning' }
}

const statusMap = {
  PUBLISHED: { label: '已发布', tagType: 'success' },
  WITHDRAWN: { label: '已撤回', tagType: 'info' },
  DRAFT: { label: '草稿', tagType: 'warning' }
}

// ====== 列表相关 ======
const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const query = reactive({ keyword: '', type: '', status: '' })

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: page.current,
      size: page.size,
      type: query.type || undefined,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    }
    const res = await getAnnouncementPage(params)
    list.value = res.data.records
    page.total = res.data.total
  } finally {
    loading.value = false
  }
}

// ====== 表单弹窗 ======
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = ref({ id: null, title: '', type: '', pinned: false, content: '' })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

// ====== WangEditor ======
const editorRef = shallowRef(null)
const toolbarConfig = {
  excludeKeys: ['fullScreen', 'group-video']
}
const editorConfig = {
  placeholder: '请输入公告内容...'
}

const handleEditorCreated = (editor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})

// ====== 操作 ======
const openForm = async (row) => {
  if (row) {
    // 编辑 - 加载详情
    const res = await getAnnouncementDetail(row.id)
    const data = res.data
    form.value = {
      id: data.id,
      title: data.title,
      type: data.type,
      pinned: data.pinned,
      content: data.content || ''
    }
  } else {
    form.value = { id: null, title: '', type: '', pinned: false, content: '' }
  }
  dialogVisible.value = true
}

const resetForm = () => {
  form.value = { id: null, title: '', type: '', pinned: false, content: '' }
  formRef.value?.resetFields()
}

const submitForm = async () => {
  await formRef.value?.validate()
  if (!form.value.content || form.value.content === '<p><br></p>') {
    ElMessage.warning('请输入公告内容')
    return
  }
  submitLoading.value = true
  try {
    if (form.value.id) {
      await updateAnnouncement(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createAnnouncement(form.value)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleWithdraw = (row) => {
  ElMessageBox.confirm(`确认撤回公告「${row.title}」？撤回后业主将无法查看。`, '撤回确认', {
    confirmButtonText: '确认撤回',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await withdrawAnnouncement(row.id)
    ElMessage.success('撤回成功')
    loadData()
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除公告「${row.title}」？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.announcement-manage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  :deep(.el-card__body) {
    padding: 0;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
}

.text-muted {
  color: #c0c4cc;
}

.editor-wrapper {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
}
</style>
