# effort #736 — span 父链完整性审计

- 会话：G 会话 700 系第 37 轮 ｜ spec [736](../../../docs/spec/736-span-parent-integrity.md) ｜ 票 [T1072](../tickets/T1072-span-parent-integrity.md)/[T1073](../tickets/T1073-span-parent-integrity-verify.md) ｜ impl636
- 借鉴：OpenTelemetry trace 树语义（parent 引用完整性）

## 勘察（排重）

- SpanRecord.parentSpanId 构成 trace 树——父被逐出（729 容量逐出）/未落库/跨 trace 串写 → 树断裂；grep -i parentIntegrity 零命中。

## 决定

`SpanParentIntegrityAudit.audit(List<SpanRecord>)` 纯函数——悬空父引用 Finding(spanId,parentSpanId)+totalSpans/rootSpans；无父=根合法。调用方供单会话/单 trace 集合。

## 测试

悬空父引用发现+根计数/空表 null。
