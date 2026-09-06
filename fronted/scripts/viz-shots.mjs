/**
 * 可视化基线截图（供 /viz-check 调用）。
 * 登录李明号 → 总览 → 发展页（测评评分 + 画像）→ 按三断点截图到 docs/viz-baseline/。
 * 运行：node docs/scripts/viz-shots.mjs（需 Node>=20；用 ~/.cache/ms-playwright 内核，无 sudo 可装）
 */
import { chromium } from 'playwright-core'

const BASE = 'http://127.0.0.1:5173'
const OUT = new URL('../../docs/viz-baseline/', import.meta.url)
const { mkdirSync } = await import('node:fs')
mkdirSync(OUT, { recursive: true })

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })

// 登录
await page.goto(`${BASE}/login`)
await page.getByPlaceholder('账号或学号').fill('2026011301')
await page.getByPlaceholder('输入密码').fill('Student@2026')
await page.getByRole('button', { name: /登录系统/ }).click()
await page.waitForURL('**/student/overview', { timeout: 20000 })

const shots = []
async function shot(name, width) {
  await page.setViewportSize({ width, height: 900 })
  await page.waitForTimeout(1200) // 等 echarts 动画与接口落定
  const file = new URL(`./${name}-${width}.png`, OUT).pathname
  await page.screenshot({ path: file, fullPage: true })
  shots.push(file)
}

// 总览（含迷你雷达）
await page.goto(`${BASE}/student/overview`)
await page.waitForTimeout(1500)
for (const w of [1440, 768, 390]) await shot('overview', w)

// 发展页：先点开历史测评看到评分雷达
await page.goto(`${BASE}/student/development`)
await page.waitForTimeout(1500)
// 点第一条历史会话（SCORED 则自动加载 scores 雷达）
const sessions = page.locator('section article.card', { hasText: '我的测评会话' }).locator('button')
if ((await sessions.count()) > 0) {
  await sessions.first().click()
  await page.waitForTimeout(1500)
}
for (const w of [1440, 768, 390]) await shot('assessment', w)

// 画像 tab
await page.getByRole('button', { name: '个人画像' }).click()
await page.waitForTimeout(1500)
for (const w of [1440, 768, 390]) await shot('portrait', w)

await browser.close()
console.log(shots.join('\n'))
