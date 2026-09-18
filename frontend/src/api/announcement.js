import request from '@/utils/request'

/**
 * 公告分页列表
 * @param {Object} params - { current, size, type, keyword, status }
 */
export function getAnnouncementPage(params) {
  return request.get('/announcement/page', { params })
}

/**
 * 公告详情
 */
export function getAnnouncementDetail(id) {
  return request.get(`/announcement/${id}`)
}

/**
 * 发布公告
 */
export function createAnnouncement(data) {
  return request.post('/announcement', data)
}

/**
 * 编辑公告
 */
export function updateAnnouncement(id, data) {
  return request.put(`/announcement/${id}`, data)
}

/**
 * 撤回公告
 */
export function withdrawAnnouncement(id) {
  return request.put(`/announcement/${id}/withdraw`)
}

/**
 * 删除公告
 */
export function deleteAnnouncement(id) {
  return request.delete(`/announcement/${id}`)
}
