# 16 — spill · context-clearing（已消费 tool_result → Handle + 显式逐出）

**What to build:** 上下文超阈值时旧 tool_result 自动降级为 Handle 占位（保最近 N 个完整），由 harness 自持、对所有模型生效；模型可主动逐出句柄、框架按引用计数 TTL 自动回收。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] ConversationPostProcessor：超阈值时把旧 tool_result 替换为 Handle 占位（"cleared; refetch via ReadRangeTool(evidence-id)"）保最近 N 个完整
- [ ] 跨 provider 由 harness 自持（不依赖厂商 server 侧 context editing）
- [ ] 显式逐出双路径：`EvictHandleTool`（模型主动）+ 引用计数 TTL（框架自动）
- [ ] cache 意识：在消息窗尾部边界批量清除、避免每 Turn 增量改写（断点策略）
- [ ] 与 hot-tail 分工明确（hot-tail=新结果何时溢出；clearing=旧 tool_result 何时降级句柄）
- [ ] 端到端：清除后模型仍可凭句柄回读原文；最近 N 完整
- [ ] spec 02（Spill）同步

> spec 12 §spill-16；[T44](../tickets/T44-spill-context-clearing.md)。源：Anthropic 注记（清旧 tool_result=最安全最轻的压缩；Claude API server 侧仅 Anthropic→harness 自持版）。
