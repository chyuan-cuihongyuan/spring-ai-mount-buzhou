---
id: T1826
title: R9 分支缺口批次 2 验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1825
created: 2026-09-15
---

## Question

R9 补测后：两靶点分支覆盖提升多少？observability 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量 + JaCoCo 复扫）：

1. **分支提升**：ObservabilitySessionState 46%→88%（42/6）；ObservableToolCallback 29%→86%（24/4）。
2. **模块全绿**：observability 83 用例 0 失败 0 错误（新增 17 用例）。
3. 主代码零变化；fake 行为对齐真实 recorder 语义（初始属性袋落 handle）。
4. 批次 3 候选（ObservabilityAdvisor 68 missed / MicrometerDualWriter 15）留 R10+。
