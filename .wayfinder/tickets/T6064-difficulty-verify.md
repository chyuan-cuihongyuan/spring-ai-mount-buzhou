---
id: T6064
title: R 会话 R32 难度目标重定的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6063]
created: 2026-09-23
---

## Question

R32 合同怎么逐一验绿？（spec 4031 / effort #4031 / R32）

## Resolution

**验证通过**：DifficultyRetargetTest 七测全绿——准时不变/
快 2× 减半/慢 8× 钳制 ×4/powLimit 封顶四证；窗滚动再重定 +
blocksToRetarget 读数；倒流与畸形定构 fail-fast；确定性回放
同轨迹。
