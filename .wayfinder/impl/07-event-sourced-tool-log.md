# 07 — core · 事件溯源工具调用日志 + 幂等键

**What to build:** 追加式工具调用日志让「exactly-once」变真话：崩溃恢复时已落盘 outcome 的调用按 id 短路不重跑；幂等键随调用传给工具端；恢复从 Completed-Turn 之后续跑、绝不重放 LLM。

**Blocked by:** 06（Run 注册表——日志是快照的前置证据层、同存储介质）

**Status:** done（2026-08-14：ToolCallLog SPI + InMemory/JDBC 双实现（COMPLETED 只记录一次）+ manager 结局记录（五态 outcome + argsHash + 64K 封顶）+ DanglingCallRepairer 优先日志回放（exactly-once、写型工具零重跑断言）；schema 三方言增 buzhou_tool_call_log 表；崩溃恢复端到端含「不重放 LLM」路径——恢复点=Completed-Turn 后）

- [ ] 追加式 `ToolCallLog`（turnId、toolCallId、请求指纹 argsHash、outcome）与 RunRegistry 同介质（InMemory+JDBC）
- [ ] restart/续跑时已落盘 outcome 的 toolCall 按 id 短路不重跑（可观测：跳过计数）
- [ ] 幂等键 = sessionId+turnId+toolCallId 随调用传给工具端（工具可透传下游去重）
- [ ] 恢复语义=最后 Completed-Turn 之后续跑、不重放 LLM（断言无重复模型调用）
- [ ] argsHash 指纹算法确定（归一化 JSON→sha256）
- [ ] 端到端：写型工具崩溃恢复后不重复执行
- [ ] spec（恢复章节）同步

> spec 12 §core-5；[T33](../tickets/T33-core-event-sourced-tool-log.md)。源：temporal 22,284★ + dapr 26,021★（Event History+幂等；不引入 engine）。
