---
id: T3180
title: SimHash 近重复指纹的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3179]
created: 2026-09-17
---

## Question

SimHashFingerprint 合同（确定性/近重复/对称/阈值/畸形）怎么钉住？（spec 2038 / effort #2038 / R39）

## Resolution

**八用例全绿**（首跑 1 红教训：6 词元 +2 增量实测距离 10——短文本
平局 bit 噪声（3:3 平局被新增打破），SimHash 固有；改确定性性质断言
后 8/8）：同文本同指纹 / **重复词元距离恰 0**（同向票加倍符号不变
数学性质）/ 小增量距离 < 不交文本距离一半 / 不交 >16（期望 32） /
汉明对称+64 上界+自距 0 / 阈值 0=精确匹配、差 1 bit 阈 1 判近 / 单
词元指纹非零 / 畸形四型（null/空清单/含 null 词元/负阈值）
fail-fast。
