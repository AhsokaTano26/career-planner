import { onScopeDispose, readonly, ref } from 'vue'

const QUERY = '(prefers-reduced-motion: reduce)'

/** 系统无障碍设置：用户要求减少动态时返回 true，并持续响应设置变化。 */
export function useReducedMotion() {
  const media = typeof window === 'undefined' ? null : window.matchMedia(QUERY)
  const reduced = ref(media?.matches ?? false)
  const update = (event: MediaQueryListEvent) => { reduced.value = event.matches }

  media?.addEventListener('change', update)
  onScopeDispose(() => media?.removeEventListener('change', update))

  return readonly(reduced)
}
