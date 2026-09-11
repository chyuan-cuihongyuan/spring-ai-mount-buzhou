# Wayfinder Map — Buzhou 工具调用图谱统计（effort #519，E 会话第 20 轮）

> E 会话第 20 轮。勘察：观测面有 TOOL span（412 桶聚合只计数不分序）——
> **调用序**信息（同轮内工具先后链 a→b→c）与 per-tool 成败分布无读数面：
> 「哪些工具总被连着用」「哪个工具错误集中」是 prompt 工程/熔断配置的
> 直接依据。LangSmith trace analytics 思想。

## Destination

`observability.analytics.ToolGraphAnalyzer`（纯函数——传入 spans）：
过滤 kind=TOOL → 按 (sessionId, turnSeq, startedAt) 排序 → 同轮相邻对
生成有向边 toolA→toolB 计数；per-tool totals（calls/errors——status 含
ERROR 判红）+错误率。`ToolGraphReport(edges 降序, tools 降序)`。便捷
重载 `analyze(ObservabilityStore, sessionId)`（spansOfSession 单会话读）。
诚实边界：跨轮不连边（会话内时序链不做跨轮假设）；只在单会话/给定
spans 集内统计（跨会话聚合归 OLAP JSONL 下游）。

## Notes

- 号段：spec 519 / T789–T790 / impl-422。
- 借鉴源：LangSmith trace analytics（调用链统计）。
- 归 observability 模块（ObservabilityStore 访问在本模块——星形合规）。

## Out of scope

- 跨会话/跨实例聚合；JSONL 导出（60/67 族可扩散）；图可视化。

## Tickets

- [x] [T789 边提取与工具汇总](../tickets/T789-tool-graph-analyzer.md)
- [x] [T790 store 便捷重载](../tickets/T790-tool-graph-store.md)
