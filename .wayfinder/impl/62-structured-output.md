# 62 — 结构化输出 chatForEntity（T87 决策落地）

**What to build:** AgentSession.chatForEntity default + DefaultAgentSession 实现（BeanOutputConverter schema 注入 + REASK 一次 + StructuredOutputException）+ ErrorCode 新增 + 事件。

**Blocked by:** None.

**Status:** done

- [ ] ErrorCode 增 STRUCTURED_OUTPUT_FAILED（NON_RETRYABLE）+ StructuredOutputException
- [ ] AgentSession default chatForEntity（抛 UOE）
- [ ] DefaultAgentSession：format 注入、解析、structured.reask 事件、REASK turn、两败抛异常
- [ ] 测试：合法 JSON 直接返回；首轮非法次轮合法（reask 事件 + 模型两次调用）；两轮非法抛异常（消息含摘要）；REASK 计入预算（runaway maxSteps=1 时 REASK 被闸）

## Done

验证：`./mvnw -pl buzhou-core clean test` 257/257 绿（新增 StructuredOutputEndToEndTest 4 用例：合法直返/REASK 恢复/两败抛异常/REASK 计入会话步数预算）；starter 5/5、examples 62/62 绿。
落地：AgentSession.chatForEntity default（UOE）+ DefaultAgentSession 实现（BeanOutputConverter.getFormat() 注入 + 解析失败 structured.reask 事件 + REASK 完整轮次复用 doChat 管线 + 两败 StructuredOutputException 含输出摘要）；ErrorCode 增 STRUCTURED_OUTPUT_FAILED（NON_RETRYABLE）+ StructuredOutputException；流式 M1 不做（文档明示）。
