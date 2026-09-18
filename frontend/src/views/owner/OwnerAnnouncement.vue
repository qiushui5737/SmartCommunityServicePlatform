<template>
  <div class="owner-announcement">
    <!-- 搜索筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索公告标题或关键词" clearable style="width: 280px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="type-tabs">
          <el-radio-group v-model="query.type" @change="loadData">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="NOTICE">通知</el-radio-button>
            <el-radio-button value="ACTIVITY">活动</el-radio-button>
            <el-radio-button value="MAINTENANCE">维护</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <!-- 公告卡片列表 -->
    <div v-loading="loading" class="announcement-list">
      <div v-if="list.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无公告" />
      </div>
      <div v-for="item in list" :key="item.id" class="announcement-card" @click="openDetail(item)">
        <div class="card-header">
          <div class="card-tags">
            <el-tag v-if="item.pinned" type="warning" effect="dark" size="small" class="pin-tag">
              <el-icon><Top /></el-icon> 置顶
            </el-tag>
            <el-tag :type="typeMap[item.type]?.tagType" effect="light" size="small">
              {{ typeMap[item.type]?.label }}
            </el-tag>
          </div>
          <span class="card-time">{{ item.publishTime }}</span>
        </div>
        <h3 class="card-title">{{ item.title }}</h3>
        <p class="card-summary">{{ getSummary(item.content) }}</p>
        <div class="card-footer">
          <span class="card-publisher">
            <el-icon><User /></el-icon>
            {{ item.publisherName || '管理员' }}
          </span>
          <el-button link type="primary" @click.stop="openDetail(item)">查看详情</el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailVisible" title="公告详情" width="700px" destroy-on-close>
      <div v-if="detail" class="detail-content">
        <div class="detail-meta">
          <div class="meta-tags">
            <el-tag :type="typeMap[detail.type]?.tagType" effect="light">
              {{ typeMap[detail.type]?.label }}
            </el-tag>
            <el-tag v-if="detail.pinned" type="warning" effect="dark" size="small">
              <el-icon><Top /></el-icon> 置顶
            </el-tag>
          </div>
          <div class="meta-info">
            <span><el-icon><User /></el-icon> {{ detail.publisherName || '管理员' }}</span>
            <span><el-icon><Clock /></el-icon> {{ detail.publishTime }}</span>
          </div>
        </div>
        <el-divider />
        <h2 class="detail-title">{{ detail.title }}</h2>
        <div class="detail-body" v-html="detail.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAnnouncementPage, getAnnouncementDetail } from '@/api/announcement'

const typeMap = {
  NOTICE: { label: '通知', tagType: 'primary' },
  ACTIVITY: { label: '活动', tagType: 'success' },
  MAINTENANCE: { label: '维护', tagType: 'warning' }
}

// ====== 列表 ======
const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const query = reactive({ keyword: '', type: '' })

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: page.current,
      size: page.size,
      type: query.type || undefined,
      keyword: query.keyword || undefined,
      status: 'PUBLISHED' // 业主只看已发布
    }
    const res = await getAnnouncementPage(params)
    list.value = res.data.records
    page.total = res.data.total
  } finally {
    loading.value = false
  }
}

/**
 * 从 HTML 内容提取纯文本摘要
 */
const getSummary = (html) => {
  if (!html) return '暂无内容'
  const text = html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
  return text.length > 120 ? text.slice(0, 120) + '...' : text
}

// ====== 详情 ======
const detailVisible = ref(false)
const detail = ref(null)

const openDetail = async (item) => {
  const res = await getAnnouncementDetail(item.id)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-announcement {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.type-tabs {
  flex-shrink: 0;
}

.announcement-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.empty-state {
  grid-column: 1 / -1;
}

.announcement-card {
  background: white;
  border-radius: $radius-lg;
  padding: 22px;
  box-shadow: $shadow-sm;
  border: 1px solid $border-color;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
  gap: 12px;

  &:hover {
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
    transform: translateY(-6px);
    border-color: $primary-200;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-tags {
  display: flex;
  gap: 6px;
  align-items: center;

  .pin-tag {
    display: flex;
    align-items: center;
    gap: 3px;
    font-weight: 700;
    box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
  }
}

.card-time {
  font-size: 13px;
  color: $text-tertiary;
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.4;
  margin: 0;
  letter-spacing: -0.01em;
}

.card-summary {
  font-size: 13px;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0;
  flex: 1;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid $border-color-light;

  .card-publisher {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: $text-tertiary;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 8px 0;
}

/* 详情弹窗 */
.detail-content {
  padding: 0 8px;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  .meta-tags {
    display: flex;
    gap: 6px;
  }

  .meta-info {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: $text-tertiary;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.detail-title {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 16px;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.detail-body {
  font-size: 14px;
  line-height: 1.8;
  color: $text-secondary;

  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
  }
}
</style>
