---
id: T6004
title: R 会话 R2 Count-Min 素材计数的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6003]
created: 2026-09-23
---

## Question

R2 合同怎么逐一验绿？（spec 4001 / effort #4001 / R2）

## Resolution

**验证通过**：CountMinSketchTest 四测全绿——单键 100 次精确直读
（零碰撞零噪声）+ 未见键为零；百键 ×100 各估计 ≥ 真值且 ≤ 真值+N/8
（单侧不低估 + 有界）；加权 10⁴/1 直读 + totalCount 守恒 + 维度读数；
畸形四型 fail-fast（depth 0/width 0/null key/负 n）。
