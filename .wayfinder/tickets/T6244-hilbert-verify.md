---
id: T6244
title: T 会话 T22 Hilbert Curve 希尔伯特曲线的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6243]
created: 2026-09-26
---

## Question

T22 合同怎么逐一验绿？（spec 6021 / effort #6021 / T22）

## Resolution

**验证通过**：HilbertCurveTest 五测全绿——order4 全 256 格
双射穷举；端点锚（终点 (side−1,0) 变体锚）；order6 局部性
平均差<全域/4；order1 形状；fail-fast。
