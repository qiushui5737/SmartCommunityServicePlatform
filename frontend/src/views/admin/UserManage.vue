<template>
  <div class="user-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><UserFilled /></el-icon>
            </div>
            <div>
              <div class="header-title">用户管理</div>
              <div class="header-desc">管理系统内所有用户信息</div>
            </div>
          </div>
          <div class="header-right">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名/姓名"
              clearable
              @clear="loadData"
              @keyup.enter="loadData"
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="loadData" class="btn">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button type="success" @click="openDialog()" class="btn">
              <el-icon><Plus /></el-icon>新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe row-key="id" class="data-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="phone" label="联系电话" min-width="130" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'STAFF' ? 'warning' : 'success'" effect="light">
              {{ roleMap[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="buildingName" label="所属楼栋" min-width="120" align="center">
          <template #default="{row}">{{ row.buildingName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="roomNo" label="房间号" width="100" align="center">
          <template #default="{row}">{{ row.roomNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="toggleStatus(row)"
              :disabled="row.id === 1"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" @click="openDialog(row)" class="op-btn">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-popconfirm title="确定删除该用户?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" :disabled="row.id === 1" class="op-btn">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="550px" destroy-on-close class="user-dialog">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="留空默认 123456" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="工作人员" value="STAFF" />
            <el-option label="业主" value="OWNER" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定房屋" v-if="form.role === 'OWNER'">
          <el-cascader
            v-model="cascaderValue"
            :options="houseTree"
            :props="{ value: 'id', label: 'name', children: 'children', checkStrictly: false, emitPath: false }"
            placeholder="选择 楼栋 / 单元 / 房间"
            clearable
            style="width: 100%"
            @change="onHouseChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { UserFilled, Search, Plus, Edit, Delete } from '@element-plus/icons-vue'

const roleMap = { ADMIN: '管理员', STAFF: '员工', OWNER: '业主' }
const keyword = ref('')
const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const formRef = ref(null)
const houseTree = ref([])
const cascaderValue = ref(null)

const form = reactive({
  id: null, username: '', realName: '', phone: '', role: 'OWNER', password: '', houseId: null
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/users/page', {
      params: { ...page, keyword: keyword.value }
    })
    tableData.value = res.data.records
    page.total = res.data.total
  } finally {
    loading.value = false
  }
}

const openDialog = async (row = null) => {
  if (row) {
    Object.assign(form, row)
    form.password = ''
    if (row.houseId) cascaderValue.value = row.houseId
    else cascaderValue.value = null
  } else {
    Object.assign(form, { id: null, username: '', realName: '', phone: '', role: 'OWNER', password: '', houseId: null })
    cascaderValue.value = null
  }
  await loadHouseTree()
  if (row && row.houseId) cascaderValue.value = row.houseId
  dialogVisible.value = true
}

const loadHouseTree = async () => {
  const bRes = await request.get('/users/buildings')
  const tree = []
  for (const b of bRes.data) {
    try {
      const hRes = await request.get('/users/houses', { params: { buildingId: b.id } })
      tree.push({
        id: b.id, name: b.name,
        children: hRes.data.map(h => ({ id: h.id, name: h.roomNo, isLeaf: true }))
      })
    } catch (e) { console.error(e) }
  }
  houseTree.value = tree
}

const onHouseChange = (val) => { form.houseId = val }

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (form.id) await request.put(`/users/${form.id}`, form)
      else await request.post('/users', form)
      dialogVisible.value = false
      ElMessage.success('操作成功')
      loadData()
    } catch (e) {}
  })
}

const handleDelete = async (id) => {
  await request.delete(`/users/${id}`)
  ElMessage.success('删除成功')
  loadData()
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/users/status/${row.id}?status=${row.status}`)
    ElMessage.success('状态更新成功')
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.user-manage {
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
        background: linear-gradient(135deg, $primary, $primary-light);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
      }

      .header-title {
        font-size: 16px;
        font-weight: 700;
        color: $text-primary;
      }

      .header-desc {
        font-size: 12px;
        color: $text-secondary;
        margin-top: 2px;
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
      align-items: center;

      .search-input { width: 220px; }
      .btn { font-weight: 500; }
    }
  }

  .data-table {
    margin-top: 8px;
  }

  .op-btn {
    font-weight: 500;
    .el-icon { margin-right: 2px; }
  }

  .pagination-box {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}

.user-dialog {
  :deep(.el-form-item__label) { font-weight: 500; }
}
</style>
