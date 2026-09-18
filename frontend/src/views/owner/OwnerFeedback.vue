<template>
  <div class="owner-feedback">
    <!-- Page Header -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </div>
            <div>
              <div class="header-title">留言反馈</div>
              <div class="header-desc">建议、投诉、咨询——您的声音对我们很重要</div>
            </div>
          </div>
          <el-button type="primary" @click="showSubmit = true">
            <el-icon><Plus /></el-icon>提交反馈
          </el-button>
        </div>
      </template>

      <!-- Stats -->
      <div class="stat-row">
        <div v-for="s in stats" :key="s.label" class="stat-box" :class="s.cls">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-lbl">{{ s.label }}</div>
        </div>
      </div>

      <!-- Type Tabs -->
      <div class="type-tabs">
        <div v-for="t in typeTabs" :key="t.value" class="type-tab" :class="{ active: activeType === t.value }" @click="activeType = t.value; loadData()">
          <el-icon v-if="t.icon"><component :is="t.icon" /></el-icon>
          {{ t.label }}
        </div>
      </div>

      <!-- Feedback List -->
      <div v-loading="loading" class="feedback-list">
        <div v-if="list.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无反馈记录" />
        </div>
        <div v-for="item in list" :key="item.id" class="fb-card" @click="openDetail(item)">
          <div class="fb-card-top">
            <span class="fb-no">{{ item.feedbackNo }}</span>
            <el-tag :type="typeMap[item.type]?.tag" size="small" effect="plain">{{ typeMap[item.type]?.text }}</el-tag>
            <el-tag :type="statusMap[item.status]?.type" size="small" effect="dark">{{ statusMap[item.status]?.text }}</el-tag>
          </div>
          <div class="fb-card-title">{{ item.title }}</div>
          <div class="fb-card-preview">{{ item.content }}</div>
          <div class="fb-card-images" v-if="item.images">
            <el-image v-for="(img, idx) in item.images.split(',').filter(Boolean).slice(0, 3)" :key="idx" :src="img" fit="cover" class="fb-thumb" />
          </div>
          <div class="fb-card-bottom">
            <span class="fb-time">{{ item.createTime }}</span>
            <span class="fb-reply-count" v-if="item.replyCount">
              <el-icon :size="14"><ChatLineSquare /></el-icon> {{ item.replyCount }} 条回复
            </span>
          </div>
        </div>
      </div>

      <div class="pagination-wrap" v-if="page.total > page.size">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" @current-change="loadData" layout="total, prev, pager, next" />
      </div>
    </el-card>

    <!-- ===================== 提交反馈 Dialog ===================== -->
    <el-dialog v-model="showSubmit" title="提交反馈" width="520px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px">
        <el-form-item label="反馈类型" required>
          <el-radio-group v-model="form.type">
            <el-radio-button value="SUGGESTION">建议</el-radio-button>
            <el-radio-button value="COMPLAINT">投诉</el-radio-button>
            <el-radio-button value="INQUIRY">咨询</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="简要描述您的反馈" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="详细内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请详细描述您的建议、投诉或咨询内容..." maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            v-model:file-list="formImages"
            action="/api/upload/image"
            :headers="uploadHeaders"
            list-type="picture-card"
            :limit="4"
            accept="image/*"
            :on-exceed="() => ElMessage.warning('最多上传4张图片')"
            :on-success="onImageSuccess"
            :on-remove="onImageRemove"
            :before-upload="beforeImageUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmit = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- ===================== 反馈详情 Drawer ===================== -->
    <el-drawer v-model="showDetail" :title="detailData?.title || '反馈详情'" size="480px">
      <template v-if="detailData">
        <!-- Info -->
        <div class="detail-info">
          <div class="detail-tags">
            <el-tag :type="typeMap[detailData.type]?.tag" size="small" effect="plain">{{ typeMap[detailData.type]?.text }}</el-tag>
            <el-tag :type="statusMap[detailData.status]?.type" size="small" effect="dark">{{ statusMap[detailData.status]?.text }}</el-tag>
          </div>
          <div class="detail-content">{{ detailData.content }}</div>
          <!-- 图片展示 -->
          <div class="detail-images" v-if="detailImages.length">
            <el-image v-for="(img, idx) in detailImages" :key="idx" :src="img" :preview-src-list="detailImages" :initial-index="idx" fit="cover" class="detail-img" />
          </div>
          <div class="detail-time">提交时间：{{ detailData.createTime }}</div>
        </div>

        <!-- Reply Timeline -->
        <div class="reply-section">
          <div class="reply-section-title">
            <el-icon :size="16"><ChatLineSquare /></el-icon>
            对话记录（{{ replies.length }}）
          </div>
          <div v-if="replies.length === 0" class="reply-empty">暂无回复</div>
          <div class="reply-timeline">
            <div v-for="r in replies" :key="r.id" class="reply-bubble" :class="r.userRole === 'OWNER' ? 'mine' : 'theirs'">
              <div class="bubble-head">
                <el-avatar :size="28" :class="r.userRole === 'OWNER' ? 'av-owner' : 'av-admin'">
                  {{ r.userName?.charAt(0) || (r.userRole === 'OWNER' ? '我' : '物') }}
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
          <el-input v-model="replyText" type="textarea" :rows="2" placeholder="输入回复内容..." maxlength="500" />
          <el-button type="primary" size="small" :loading="replying" @click="handleReply" :disabled="!replyText.trim()">发送</el-button>
        </div>
        <div v-else class="closed-hint">此反馈已关闭</div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Plus, ChatLineSquare } from '@element-plus/icons-vue'

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

