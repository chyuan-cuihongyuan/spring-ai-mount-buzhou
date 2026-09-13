# effort #732 — 事件 payload 大小审计

- 会话：G 会话 700 系第 33 轮 ｜ spec [732](../../../docs/spec/732-event-payload-size-audit.md) ｜ 票 [T1064](../tickets/T1064-event-payload-audit.md)/[T1065](../tickets/T1065-event-payload-audit-verify.md) ｜ impl632
- 借鉴：Sentry payload 限额/Loki 体量治理

## 勘察（排重）
- grep -i payloadSize/PayloadAudit：零命中（webhook max-payload 是外发限幅非审计）。

## 决定
`EventPayloadSizeAudit.analyze(List<EventRecord>)` 纯函数：payload Jackson 序列化字节按类型聚合（count/total/max 降序）+totalBytes/serialized/skipped。
## 测试
两类型降序+max>total/2+合计一致/空表 null。
