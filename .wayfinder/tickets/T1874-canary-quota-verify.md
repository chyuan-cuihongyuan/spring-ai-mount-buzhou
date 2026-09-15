---
id: T1874
title: R34 金丝雀候选限流补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1873
created: 2026-09-15
---

## Question

R34 补测后：放行态与耗尽态两用例是否全绿？resilience 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，resilience 定向 + 全量）：

1. **定向全绿**：放行态（RPM=10 额度内双轮金丝雀照常直达备模型，主模型零调用）+ 耗尽态（RPM=1 时次轮 acquireOrThrow 抛 ModelRateLimitExceededException——全局限流窗语义实证）。
2. **模块全绿**：resilience 444+ 用例 0 失败 0 错误。
3. 主代码零变化。
