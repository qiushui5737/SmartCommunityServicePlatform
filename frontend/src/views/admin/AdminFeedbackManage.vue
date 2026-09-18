<template>
  <div class="admin-feedback-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </div>
            <div>
              <div class="header-title">留言反馈管理</div>
              <div class="header-desc">处理业主建议、投诉和咨询</div>
            </div>
          </div>
          <div class="header-right">
            <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 120px" @change="loadData">
              <el-option label="建议" value="SUGGESTION" />
              <el-option label="投诉" value="COMPLAINT" />
              <el-option label="咨询" value="INQUIRY" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 120px" @change="loadData">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已回复" value="REPLIED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </div>
        </div>
      </template>

      <!-- Stats -->
      <div class="stat-row">
        <div v-for="s in stats" :key="s.label" class="stat-box" :class="s.cls">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-lbl">{{ s.label }}</div>
        </div>
      </div>

      <!-- Table -->
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="feedbackNo" label="反馈单号" width="200" align="center" />
        <el-table-column label="业主" width="100">
          <template #default="{ row }">{{ row.ownerName || '未知' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.tag" size="small" effect="plain">{{ typeMap[row.type]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="图片" width="60" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.images" :size="16" color="#3b82f6"><Picture /></el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small" effect="dark">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复" width="60" align="center">
          <template #default="{ row }">
            <span :class="{ 'has-reply': row.replyCount > 0 }">{{ row.replyCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button link type="danger" @click="handleClose(row)" v-if="row.status !== 'CLOSED'">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="page.total > page.size">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadData" layout="total, prev, pager, next" />
      </div>
    </el-card>

    <!-- ===================== Detail Drawer ===================== -->
    <el-drawer v-model="showDetail" :title="detailData?.title || '反馈详情'" size="500px">
      <template v-if="detailData">
        <div class="detail-info">
          <div class="detail-tags">
            <el-tag :type="typeMap[detailData.type]?.tag" size="small" effect="plain">{{ typeMap[detailData.type]?.text }}</el-tag>
            <el-tag :type="statusMap[detailData.status]?.type" size="small" effect="dark">{{ statusMap[detailData.status]?.text }}</el-tag>
          </div>
          <div class="detail-owner">提交人：{{ detailData.ownerName || '业主' }}  |  {{ detailData.createTime }}</div>
          <div class="detail-content">{{ detailData.content }}</div>
          <!-- 图片展示 -->
          <div class="detail-images" v-if="adminDetailImages.length">
            <el-image v-for="(img, idx) in adminDetailImages" :key="idx" :src="img" :preview-src-list="adminDetailImages" :initial-index="idx" fit="cover" class="detail-img" />
          </div>
        </div>

        <!-- Replies -->
        <div class="reply-section">
          <div class="reply-section-title">
            <el-icon :size="16"><ChatLineSquare /></el-icon>
            对话记录（{{ replies.length }}）
          </div>
          <div v-if="replies.length === 0" class="reply-empty">暂无回复</div>
          <div class="reply-timeline">
            <div v-for="r in replies" :key="r.id" class="reply-bubble" :class="r.userRole === 'OWNER' ? 'theirs' : 'mine'">
              <div class="bubble-head">
                <el-avatar :size="28" :class="r.userRole === 'OWNER' ? 'av-owner' : 'av-admin'">
                  {{ r.userName?.charAt(0) || (r.userRole === 'OWNER' ? '业' : '物') }}
                </el-avatar>
                <div class="bubble-meta">
                  <span class="bubble-name">{{ r.userName || (r.userRole === 'OWNER' ? '业主' : '物业') }}</span>
                  <span class="bubble-time">{{ r.createTime }}</span>
                </div>
              </div>
              <div class="bubble-body">{{ r.content }}</div>
            </div>
          </div>
        </div>

        <!-- Reply Input -->
        <div class="reply-input-area" v-if="detailData.status !== 'CLOSED'">
          <el-input v-model="replyText" type="textarea" :rows="3" placeholder="输入回复内容..." maxlength="500" />
          <el-button type="primary" :loading="replying" @click="handleReply" :disabled="!replyText.trim()">
            <el-icon><Promotion /></el-icon>回复
          </el-button>
        </div>
        <div v-else class="closed-hint">此反馈已关闭</div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, ChatLineSquare, Promotion, Picture } from '@element-plus/icons-vue'

const typeMap = {
  SUGGESTION: { text: '建议', tag: 'primary' },
  COMPLAINT: { text: '投诉', tag: 'danger' },
  INQUIRY: { text: '咨询', tag: 'success' }
}
const statusMap = {
  PENDING: { text: '待处理', type: 'warning' },
  PROCESSING: { text: '处理中', type: 'primary' },
  REPLIED: { text: '已回复', type: 'success' },
  CLOSED: { text: '已关闭', type: 'info' }
}

const filterType = ref('')
const filterStatus = ref('')
const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const stats = reactive([
  { label: '总反馈', value: 0, cls: 's-total' },
  { label: '待处理', value: 0, cls: 's-pending' },
  { label: '已回复', value: 0, cls: 's-replied' },
  { label: '已关闭', value: 0, cls: 's-closed' }
])

// Detail
const showDetail = ref(false)
const detailData = ref(null)
const replies = ref([])
const replyText = ref('')
const replying = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: page.current, size: page.size }
    if (filterType.value) params.type = filterType.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await request.get('/feedback/page', { params })
    list.value = res.data.records || []
    page.total = res.data.total || 0
    calcStats()
  } catch (e) {
    console.error('加载反馈列表失败:', e)
  } finally { loading.value = false }
}

