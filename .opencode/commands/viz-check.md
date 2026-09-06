---
description: Screenshot-verify frontend charts at 3 breakpoints and self-review against the design system
agent: build
subtask: true
---

Read the skill definition at `.opencode/skills/viz-design/SKILL.md` and follow it.

Run (repo root, Linux container):

1. `export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"; nvm use 20.20.2` then
   `LD_LIBRARY_PATH="$HOME/devtools/chromium-libs/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH" node fronted/scripts/viz-shots.mjs`
   — logs in as 李明 (2026011301), captures overview + assessment + portrait at 1440/768/390 into `docs/viz-baseline/`.
   Requires vite :5173, core :8080, MySQL running. Chromium headless-shell lives in `~/.cache/ms-playwright` (user-space `apt-get download` libs in `~/devtools/chromium-libs`, no sudo needed).
2. Read each PNG yourself and check: axis labels collision-free, legend readable, mobile single-column, colors only ink/lime/orange/grays, numbers table present beside every radar, no echarts default theme leakage (rounded tooltip, gradient, rainbow).
3. Fix discrepancies, re-shoot, repeat until pass. Report per-breakpoint verdict with file evidence.
