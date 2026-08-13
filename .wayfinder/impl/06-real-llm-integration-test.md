# 06 — 加入至少一条真实 LLM 行为的集成测试

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T5](../tickets/T5-real-llm-integration-test.md)

**What to build:** 至少一条贴近真实模型行为的端到端集成测试——**Mock 模拟真实行为进 CI**（默认跑）+ **真实 API 凭据门控仅本地**（CI 默认跳过）。覆盖一条最有价值的 core 链（多轮 + 工具 + 压缩 / crash-recovery 续跑 / 并行 fan-out 之一）。用录制回放 / 契约测试 / 黄金样本防脆，避免裸网络依赖。

**Blocked by:** **01**（依赖可解析）+ 决策票 **[T5](../tickets/T5-real-llm-integration-test.md)**（覆盖链 / 真实 vs Mock 边界 / 防脆策略须 prototype 拍板）。

**Status:** done (assignee: zcode)

- [x] T5 prototype 决策已落 —— 两者都要：真实仅本地 + Mock 进 CI；覆盖链 = 多轮+工具+压缩
- [x] Mock 变体进默认 `mvn verify`，CI 绿 —— `RealBehaviorIntegrationTest` ✓ `Tests run:1 Failures:0`
- [x] 真实 API 变体凭据门控，无 key 时 CI 跳过、不红 —— `RealLlmIntegrationTest` ✓ `Skipped:1`、`BUILD SUCCESS`
- [x] 覆盖所选 core 链的端到端真实行为（含工具调用循环在 Advisor 链内的协同）—— Mock 断言反应式模型决策工具→执行→回注→小结 + 微压缩 + evidence 回查

## Resolution

**双轨交付**（同一条 core 链）：

- **Mock 变体** `RealBehaviorIntegrationTest`（CI 绿）：**反应式** mock 模型——按输入*决策*是否调工具（见用户排查请求且无工具结果 → 发 `get_order_status`；收工具结果 → 文本小结），比脚本化 stub 更贴近真实 LLM 工具调用行为；端到端跑通工具调用循环（模型决策 → Harness 执行 → 结果回注 → 再决策）+ 微压缩 + evidence 回查，逐项断言。
- **真实 API 变体** `RealLlmIntegrationTest`（gated、CI 跳过）：`@EnabledIfEnvironmentVariable("BUZHOU_LLM_API_KEY")` + `@SpringBootTest`（经 `spring-ai-starter-model-openai` 自动装配 OpenAI 兼容 `ChatModel`，Buzhou 仍纯编程式装配）。弱断言（回复非空 + 消息持久 + 不抛异常），规避真实模型输出的不可预测性。

**防脆性**：Mock 确定性、无网络；真实凭据门控、弱断言、无精确文本匹配——避免裸网络依赖。

**验证（JDK 21 本机）**：Mock `Tests run:1 Failures:0`；真实 `Skipped:1`（无 key）`BUILD SUCCESS`。

**注**：examples/pom 加 test-scope `spring-boot-starter-test` + `spring-ai-starter-model-openai`（真实变体自动装配用；Mock 变体不需要但无害）。本 Windows 主机可编译+跑 Mock、真实 gated skip。
