---
description: Build and unit-test the Vue frontend in an isolated subagent
agent: build
subtask: true
---

Frontend lives in `@fronted/` (note the directory name), Node requirement `>=20.0.0` (`engines` in `@fronted/package.json`).

Run in order (repo root, Linux container; current `~/.nvm` Node v18 is BELOW the requirement — use a Node>=20 toolchain if available, otherwise report the version gap explicitly):

1. `npm ci` (only if `node_modules` missing or `package-lock.json` changed).
2. `npm run build` — must pass (`vue-tsc -b && vite build`).
3. `npm run test` — `vitest run`, all green.

Report: Node/npm versions, build status, test counts. Do not downgrade the `engines` requirement to fit the local Node; surface the mismatch instead.
