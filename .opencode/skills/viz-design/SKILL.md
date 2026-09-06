---
name: viz-design
description: Use when adding or changing any data visualization (radar, bar, line, progress) in the frontend, reviewing chart styling, or auditing charts against the design system.
---

# Viz Design Skill（生涯同行 · 图表强制规则）

> 综合 2026 年可视化共识（Antichaos/Flourish/quasa/Deckary 方法论）。雷达只讲“形状故事”，精确值永远靠数字/tooltip。

## 雷达图铁律

1. **同量纲归一**：所有轴同一量程（测评 0–5，画像 0–100），禁混轴；零基线。
2. **5–8 轴**：本项目固定 6 维；轴序固定且有业务含义（兴趣→价值观→能力→学业→倾向→实践），改序必须注释理由（轴序改变形状故事）。
3. **单图 1 个多边形**：本项目雷达只画本人（用户已裁决：基准线多余，不加目标圈/平均线）；多实体对比用 small multiples。
4. **HLTV 广播语言·重大蓝版（2026-09 用户裁决：去绿要蓝）**：标准蓝 `#004b9d` 3px 描边 + 同色辉光（`shadowBlur`）+ 14% 淡蓝填充 + 深海军蓝 `#003b7b` 方点 + 顶点 HUD 数字小牌（直角，白底藏青边，`hideOverlap` 防撞）+ 浅蓝灰实线网格 + 12px 等宽轴名；禁渐变。
5. ~~基准线必备~~（已废弃：用户明确不要目标圈；后端平均线待办保留在环境文档，不入图）。
6. **数字兜底**：雷达旁永远配数字表/条形（本项目 `metric-grid` + `dimension-list`），tooltip 直角单色。
7. **完工条形自检**：同一数据套条形图看结论是否一致；若条形下结论更清晰，雷达降为辅助。

## 主题铁律（设计系统）

- 色板唯一来源 `fronted/src/assets/styles/cqu-theme.css` 的 `:root` 变量（后加载覆盖，为最终生效值）；TS 侧只消费 `utils/theme-tokens.ts`，**禁手抄 hex**（漂移源）。
- 线框藏青 `#172b45` / 弱分割 `#c9d8e8` / 雷达标准蓝 `#004b9d` / 深海军蓝 `#003b7b` / 提醒橙红 `#FF5C35`；圆角 0；等宽字体做标签；禁彩虹色/渐变/玻璃态。
- 无障碍：容器 `tabindex+role=img+aria-label` 中文播报；`prefers-reduced-motion` 关动画（复用 `useReducedMotion`）。
- 移动端：640px 纵排全宽，与现有 media query 同值。

## 落点约定

- 纯函数进 `utils/radarOption.ts`（可单测），组件只做挂载/resize；echarts 按需引入 + 动态 `import()` 拆 chunk。
- 完工跑 `/viz-check`（1440/768/390 三断点截图自读）+ `npm run test/build`。
