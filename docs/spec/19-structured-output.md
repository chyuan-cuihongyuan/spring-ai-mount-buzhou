# Spec 19 — 结构化输出（mechanism）

> effort #5（T87 / impl-62）。借鉴 LangChain withStructuredOutput / Spring AI entity()——语义借鉴、
> 直接复用 Spring AI `BeanOutputConverter` 做 schema 生成与解析。

## API 与语义

- **`AgentSession.chatForEntity(String input, Class<T> type)`**（default 抛 UnsupportedOperationException；
  DefaultAgentSession 实现）。流式 M1 不做（JSON 增量解析语义不明）。
- **schema 注入**：`new BeanOutputConverter<>(type).getFormat()` 追加到用户输入后（与 Spring AI
  entity() 同手法）。
- **REASK 一次**：首轮回复解析失败（convert 抛异常 / 返回 null）→ 发 `structured.reask` 事件
  （SessionEvent 通道，含解析错误摘要）→ 以「解析错误反馈 + format 重申」**追加一次完整 turn**
  （复用 doChat 全管线：hook / memory / observability / 预算闸 / 失控检测对 REASK 轮全部生效，
  诚实计入步数与 token）→ 再失败抛 `StructuredOutputException`（ErrorCode `STRUCTURED_OUTPUT_FAILED`，
  NON_RETRYABLE，携带两轮原始输出摘要）。
- 事件：`structured.reask`；无新指标（REASK 频率由事件流可统计）。
