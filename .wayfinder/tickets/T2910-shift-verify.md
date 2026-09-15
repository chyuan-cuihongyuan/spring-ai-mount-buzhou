---
id: T2910
title: 双窗口漂移检测的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2909]
created: 2026-09-16
---

## Question]

漂移判定在双向/双闸/零基线/畸形四面下正确吗？（spec 1854 / effort #1854 / R55）

## Resolution`

**WindowShiftDetectorTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=WindowShiftDetectorTest）：100→150 升漂/反向降漂；双闸缺一不可
（0.9<1 绝对不过 STABLE）；零基线退化（6≥5×1 漂）；空窗/负闸参/null
与 NaN 样本 fail-fast。首跑红为用例闸参过严（6<10×1），修正后绿。

