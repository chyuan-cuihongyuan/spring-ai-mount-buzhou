---
id: T3022
title: 随机早期丢弃的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3021]
created: 2026-09-23
---

## Question)

丢弃曲线在三区/边界/畸形下正确吗？（spec 1910 / effort #1910 / R111）

## Resolution`

**RandomEarlyDropTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=RandomEarlyDropTest）：三区各一例（25→0/75→0.05/120→0.1）；
边界含下（恰 minTh 线性区起点/恰 maxTh 达 maxP）；畸形三型
fail-fast。
