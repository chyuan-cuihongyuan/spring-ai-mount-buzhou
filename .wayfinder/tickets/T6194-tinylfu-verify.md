---
id: T6194
title: S 会话 S47 TinyLFU Admission 准入策略的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6193]
created: 2026-09-25
---

## Question

S47 合同怎么逐一验绿？（spec 5046 / effort #5046 / S47）

## Resolution

**验证通过**：TinyLfuAdmissionTest 四测全绿——大容量单调
饱和 15；准入方向（热进冷拒/追平即进）；小容量老化（第 8
记自增后减半 8→4、resetCount=1）；null/参数 fail-fast。
