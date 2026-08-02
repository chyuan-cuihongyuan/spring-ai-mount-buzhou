# 18 — OTel 导出桥

**What to build:** buzhou-observe-otel 可选模块：四类 Span 映射为 OpenTelemetry span 导出（traceId 由 sessionId 派生），Event 映射为 span event/attribute；对接 Collector 验证；默认关，引入并开启即生效。

**Blocked by:** 11

**Status:** ready-for-agent

- [ ] OTLP 导出到本地 Collector 可见完整 trace 树
- [ ] Span 种类/属性/耗时映射规则与 03 spec 一致
- [ ] 模块缺省关闭，对主链路零开销
