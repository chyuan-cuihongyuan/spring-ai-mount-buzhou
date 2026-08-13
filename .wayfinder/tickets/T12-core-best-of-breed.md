---
id: T12
title: core 对标开源最优——错误回喂/有界 turn/事件溯源 等 best-of-breed 落地
type: task
status: open
assignee: ""
blocked-by: [T11]
created: 2026-08-13
---

## Question

把 [T11](T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md` §1 §5 里 **core** 的 best-of-breed 思想择优写入 Buzhou。T3 闭合于「SPEC 判据满足」；本票把杆抬到「对标开源最优」。落地项（按 ROI / 依赖排序）：

### Tier 1（廉价，先做）
- **错误回喂模型**（OpenAI Agents SDK：`tool_not_found_behavior='return_error_to_model'` + `tool_error_formatter`）：工具异常→合成 `ToolResponseMessage`（错误文案 + 原 args）入历史、递归继续，而非整轮死。落点：`buzhou-core/.../exec/HarnessToolCallingManager.java`。
- **有界 turn 预算 + 可组合停止条件**（Vercel `stopWhen` / OpenAI `max_turns` / AutoGen 可组合终止条件 `&`/`|`）：每 Turn 限 think→tool 递归数；超限走可插拔 handler（优雅收尾）。建模为 `Predicate<TurnContext>` 链。落点：`DefaultAgentSession` / `DefaultAgentRuntime`。
- **结构化 span**（OpenAI typed spans）：每个 think→tool 批次与每个并行工具发 span，打 `toolCallId/turnId/sessionId`。落点：`SpanRecorder`（已有基建，补 tag）。

### Tier 2（中等）
- 结构化输出/工具参数 schema 校验重试（Pydantic AI `ModelRetry` + per-turn 预算）。
- **持久 run 注册表 + 枚举续跑**（Mastra `listActiveWorkflowRuns`/`restartAllActiveWorkflowRuns`）：把现有「悬空调用 reactive 修复」升级为「proactive 恢复」。**单笔最高价值中等投入。**
- `FakeChatModel` + record/replay fixture（Vercel `MockLanguageModelV4` / Pydantic `TestModel`）：确定性单测并行/修复/turn 语义。
- 显式 `CancelMode`（立即/当前工具后/当前 turn 后）+ token 贯穿（AutoGen）。

### Tier 3（冲极致）
- **事件溯源工具调用日志 + 幂等键**（Temporal/Restate 金标准）：append-only `(turnId,toolCallId,argsHash,status,result)`；replay 走缓存、在途调用按幂等键去重续跑。**唯一让 crash-safe+exactly-once 成真的路径**。
- 事务性并行批（LangGraph superstep）：整批成功才提交历史。
- time-travel / fork（以 Completed-Turn 为检查点）。
- HITL `interrupt()`/`Command(resume)` 按 `toolCallId` 匹配（避 LangGraph「resume 从头重跑」反模式）。

## Context

- **已领先（勿重做）**：并行工具+虚拟线程结构化关停（JVM best-in-class）、悬空调用 reactive 修复（唯一 agent harness 做）、Session/Turn/Completed-Turn 语义。
- 反模式（勿踩）：checkpoint≠durable（需事件日志+幂等键）；`interrupt()` 在 `while True`；工具异常上抛杀整轮；并发恢复无分布式锁；代码突变走 JSON schema 降质。
- 详源见 `docs/research/oss-best-of-breed.md` §1 与 §6（Diagrid 批判 / LangGraph / OpenAI / AutoGen / Pydantic / Vercel / Mastra / Aider）。

## Resolution
<!-- 实现后填：落地了哪些 Tier、落点文件、测试证据；未做的留作后续 -->

## Resolution（Tier-1 部分，2026-08-13）

Tier-1 两项已落地并闭合于细化票：[错误回喂模型](T16-core-tool-error-feedback.md)（`ToolErrorFeedback` 统一「错误即反馈」通道）、[有界 turn + 可组合停止条件](T17-core-bounded-turn.md)（`TurnLoopPolicy` + `BoundedToolCallingAdvisor`，默认 40 轮）。结构化 span（Tier-1 第 3 项）未做——`SpanRecorder` tag 增强留后续。Tier-2/3 清单继续由本票追踪。
