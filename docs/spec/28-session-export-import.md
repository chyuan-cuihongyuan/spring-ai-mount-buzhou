# Spec 28 — 会话可移植导出/导入（SessionExport）

> effort #6（T107 / impl-82）。灰度切流/灾难恢复/bug 复现包的备份能力；
> 对齐 LangGraph checkpoint 可移植语义（轻量版：数据面移植，不含执行态）。

## Problem Statement

会话数据困在单环境 store 内：跨环境迁移（灰度切流）、备份恢复（灾难恢复）、
问题会话打包给开发者复现（bug report）均无标准载体。仅有摘要导出（T90）不够。

## Solution

`AgentRuntime.exportSession(sessionId)` → `SessionExport`（format/version 单 JSON 文档：
messages 全量 + 最新 Summary + State entries + spill 引用清单派生）；
`importSession(export, keepIds)` 默认新 sessionId 重映射（引用一致重写），keepIds 冲突
fail-fast。导入是数据恢复（不建租约），后续以该 Id spawn 续用。

## User Stories

1. As a 平台运维, I want 整会话导出为单 JSON, so that 跨环境迁移与备份有标准载体。
2. As a 平台运维, I want 导入默认重映射新 Id, so that 跨环境 Id 撞车不可能发生。
3. As a 平台运维, I want keepIds 冲突 fail-fast, so that 已有数据绝不被静默覆盖。
4. As a 应用开发者, I want 问题会话打包给支持方, so that bug 可离线复现。
5. As a SRE, I want 导入的会话可直接 spawn 续聊, so that 恢复后业务连续。

## Implementation Decisions

- **文档结构**：`format="buzhou.session-export"` / `version=1`；时间 epoch millis
  （不假定 jackson-jsr310）；DTO 形态 Jackson 直转（MessageDto/SummaryDto/StateDto）。
- **导出面**：core 三槽（messages/summary-latest/state）+ spill 引用清单派生
  （metadata.spillUri → {evidenceId, spillUri}）；appId/agentName 尽力携带
  （活跃会话可查，历史会话 null——导入不依赖）。
- **导入语义**：默认 UUID 重映射（消息 sessionId + summary sessionId + state 键空间
  一致重写）；keepIds 目标有消息 → `SessionImportException` fail-fast；不建租约
  （数据恢复语义）；指标 `buzhou.session.imports` + INFO 日志。
- **验证**：格式/版本不符 fail-fast；空消息文档拒绝；空源（无消息会话）导出拒绝。
- **API 位置**：`AgentRuntime` default UOE + DefaultAgentRuntime 实现（会话级生命周期
  操作归 runtime）。
- **边界（诚实声明）**：spill 证据内容不内嵌（引用随消息 metadata 导出，内容由 spill
  侧运维另行导出；悬垂读路径由 spec 26 EVIDENCE_GONE 容错）；facts 属 memory 模块
  内部存储不在导出面（fog）；observability 记录不导出（审计随源环境留存）。

## Testing Decisions

- e2e（ScriptedChatModel）：①导出→JSON→导入全保真（消息/摘要/state + 重映射后 Id 一致）
  + 导入 Id spawn 续聊历史注入；②跨环境 keepIds 落位 + 同环境冲突 fail-fast；
  ③空源/空档/格式不符拒绝；④spill 引用清单派生 + JSON 往返 metadata 保真。
- 先例：`SessionForkEndToEndTest`。

## Out of Scope

- spill 证据内容内嵌（体积与配额双杀；运维路径已记载）。
- facts/observability 导出（fog：memory 侧导出扩展）。
- 导出文档加密（传输安全由运维通道负责）。

## Further Notes

- 与 fork（spec 20/26）正交：fork 是运行时分支（同 store），导出/导入是跨 store 移植。
- runbook §4（升级回滚）引用本能力作会话级备份手段。