const calcStats = async () => {
  try {
    const all = await request.get('/feedback/page', { params: { current: 1, size: 999 } })
    const records = all.data.records || []
    stats[0].value = all.data.total || 0
    stats[1].value = records.filter(r => r.status === 'PENDING').length
    stats[2].value = records.filter(r => r.status === 'REPLIED').length
    stats[3].value = records.filter(r => r.status === 'CLOSED').length
  } catch (e) {}
}

const openDetail = async (item) => {
  detailData.value = item
  showDetail.value = true
  replyText.value = ''
  try {
    const res = await request.get(`/feedback/${item.id}/replies`)
    replies.value = res.data || []
  } catch (e) { replies.value = [] }
}

const adminDetailImages = computed(() => {
  if (!detailData.value?.images) return []
  return detailData.value.images.split(',').filter(Boolean)
})

const handleReply = async () => {
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    await request.post(`/feedback/${detailData.value.id}/reply`, { content: replyText.value })
    ElMessage.success('回复成功')
    replyText.value = ''
    const res = await request.get(`/feedback/${detailData.value.id}/replies`)
    replies.value = res.data || []
    detailData.value.status = 'REPLIED'
    loadData()
  } catch (e) { ElMessage.error('回复失败') }
  finally { replying.value = false }
}

const handleClose = async (item) => {
  try {
    await ElMessageBox.confirm('确定关闭该反馈？', '提示', { type: 'warning' })
    await request.put(`/feedback/${item.id}/close`)
    ElMessage.success('已关闭')
    loadData()
  } catch (e) {}
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.admin-feedback-manage {
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    .header-left {
      display: flex; align-items: center; gap: 14px;
      .header-icon {
        width: 48px; height: 48px;
        background: linear-gradient(135deg, #3b82f6, #2563eb);
        border-radius: 14px; display: flex; align-items: center; justify-content: center;
        color: white; box-shadow: 0 4px 12px rgba(37,99,235,0.25);
      }
      .header-title { font-size: 18px; font-weight: 700; color: $text-primary; }
      .header-desc { font-size: 13px; color: $text-secondary; margin-top: 2px; }
    }
    .header-right { display: flex; gap: 8px; }
  }
}

.stat-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px;
  .stat-box {
    padding: 16px; border-radius: 12px; text-align: center;
    background: $gray-50; border: 1px solid $border-color-light;
    .stat-num { font-size: 26px; font-weight: 700; letter-spacing: -0.02em; }
    .stat-lbl { font-size: 12px; color: $text-secondary; margin-top: 2px; }
    &.s-total .stat-num { color: $text-primary; }
    &.s-pending .stat-num { color: #f59e0b; }
    &.s-replied .stat-num { color: #10b981; }
    &.s-closed .stat-num { color: $text-tertiary; }
  }
}

.has-reply { color: $primary; font-weight: 700; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

/* Detail Drawer */
.detail-info {
  padding: 16px 20px; background: $gray-50; border-radius: 12px; margin-bottom: 20px;
  .detail-tags { display: flex; gap: 8px; margin-bottom: 10px; }
  .detail-owner { font-size: 12px; color: $text-tertiary; margin-bottom: 10px; }
  .detail-content { font-size: 14px; line-height: 1.7; color: $text-primary; }
  .detail-images {
    display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px;
    .detail-img {
      width: 100px; height: 100px; border-radius: 8px; cursor: pointer;
      border: 1px solid $border-color-light;
    }
  }
}

.reply-section {
  margin-bottom: 20px;
  .reply-section-title { font-size: 15px; font-weight: 700; color: $text-primary; margin-bottom: 14px; display: flex; align-items: center; gap: 6px; }
  .reply-empty { text-align: center; padding: 24px; color: $text-tertiary; font-size: 13px; }
}

.reply-timeline {
  display: flex; flex-direction: column; gap: 14px;
  .reply-bubble {
    display: flex; flex-direction: column; gap: 6px;
    &.mine { align-items: flex-end; }
    &.theirs { align-items: flex-start; }
    .bubble-head { display: flex; align-items: center; gap: 8px; }
    .av-owner { background: linear-gradient(135deg, #14b8a6, #0d9488); color: white; font-size: 12px; }
    .av-admin { background: linear-gradient(135deg, #3b82f6, #2563eb); color: white; font-size: 12px; }
    .bubble-meta {
      .bubble-name { font-size: 13px; font-weight: 600; color: $text-primary; }
      .bubble-time { font-size: 11px; color: $text-tertiary; margin-left: 8px; }
    }
    .bubble-body {
      max-width: 85%; padding: 10px 14px; border-radius: 12px; font-size: 13px; line-height: 1.6;
    }
    &.mine .bubble-body {
      background: #dbeafe; color: $text-primary; border-bottom-right-radius: 4px;
    }
    &.theirs .bubble-body {
      background: $gray-100; color: $text-primary; border-bottom-left-radius: 4px;
    }
  }
}

.reply-input-area {
  display: flex; gap: 8px; align-items: flex-end;
  padding: 14px 0; border-top: 1px solid $border-color-light;
  .el-input { flex: 1; }
}
.closed-hint {
  text-align: center; padding: 14px; color: $text-tertiary; font-size: 13px;
  background: $gray-50; border-radius: 8px;
}
</style>
