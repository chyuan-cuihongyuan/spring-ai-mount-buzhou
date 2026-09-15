---
id: T2936
title: 关键路径长度的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2935]
created: 2026-09-16
---

## Question]

最长链在串行/分支/退化/环/畸形五面下正确吗？（spec 1867 / effort #1867 / R68）

## Resolution`

**CriticalPathLengthTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=CriticalPathLengthTest）：链 10+20+30=60 终点 c；分支 110 走
a+b 终点 b；单任务 7/空 null/非连通取最大 50 终点 y；环（a↔b）与
端点 ghost fail-fast；空白 id/负时长/重复任务 fail-fast。

