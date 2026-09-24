---
id: T6162
title: S 会话 S31 Wait-Die/Wound-Wait 死锁预防时序裁决的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6161]
created: 2026-09-24
---

## Question

S31 合同怎么逐一验绿？（spec 5030 / effort #5030 / S31）

## Resolution

**验证通过**：WoundWaitGateTest 九测全绿——空闲授予；四裁决
分叉（两模式×年龄）；release 交接年长等待者；finish 释放+
剔除；tie-break（a 枪伤 z）；畸形 fail-fast（含重复持有、
非持有者释放）。
