---
id: T2296
title: 并行剪枝的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2295
created: 2026-09-15
---

## Question

M 会话第 25 轮：并行剪枝如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=EvalParallelPruneTest,EvalPruneTest,EvalRunnerCancelTest` 绿——
① 并行（workers=2）+ 恒 fail 评估器 + 剪枝策略：首波失败率达阈值 → 剩余项 pruned、评估器调数 < 全量；
② 未配剪枝的并行 run 零变化（既有并行测试零回归）；
③ 串行剪枝/取消既有用例零回归。
