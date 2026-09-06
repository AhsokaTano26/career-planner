export type AdminStatusTone = 'success' | 'warning' | 'danger' | 'neutral'

export type AdminStatusPresentation = {
  label: string
  tone: AdminStatusTone
}

const labels: Record<string, string> = {
  DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用', COMPLETED: '已完成',
  PENDING: '待处理', PROCESSING: '处理中', FAILED: '失败', APPROVED: '已通过',
  REJECTED: '已驳回', MERGED: '已合并', SUCCESS: '成功', ERROR: '失败',
  ACTIVE: '已启用', USED: '已使用', UPLOADED: '已上传', PARSING: '解析中', INFO: '记录', WARN: '警告', WARNING: '警告',
}

export function adminStatusPresentation(input: unknown): AdminStatusPresentation {
  const status = String(input ?? '').toUpperCase()
  const label = labels[status] || (status || '—')
  if (['ACTIVE', 'PUBLISHED', 'SUCCESS', 'COMPLETED', 'APPROVED', 'USED'].includes(status)) return { label, tone: 'success' }
  if (['PENDING', 'PROCESSING', 'UPLOADED', 'PARSING', 'WARN', 'WARNING'].includes(status)) return { label, tone: 'warning' }
  if (['FAILED', 'REJECTED', 'ERROR'].includes(status)) return { label, tone: 'danger' }
  return { label, tone: 'neutral' }
}
