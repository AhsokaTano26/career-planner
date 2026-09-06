/** 分页钳制（复审 Batch4：筛选收敛后服务端总页收缩，旧大页码须回落末页重拉）。 */
export function resolvePage(
  requested: number,
  serverPage: number,
  totalPages: number,
): { page: number; refetch: boolean } {
  const total = Math.max(Math.floor(totalPages) || 1, 1)
  if (serverPage > total && requested > 1) {
    return { page: total, refetch: true }
  }
  return { page: Math.min(Math.max(Math.floor(serverPage) || 1, 1), total), refetch: false }
}
