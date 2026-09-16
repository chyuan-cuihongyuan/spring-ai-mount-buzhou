---
id: T3138
title: 检索强度重排接线的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3137]
created: 2026-09-17
---

## Question

RecallStrengthReranker 合同（融合序/两极退化/稳定/畸形）怎么钉住？（spec 2018 / effort #2018 / R19）

## Resolution

**八用例全绿**（buzhou-memory；首版测试 BuzhouMessage 构造签名错——
对齐 12 组件 canonical 后 8/8）：同相关度新热靠前 / 高相关低强度胜
（0.7×0.95=0.665 > 0.51 算术钉死）/ 纯强度序 / 纯相关度保原序 /
null 元数据垫底 / 同融合分稳定 / TIME 模式强度序 / 畸形三型
（null hits、1.5、−0.1）fail-fast。
