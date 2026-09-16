---
id: T3198
title: 卡方均匀性检验的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3197]
created: 2026-09-17
---

## Question

ChiSquareUniformity 合同（均匀/偏斜/公式/临界/畸形）怎么钉住？（spec 2048 / effort #2048 / R49）

## Resolution

**七用例全绿**（首跑 1 错：临界值递增用例误传零总量数组——全 1 后
7/7）：完全均匀 χ²=0 自由度 3 不拒 / 97:1:1:1 拒绝 / 52/49/51/48 轻
波动远低临界 / 60:40 手算恰 4.0 超临界 3.841 公式钉死 / 临界随自由
度 4<10 递增 / 零观测桶合法 χ² 正 / 畸形五型（null、单桶、22 桶、
零总量、负频数）fail-fast。
