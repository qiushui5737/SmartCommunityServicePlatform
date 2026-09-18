<template>
  <div class="owner-access-card">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon :size="20"><Key /></el-icon>
            </div>
            <div>
              <div class="header-title">我的门禁卡</div>
              <div class="header-desc">查看和管理您的门禁卡信息</div>
            </div>
          </div>
        </div>
      </template>

      <!-- Cards Grid -->
      <div v-loading="loading" class="cards-grid">
        <div v-if="cards.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无门禁卡，请联系物业办理" />
        </div>
        <div v-for="card in cards" :key="card.id" class="access-card-item" :class="'st-' + card.status">
          <!-- Card visual -->
          <div class="card-visual">
            <div class="card-chip">
              <el-icon :size="24"><Key /></el-icon>
            </div>
            <div class="card-no">{{ card.cardNo }}</div>
            <div class="card-type-badge">
              <el-tag :type="cardTypeMap[card.cardType]?.tag" size="small" effect="dark">{{ cardTypeMap[card.cardType]?.text }}</el-tag>
            </div>
          </div>

          <!-- Card info -->
          <div class="card-info">
            <div class="info-row">
              <span class="info-label">状态</span>
              <el-tag :type="statusMap[card.status]?.type" effect="plain" size="small">{{ statusMap[card.status]?.text }}</el-tag>
            </div>
            <div class="info-row">
              <span class="info-label">通行楼栋</span>
              <span class="info-value">{{ card.buildingIds || '全部楼栋' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">有效期</span>
              <span class="info-value">{{ formatValidity(card) }}</span>
            </div>
            <div class="info-row" v-if="card.remark">
              <span class="info-label">备注</span>
              <span class="info-value">{{ card.remark }}</span>
            </div>
          </div>

          <!-- Actions -->
          <div class="card-actions" v-if="card.status === 'ACTIVE'">
            <el-button type="warning" plain size="small" @click="handleSuspend(card)">
              <el-icon><WarningFilled /></el-icon>挂失
            </el-button>
          </div>
          <div class="card-actions" v-if="card.status === 'SUSPENDED'">
            <el-button type="success" plain size="small" disabled>已冻结</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, WarningFilled } from '@element-plus/icons-vue'

const cardTypeMap = {
  OWNER: { text: '业主卡', tag: 'primary' },
  FAMILY: { text: '家庭卡', tag: 'success' },
  VISITOR: { text: '访客卡', tag: 'warning' },
  TEMPORARY: { text: '临时卡', tag: 'info' }
}
const statusMap = {
  ACTIVE: { text: '正常', type: 'success' },
  SUSPENDED: { text: '已挂失', type: 'danger' },
  CANCELLED: { text: '已注销', type: 'info' }
}

const cards = ref([])
const loading = ref(false)

const formatValidity = (card) => {
  if (!card.validFrom && !card.validTo) return '永久有效'
  let s = card.validFrom || '—'
  s += ' 至 '
  s += card.validTo || '永久'
  return s
}

const loadCards = async () => {
  loading.value = true
  try {
    const res = await request.get('/access-card/my')
    cards.value = res.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleSuspend = async (card) => {
  try {
    await ElMessageBox.confirm('确定挂失该门禁卡？挂失后将立即冻结使用权限。', '挂失确认', { type: 'warning' })
    await request.put(`/access-card/${card.id}/suspend`)
    ElMessage.success('挂失成功，卡片已冻结')
    loadCards()
  } catch (e) {}
}

onMounted(() => loadCards())
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.owner-access-card {
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    .header-left {
      display: flex; align-items: center; gap: 14px;
      .header-icon {
        width: 48px; height: 48px;
        background: linear-gradient(135deg, #10b981, #059669);
        border-radius: 14px; display: flex; align-items: center; justify-content: center;
        color: white; box-shadow: 0 4px 12px rgba(16,185,129,0.25);
      }
      .header-title { font-size: 18px; font-weight: 700; color: $text-primary; }
      .header-desc { font-size: 13px; color: $text-secondary; margin-top: 2px; }
    }
  }
}

.cards-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px;
  min-height: 120px;
}

.access-card-item {
  border-radius: 16px; overflow: hidden;
  border: 1px solid $border-color;
  transition: all 0.25s cubic-bezier(0.16,1,0.3,1);
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
  &.st-SUSPENDED { opacity: 0.7; }
  &.st-CANCELLED { opacity: 0.5; }
}

.card-visual {
  padding: 20px; position: relative;
  background: linear-gradient(135deg, #0f766e 0%, #115e59 60%, #134e4a 100%);
  color: white;
  .card-chip {
    width: 44px; height: 44px; border-radius: 10px;
    background: rgba(255,255,255,0.15); display: flex; align-items: center; justify-content: center;
    margin-bottom: 12px; border: 1px solid rgba(255,255,255,0.1);
  }
  .card-no { font-size: 20px; font-weight: 700; letter-spacing: 0.08em; font-family: $font-mono; }
  .card-type-badge { position: absolute; top: 16px; right: 16px; }
  .st-SUSPENDED & { background: linear-gradient(135deg, #991b1b 0%, #7f1d1d 100%); }
  .st-CANCELLED & { background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%); }
}

.card-info {
  padding: 16px 20px;
  .info-row {
    display: flex; align-items: center; justify-content: space-between;
    padding: 6px 0; border-bottom: 1px solid $border-color-light;
    &:last-child { border-bottom: none; }
    .info-label { font-size: 13px; color: $text-secondary; font-weight: 500; }
    .info-value { font-size: 13px; color: $text-primary; font-weight: 600; }
  }
}

.card-actions {
  padding: 12px 20px; border-top: 1px solid $border-color-light;
  display: flex; justify-content: flex-end; gap: 8px;
}
</style>
