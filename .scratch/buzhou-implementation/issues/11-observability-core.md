# 11 — 可观测采集

**What to build:** Span（Session/Turn/ModelCall/ToolCall+HarnessInternal）/Event（Thinking/FinalReply/ToolInput/ToolOutput/Error+开放枚举）模型与 ObservabilityStore（平铺 parent_id，含注入快照表）落地；advisor(+400)+ToolCallback 包装采集、显式上下文传递、并发归属不串味；思维链厂商适配表（reasoningContent/thinking/thinking_content/Anthropic 块/Google thoughts，OpenAI 降级标记）；异步批量落库无采样+关闭强制 flush；token/耗时 Span 属性+Micrometer 双写；每轮注入快照落库。

**Blocked by:** 04, 03

**Status:** ready-for-agent

- [ ] 一次含工具并行的会话产出完整 Span 树（归属正确）有断言
- [ ] 思维链按厂商 key 适配采集、OpenAI 官方降级为计数+标记
- [ ] 关闭会话强制 flush，事件不丢；队列背压不丢事件
- [ ] 注入快照可按轮次还原「模型实际所见」
