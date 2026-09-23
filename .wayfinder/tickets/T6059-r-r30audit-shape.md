---
id: T6059
title: R 会话 R30 周期对账的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

Wave 5 五新类型的快照/档案/台账怎么对齐？（spec 4029 / effort #4029 / R30）

## Resolution

**对账轮收口**：快照补登 1148→1153（TopologySpread/Supervisor/
Speculative/StableMatching/DynamicSnitch——policy×4+runaway×1+
exec×1 三段）；api-surface.md 同步 +5 行；CONTEXT 计数 1148→1153；
全仓 16 模块离线 verify 三门绿（排除 R18 已入档 ShadowMirror
flaky）；台账核账 4000–4028 廿九轮零缺位。
