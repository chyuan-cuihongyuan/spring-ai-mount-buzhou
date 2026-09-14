---
id: T2262
title: EvalRunner 评估 run 协作式取消面的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2261
created: 2026-09-15
---

## Question

M 会话第 6 轮：取消面如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=EvalRunnerCancelTest,EvalPruneTest,EvalRunBudgetTest` 绿——
① 第 2 项评估器内触发 requestCancel()：前 2 项有真结果、剩余全 STATUS_CANCELLED、评估器恰被调 2 次（未启动项不再启动）；
② 不取消零行为变化（既有 Prune/Budget 测试不动全绿）；
③ run 开始清零：取消后的下一次 run 完整执行不受残留标记污染。
