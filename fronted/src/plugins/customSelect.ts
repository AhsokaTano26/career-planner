type Cleanup = () => void

const enhanced = new WeakMap<HTMLSelectElement, Cleanup>()
let documentObserver: MutationObserver | undefined
let nextMenuId = 0

function selectOptions(select: HTMLSelectElement) {
  return [...select.options]
}

/** 将一个原生 select 呈现为学院统一样式，同时保留表单值与键盘操作。 */
export function enhanceSelect(select: HTMLSelectElement): Cleanup {
  const current = enhanced.get(select)
  if (current) return current

  const root = document.createElement('div')
  const trigger = document.createElement('button')
  const menu = document.createElement('div')
  const menuId = `native-select-menu-${++nextMenuId}`
  let closed = false

  root.className = 'native-select-renderer'
  trigger.type = 'button'
  trigger.className = 'native-select-trigger'
  trigger.setAttribute('aria-haspopup', 'listbox')
  trigger.setAttribute('aria-expanded', 'false')
  trigger.setAttribute('aria-controls', menuId)
  menu.id = menuId
  menu.className = 'native-select-menu'
  menu.setAttribute('role', 'listbox')

  select.classList.add('native-select-source')
  select.insertAdjacentElement('afterend', root)
  root.append(trigger, menu)

  const isOpen = () => root.classList.contains('open')
  const close = () => {
    root.classList.remove('open')
    trigger.setAttribute('aria-expanded', 'false')
  }
  const open = () => {
    if (select.disabled) return
    root.classList.add('open')
    trigger.setAttribute('aria-expanded', 'true')
  }
  const render = () => {
    const selected = select.selectedOptions[0]
    const caret = document.createElement('i')
    caret.textContent = '⌄'
    trigger.replaceChildren(document.createTextNode(selected?.textContent || '请选择'), caret)
    menu.replaceChildren(...selectOptions(select).map((option, index) => {
      const item = document.createElement('button')
      item.type = 'button'
      item.textContent = option.text
      item.disabled = option.disabled
      item.id = `${menuId}-option-${index}`
      item.setAttribute('role', 'option')
      item.setAttribute('aria-selected', String(option.selected))
      item.className = option.selected ? 'selected' : ''
      item.addEventListener('click', () => choose(index))
      return item
    }))
  }
  const choose = (index: number) => {
    const option = selectOptions(select)[index]
    if (!option || option.disabled) return
    select.selectedIndex = index
    select.dispatchEvent(new Event('change', { bubbles: true }))
    render()
    close()
    trigger.focus()
  }
  const move = (offset: number) => {
    const options = selectOptions(select)
    let index = select.selectedIndex
    do {
      index = Math.min(options.length - 1, Math.max(0, index + offset))
      if (!options[index]?.disabled) return choose(index)
    } while (index > 0 && index < options.length - 1)
  }
  const onTriggerClick = () => (isOpen() ? close() : open())
  const onKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      if (isOpen()) event.preventDefault()
      close()
      return
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      isOpen() ? close() : open()
      return
    }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      open()
      move(event.key === 'ArrowDown' ? 1 : -1)
    }
  }
  const onDocumentClick = (event: MouseEvent) => {
    if (!root.contains(event.target as Node)) close()
  }
  const onSelectChange = () => render()
  const optionsObserver = new MutationObserver(render)

  trigger.addEventListener('click', onTriggerClick)
  trigger.addEventListener('keydown', onKeydown)
  document.addEventListener('click', onDocumentClick)
  select.addEventListener('change', onSelectChange)
  optionsObserver.observe(select, { childList: true, subtree: true, attributes: true })
  render()

  const cleanup = () => {
    if (closed) return
    closed = true
    trigger.removeEventListener('click', onTriggerClick)
    trigger.removeEventListener('keydown', onKeydown)
    document.removeEventListener('click', onDocumentClick)
    select.removeEventListener('change', onSelectChange)
    optionsObserver.disconnect()
    root.remove()
    select.classList.remove('native-select-source')
    enhanced.delete(select)
  }
  enhanced.set(select, cleanup)
  return cleanup
}

function cleanRemoved(node: Node) {
  if (!(node instanceof Element)) return
  if (node instanceof HTMLSelectElement) enhanced.get(node)?.()
  node.querySelectorAll?.('select').forEach(item => enhanced.get(item)?.())
}

/** 维护应用内新增的原生 select，并在页面卸载时自动释放增强器。 */
export function installCustomSelects() {
  const scan = () => document.querySelectorAll<HTMLSelectElement>('select').forEach(enhanceSelect)
  scan()
  if (documentObserver) return
  documentObserver = new MutationObserver(records => {
    records.forEach(record => record.removedNodes.forEach(cleanRemoved))
    scan()
  })
  documentObserver.observe(document.body, { childList: true, subtree: true })
}
