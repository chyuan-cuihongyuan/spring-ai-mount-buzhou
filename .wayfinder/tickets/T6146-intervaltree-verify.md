---
id: T6146
title: S 会话 S23 区间树 stabbing 查询的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6145]
created: 2026-09-24
---

## Question

S23 合同怎么逐一验绿？（spec 5022 / effort #5022 / S23）

## Resolution

**验证通过**：IntervalTreeTest 四测全绿——固定集 stabbing vs
线性扫圣像全等（边界/中点/稀疏域）；空树空结果；倒置区间
fail-fast；确定性回放。
