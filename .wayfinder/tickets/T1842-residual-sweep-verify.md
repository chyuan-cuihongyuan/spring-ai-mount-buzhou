---
id: T1842
title: R17 残余清扫验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1841
created: 2026-09-15
---

## Question

R17 补测后：ObservabilityAdvisor 残余 missed 收敛多少？observability 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量 + JaCoCo 复扫）：

1. **残余收敛**：ObservabilityAdvisor 75%→77%（累计口径；recordModelCallOutcome 22→14、accumulateStreamChunk 8→未列前八）。
2. **模块全绿**：observability 141 用例 0 失败 0 错误（新增 5 用例）。
3. **防御性不可达入档**：ChatResponse(List, null) 将 metadata 归一为 NULL 常量（usage 0/0/0）——recordModelCallOutcome 的 metadata-null 分支经公共构造器不可达，断言按实际归一口径锁定。
4. 主代码零变化。firstMatch/ToolResponseMessage 分支记录豁免。
