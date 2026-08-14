---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

结构化输出怎么做？现状：全仓无 EntityResponse/BeanOutputConverter 集成，agent 无法声明类型化输出；工具入参校验已有（ToolArgsValidator+REASK）。借鉴：LangChain withStructuredOutput、Spring AI 自带 entity() API。决策点：API 形态（AgentSession.chatForEntity(Class,schema)？流式？）、校验失败 REASK 一次再降级异常的语义、与 hook 链/observability 的关系（结构化输出算 event？）、schema 注入方式（Spring AI BeanOutputConverter format）。产出 spec 19 + impl 62。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **API**：`AgentSession.chatForEntity(String input, Class<T> type)`（default 抛 UnsupportedOperationException，DefaultAgentSession 实现）；流式 M1 不做（JSON 增量解析语义不明，文档明示）。
2. **schema 注入**：Spring AI `BeanOutputConverter<T>`（`converter.getFormat()` 追加到用户输入后——与 Spring AI entity() 同手法，不重复造 schema 生成）。
3. **REASK 一次语义**：首轮回复解析失败（convert 抛异常/返回 null）→ 发 `structured.reask` 事件（含解析错误摘要，走 SessionEvent 通道）→ 以「解析错误反馈 + format 重申」追加一次完整 turn（复用 doChat 全管线——hook/memory/预算/失控检测全走，REASK 消耗一次正常轮次预算）→ 再失败抛 `StructuredOutputException`（ErrorCode 新增 STRUCTURED_OUTPUT_FAILED，NON_RETRYABLE）。
4. **两轮均走既有管线**：不旁路——beforeTurn/afterTurn/observability/预算闸对 REASK 轮全部生效（诚实计入步数与 token）。
5. **事件**：`structured.reask`（SessionEvent）+ 失败异常即终态；无新指标（REASK 频率可由事件流统计）。
