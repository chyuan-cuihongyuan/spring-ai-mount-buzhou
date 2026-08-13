# 06 — 加入至少一条真实 LLM 行为的集成测试

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T5](../tickets/T5-real-llm-integration-test.md)

**What to build:** 至少一条贴近真实模型行为的端到端集成测试——**Mock 模拟真实行为进 CI**（默认跑）+ **真实 API 凭据门控仅本地**（CI 默认跳过）。覆盖一条最有价值的 core 链（多轮 + 工具 + 压缩 / crash-recovery 续跑 / 并行 fan-out 之一）。用录制回放 / 契约测试 / 黄金样本防脆，避免裸网络依赖。

**Blocked by:** **01**（依赖可解析）+ 决策票 **[T5](../tickets/T5-real-llm-integration-test.md)**（覆盖链 / 真实 vs Mock 边界 / 防脆策略须 prototype 拍板）。

**Status:** ready-for-agent

- [ ] T5 prototype 决策已落（覆盖链 / 真实 vs Mock 边界 / 防脆方式）
- [ ] Mock 变体进默认 `mvn verify`，CI 绿
- [ ] 真实 API 变体凭据门控，无 key 时 CI 跳过、不红
- [ ] 覆盖所选 core 链的端到端真实行为（含工具调用循环在 Advisor 链内的协同）
