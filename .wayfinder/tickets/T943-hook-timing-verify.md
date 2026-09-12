---
id: T943
title: hook 链耗时观测的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T942
created: 2026-09-13
---

## Question

计时真累计（count/total/max）？慢 hook WARN 只告一次？多 hook 互不串账？默认开零行为回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 47 轮）：① 快 hook + 慢 hook（Thread.sleep）混合链——slow hook 的 stats() count/total/max 均非零且 max ≥ sleep 时长，快 hook max 微小；② 多次调用 count 累计；③ HookResult 阻断（block）路径同样计时且不影响返回值语义（既有 HookChainTest 零回归）；④ stats() 快照不可变。`mvn -pl buzhou-core -am test` 全绿。
