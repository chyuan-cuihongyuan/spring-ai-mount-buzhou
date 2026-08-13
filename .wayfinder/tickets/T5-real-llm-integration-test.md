---
id: T5
title: 加入至少一个真实 LLM 行为的集成测试
type: prototype
status: closed
assignee: zcode
blocked-by: [T1]
created: 2026-08-13
---

## Question

- 真实 API（凭 key gating，CI 默认跳过）还是 **Mock 模拟真实行为**（用户原话允许）？或两者都要（真实仅本地、Mock 进 CI）？
- 覆盖哪条 core 链最有价值（多轮+工具+压缩 / crash-recovery 续跑 / 并行工具 fan-out）？
- 如何避免脆性与网络依赖——录制回放（wiremock）/ 契约测试 / 黄金样本？

## Context

- 这是「core 做深」的**测试深度**部分：当前测试多为单元/脚本化，缺一条贴近真实模型行为的端到端链。
- 依赖前提：[T1](T1-ci-red-remotely-green-locally.md)（已 closed）确证依赖从 Central 正常解析（CI 红是无关 OS 缺陷，见 [T10](T10-fix-ci-os-specific-defect.md)）；CI-badge 绿由 T10 追踪、不阻塞本 ticket 策略决策（本地可验证）。
- 偏 prototype（HITL）：真实 LLM 的边界（成本/凭据/稳定性）需用户定。

## Resolution

**决策（两者都要：真实仅本地 + Mock 进 CI）**：
- **覆盖链**：多轮 + 工具 + 渐进式压缩（最有代表性、与 demo 同链）。
- **真实 vs Mock 边界**：Mock 变体（反应式 mock，按输入决策工具调用）进默认 `mvn verify`、CI 绿、无 key；真实 API 变体（OpenAI 兼容）`@EnabledIfEnvironmentVariable("BUZHOU_LLM_API_KEY")` 凭据门控、CI 跳过、仅本地带 key 跑。二者覆盖<b>同一条 core 链</b>。
- **防脆性**：Mock = 确定性反应式（无网络）；真实 = 弱断言（回复非空 + 消息持久 + 不抛异常，不做精确文本匹配）+ 凭据门控，避免裸网络依赖。

**实现**：`examples/src/test/.../demo/RealBehaviorIntegrationTest`（Mock，反应式模型驱动工具调用循环 + 微压缩 + evidence 回查，已断言）+ `RealLlmIntegrationTest`（真实，`@SpringBootTest` 经 `spring-ai-starter-model-openai` 自动装配 ChatModel、Buzhou 纯编程式装配）。

**验证（JDK 21 本机）**：Mock `Tests run:1 Failures:0`；真实 `Skipped:1`（无 key）、`BUILD SUCCESS`（不红）。

**实现切片**：[impl/06](../impl/06-real-llm-integration-test.md)。
