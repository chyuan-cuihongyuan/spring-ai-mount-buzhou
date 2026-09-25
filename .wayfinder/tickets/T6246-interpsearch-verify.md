---
id: T6246
title: T 会话 T23 Interpolation Search 插值查找的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6245]
created: 2026-09-26
---

## Question

T23 合同怎么逐一验绿？（spec 6022 / effort #6022 / T23）

## Resolution

**验证通过**：InterpolationSearchTest 五测全绿——均匀全键
命中；50×50 binarySearch 圣像；全等窗口；MIN/0/MAX 极值；
fail-fast。
