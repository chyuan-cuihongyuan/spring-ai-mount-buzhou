---
id: T1838
title: R15 sink 分发补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1837
created: 2026-09-15
---

## Question

R15 补测后：dispatchToSinks 分支覆盖提升多少？observability 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量 + JaCoCo 复扫）：

1. **分支提升**：BaseSpanRecorder 85%（46/8，残余 = catch 内 WARN 日志分支）。
2. **模块全绿**：observability 123 用例 0 失败 0 错误（新增 5 用例）。
3. 主代码零变化。ObservabilityAdvisor 细粒度残余视 R16 复扫后定。
