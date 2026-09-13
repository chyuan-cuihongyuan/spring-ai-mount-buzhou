---
id: T1802
title: R1 补测验证——零覆盖清零与模块级绿
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1801
created: 2026-09-14
---

## Question

R1 补测后：6 模块零覆盖靶点是否全部清零（JaCoCo 复扫）？各模块测试是否绿？豁免残留是否与台账一致？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-14，core `mvn verify` 全量 + JaCoCo site 复扫 + guard/spill/resilience/mcp 全量 verify）：

1. **core 靶点 14/14 全部脱离零覆盖**：TableContextWindowResolver / SessionInterrupts / ToolSetSpec / EventType / RunStateTrackerHook / EmbeddingProvider / CompositeBuzhouMetrics / SessionStateHandle / OnFail / SessionStateStore 十类完全清零（miss=0）；Spotlighting（残 1）/ ConfigMaps（残 2）/ CompositeAttachmentRenderer（残 1）/ RecoverySupport（残 1）为防御分支边缘行，零覆盖状态解除。
2. **剩余零覆盖恰为台账豁免 2 项**：AgentSession（接口 default 面，装配期）/ BuzhouCoreAutoConfiguration$SmartLifecycle 匿名类（miss=9）——与 T1801 裁决一致，无未入档残留。
3. **模块级全绿**：core verify exit 0（含 JaCoCo ≥70% 门）；guard / spill / resilience / mcp 四模块全量 verify 绿（mcp 含真实 PG Testcontainers 回归，Docker 在场全跑）。
4. **超额收获**：补测显形三真实缺陷并单列修复——T1803（guard 禁用启动崩溃）/ T1804（mcp CLOB 方言）/ T1805（ToolDenialLog 排序被 Map.copyOf 破坏的主干既有红）。
5. 新增用例 44 个（core 167 断言面 + 四模块 runner/H2/PG 面），全部绿。
