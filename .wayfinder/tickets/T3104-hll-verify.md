---
id: T3104
title: HLL 基数素描的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3103]
created: 2026-09-17
---

## Question

HllCardinalitySketch 合同（幂等/误差带/合并/畸形）怎么钉住？（spec 2001 / effort #2001 / R2）

## Resolution

**七用例全绿**（buzhou-core HllCardinalitySketchTest）：单元素精确=1 /
千次重复幂等 distinct=1 且 offeredCount=1000 / 50 元素近精确（LC 修正域）/
20k distinct 误差 ≤5%（3× 理论界宽容带）/ merge 并集 10k 误差 ≤5% /
寄存器数 2^b 幂表 + 误差界换算 / 畸形五型（precision 3、17、null offer、
跨精度 merge、null merge）fail-fast。
