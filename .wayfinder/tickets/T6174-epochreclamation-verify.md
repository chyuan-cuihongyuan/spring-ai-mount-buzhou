---
id: T6174
title: S 会话 S37 Epoch-Based Reclamation 时代回收的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6173]
created: 2026-09-25
---

## Question

S37 合同怎么逐一验绿？（spec 5036 / effort #5036 / S37）

## Resolution

**验证通过**：EpochReclamationTest 七测全绿——守卫钉住不可
回收/退出可收；推进即可收；时代粒度分拣（E1 守卫不挡 E0）；
回收序确定性；守卫计数配平；双重关闭/重复退休 fail-fast。
