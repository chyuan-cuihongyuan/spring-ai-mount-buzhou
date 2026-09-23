---
id: T6080
title: R 会话 R40 DDSketch 相对误差分位的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6079]
created: 2026-09-24
---

## Question

R40 合同怎么逐一验绿？（spec 4039 / effort #4039 / R40）

## Resolution

**验证通过**：DdSketchTest 六测全绿——1..1000 α=0.01 p50/p99
相对误差 <2.5%；min/max 精确；合并等价单建；γ 不一致/
空草图/α 越界/非正值 fail-fast。
