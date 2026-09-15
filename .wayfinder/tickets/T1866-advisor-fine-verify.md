---
id: T1866
title: R26 细粒度补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1865
created: 2026-09-15
---

## Question

R26 补测后：定向用例是否全绿？observability 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量）：

1. **定向全绿**：content=null chunk 无 NPE 且 chunk2 answer 照常聚合 FINAL_REPLY（修正：FINAL_REPLY 为 chunk2 合法产出非防御对象）；TRM 正例（ev-7/s-1/3 提取）+ 反例（无模式 → null/null）。
2. **模块全绿**：observability 153 用例 0 失败 0 错误（新增 2 用例）。
3. 主代码零变化。T1867 观察票开放（TRM getText 聚合语义待核）。
