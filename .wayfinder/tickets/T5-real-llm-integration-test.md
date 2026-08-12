---
id: T5
title: 加入至少一个真实 LLM 行为的集成测试
type: prototype
status: open
assignee: ""
blocked-by: [T1]
created: 2026-08-13
---

## Question

- 真实 API（凭 key gating，CI 默认跳过）还是 **Mock 模拟真实行为**（用户原话允许）？或两者都要（真实仅本地、Mock 进 CI）？
- 覆盖哪条 core 链最有价值（多轮+工具+压缩 / crash-recovery 续跑 / 并行工具 fan-out）？
- 如何避免脆性与网络依赖——录制回放（wiremock）/ 契约测试 / 黄金样本？

## Context

- 这是「core 做深」的**测试深度**部分：当前测试多为单元/脚本化，缺一条贴近真实模型行为的端到端链。
- **blocked-by [T1](T1-ci-red-remotely-green-locally.md)**：测试要在依赖可解析的环境跑。
- 偏 prototype（HITL）：真实 LLM 的边界（成本/凭据/稳定性）需用户定。

## Resolution

<!-- prototype 后填写：策略决策 + 覆盖链 + 实现方式 -->
