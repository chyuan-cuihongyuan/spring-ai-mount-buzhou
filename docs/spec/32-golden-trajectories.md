# Spec 32 — 黄金轨迹回归评估（EventSequenceAssert）

> effort #6（T111 / impl-86）。LangSmith evals / OpenHands runtime evals 的本地零依赖版：
> 「脚本化输入 → 事件序列断言」的机制行为回归防线。

## Problem Statement

examples 有零散端到端用例，但断言的是「最终返回值」——机制**过程行为**（事件序列：先跳闸
后降级、拦截后不再有模型调用等）无系统性断言面。机制改动破坏行为不变性时只能靠人工翻日志。

## Solution

`EventSequenceAssert`（core test-jar testsupport，与 ScriptedChatModel 同发布）：
收集 SessionEvent 流，断言类型子序列（containsInOrder）、间隔约束（neverAfter /
followedBy）、计数（assertCount）、payload 谓词（assertPayload）；`attach(session)` 会话面
+ `attachGlobal(runtime)` 全局面（forked 等发往分支/全局通道的事件）。
examples `golden/GoldenTrajectoryTest` 六条黄金轨迹，进 ci.yml 常规跑。

## User Stories

1. As a 机制维护者, I want 机制行为有事件序列断言, so that 重构破坏行为不变性时 CI 挡下。
2. As a 贡献者, I want 断言器与脚本模型同处 testsupport, so that 各模块复用同一套断言词汇。
3. As a SRE, I want 黄金集失败语义可读（实际序列打印）, so that 排障直击差异点。

## Implementation Decisions

- **六条黄金轨迹（v1）**：G1 降级链（retry-exhausted → fallback.switched → 其后无新切换）；
  G2 预算闸（tokens-accumulated → token-hard-stop → 其后无新累计）；G3 日配额
  （quota.exceeded 恰一次）；G4 熔断恢复（state-changed ≥2：跳闸→探测成功回 CLOSED；
  min-calls=1——熔断样本按**逻辑调用**计，非重试次数）；G5 REASK（structured.reask 恰一次）；
  G6 fork（session.forked 恰一次，经全局通道——事件发往**分支**会话）。
- **压缩轨迹不进 v1**：微压缩无会话级事件面（指标口径），其行为回归由 buzhou-memory
  既有测试承载（fog：memory 侧事件化后再入集）。
- **JSON 用例 runner 不做**：脚本化响应用 Java 构造最直接，JSON DSL 是伪需求。
- **CI 接入**：examples 常规测试集（回归性质快且该挡）；与红队（对抗输入）/perf（时延）
  三者互不重叠。

## Testing Decisions

- 断言器自身经六条轨迹实战校验（失败消息含实际序列，可读排障）。
- 轨迹全部 ScriptedChatModel 驱动（零真实模型依赖），全链路经完整会话管线。

## Out of Scope

- LLM-as-judge / 语义质量评估（保持可选方法论，不进回归集）。
- 压缩/观测轨迹事件化（fog）。

## Further Notes

- G4/G6 的实现期发现（熔断样本按逻辑调用计、forked 发往分支通道）已回写本 spec——
  黄金集本身即机制语义的活文档。
