---
id: T6070
title: R 会话 R35 JSON Patch 应用的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6069]
created: 2026-09-23
---

## Question

R35 合同怎么逐一验绿？（spec 4034 / effort #4034 / R35）

## Resolution

**验证通过**：JsonPatchApplierTest 八测全绿——六操作正反例
（add 移位/- 追加、replace 缺路径拒、move 下标记账、test
失败整patch拒）；~0/~1 转义；坏转义/非数字下标/remove `-`
fail-fast；原子性（失败后原文档不变）。
