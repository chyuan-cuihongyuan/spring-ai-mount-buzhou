---
id: X10074
title: X 会话 37 FftIterative 迭代快速傅里叶 的验证裁决
type: task
status: closed
assignee: zcode-x
blocked-by: [X10073]
created: 2026-10-01
---

## Question

37 合同怎么逐一验绿？

## Resolution

验证通过：七测全绿（单位脉冲全频带 1+全 1 仅直流+8 点随机与 O(n²) DFT 交叉互证 1e-9+Parseval 能量守恒+线性性+确定性+fail-fast）。
