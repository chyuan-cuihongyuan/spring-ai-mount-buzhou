---
id: T3206
title: 文本编辑距离的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3205]
created: 2026-09-17
---

## Question

TextDistance 合同（教材用例/对称/归一/阈值/畸形）怎么钉住？（spec 2052 / effort #2052 / R53）

## Resolution

**八用例一次全绿**（buzhou-core）：kitten/sitting=3、flaw/lawn=2、
intention/execution=5 教材钉死 / 全同 0 双空 0 单向空全插删 / 对称 /
aaa/bbb=3 相似比 0 / dist1÷len5=0.8 归一 / 键纠错近匹配+零阈全过
满阈全同 / 500 长串 dist=1 / 畸形五型（null ×3、阈 1.5、−0.1）
fail-fast。