const typeTabs = [
  { label: '全部', value: '', icon: null },
  { label: '建议', value: 'SUGGESTION', icon: 'Star' },
  { label: '投诉', value: 'COMPLAINT', icon: 'Warning' },
  { label: '咨询', value: 'INQUIRY', icon: 'QuestionFilled' }
]

const activeType = ref('')
const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const stats = reactive([
  { label: '总反馈', value: 0, cls: 's-total' },
  { label: '待处理', value: 0, cls: 's-pending' },
  { label: '已回复', value: 0, cls: 's-replied' },
  { label: '已关闭', value: 0, cls: 's-closed' }
])

// Submit
const showSubmit = ref(false)
const submitting = ref(false)
const form = ref({ type: 'SUGGESTION', title: '', content: '', images: '' })
const formImages = ref([])  // el-upload file list
const uploadHeaders = { Authorization: 'Bearer ' + sessionStorage.getItem('token') }

const beforeImageUpload = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过5MB')
    return false
  }
  return true
}

const onImageSuccess = (response) => {
  if (response.code === 200 && response.data?.url) {
    const urls = form.value.images ? form.value.images.split(',').filter(Boolean) : []
    urls.push(response.data.url)
    form.value.images = urls.join(',')
  }
}

const onImageRemove = (file) => {
  const url = file.response?.data?.url
  if (url) {
    const urls = form.value.images ? form.value.images.split(',').filter(u => u !== url) : []
    form.value.images = urls.join(',')
  }
}

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
    if (activeType.value) params.type = activeType.value
    const res = await request.get('/feedback/page', { params })
    list.value = res.data.records || []
    page.total = res.data.total || 0
    calcStats()
  } catch (e) {
    console.error('加载反馈失败:', e)
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

const handleSubmit = async () => {
  if (!form.value.title || !form.value.content) return ElMessage.warning('请填写标题和内容')
  submitting.value = true
  try {
    await request.post('/feedback', form.value)
    ElMessage.success('反馈提交成功')
    form.value = { type: 'SUGGESTION', title: '', content: '', images: '' }
    formImages.value = []
    showSubmit.value = false
    loadData()
  } catch (e) { ElMessage.error('提交失败') }
  finally { submitting.value = false }
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

const detailImages = computed(() => {
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
    loadData()
  } catch (e) { ElMessage.error('回复失败') }
  finally { replying.value = false }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-feedback {
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    .header-left {
      display: flex; align-items: center; gap: 14px;
      .header-icon {
        width: 48px; height: 48px;
        background: linear-gradient(135deg, #14b8a6, #0f766e);
        border-radius: 14px; display: flex; align-items: center; justify-content: center;
        color: white; box-shadow: 0 4px 12px rgba(15,118,110,0.25);
      }
      .header-title { font-size: 18px; font-weight: 700; color: $text-primary; letter-spacing: -0.01em; }
      .header-desc { font-size: 13px; color: $text-secondary; margin-top: 2px; }
    }
  }
}

/* Stats */
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

/* Type Tabs */
.type-tabs {
  display: flex; gap: 8px; margin-bottom: 18px;
  .type-tab {
    padding: 7px 16px; border-radius: 8px; font-size: 13px; font-weight: 600;
    color: $text-secondary; background: $gray-50; cursor: pointer;
    border: 1px solid transparent; transition: all 0.2s;
    display: flex; align-items: center; gap: 4px;
    &:hover { color: $primary; background: $primary-50; }
    &.active { color: white; background: $primary; border-color: $primary; box-shadow: 0 2px 8px rgba(37,99,235,0.2); }
  }
}

/* Feedback List */
.feedback-list {
  min-height: 200px;
  .fb-card {
    padding: 16px 18px; border-radius: 12px; margin-bottom: 12px;
    background: white; border: 1px solid $border-color-light; cursor: pointer;
    transition: all 0.25s cubic-bezier(0.16,1,0.3,1);
    &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-color: $primary; }
    .fb-card-top { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
    .fb-no { font-size: 11px; color: $text-tertiary; font-family: $font-mono; }
    .fb-card-title { font-size: 16px; font-weight: 700; color: $text-primary; margin-bottom: 4px; }
    .fb-card-preview {
      font-size: 13px; color: $text-secondary; line-height: 1.5;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .fb-card-images {
      display: flex; gap: 6px; margin-top: 8px;
      .fb-thumb {
        width: 56px; height: 56px; border-radius: 6px;
        border: 1px solid $border-color-light;
      }
    }
    .fb-card-bottom {
      display: flex; align-items: center; justify-content: space-between;
      margin-top: 10px; font-size: 12px; color: $text-tertiary;
      .fb-reply-count { display: flex; align-items: center; gap: 4px; color: $primary; font-weight: 500; }
    }
  }
}

.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

/* ===================== Detail Drawer ===================== */
.detail-info {
  padding: 16px 20px; background: $gray-50; border-radius: 12px; margin-bottom: 20px;
  .detail-tags { display: flex; gap: 8px; margin-bottom: 10px; }
  .detail-content { font-size: 14px; line-height: 1.7; color: $text-primary; margin-bottom: 10px; }
  .detail-images {
    display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px;
    .detail-img {
      width: 100px; height: 100px; border-radius: 8px; cursor: pointer;
      border: 1px solid $border-color-light;
    }
  }
  .detail-time { font-size: 12px; color: $text-tertiary; }
}

/* Reply Timeline */
.reply-section {
  margin-bottom: 20px;
  .reply-section-title {
    font-size: 15px; font-weight: 700; color: $text-primary; margin-bottom: 14px;
    display: flex; align-items: center; gap: 6px;
  }
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
      background: #e0f2fe; color: $text-primary; border-bottom-right-radius: 4px;
    }
    &.theirs .bubble-body {
      background: $gray-100; color: $text-primary; border-bottom-left-radius: 4px;
    }
  }
}

/* Reply Input */
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
