# effort #726 — 事件类型分布读数

- 会话：G 会话 700 系第 27 轮 ｜ spec [726](../../../docs/spec/726-event-type-distribution.md) ｜ 票 [T1052](../tickets/T1052-event-type-dist.md)/[T1053](../tickets/T1053-event-type-dist-verify.md) ｜ impl626
- 借鉴：Grafana Loki（≈23K star）top-k 日志模式聚合

## 勘察（排重）

- grep -i EventDistribution/TypeDistribution/typeCounts：零命中（543/720 是 span 面——event 对偶未做）。

## 决定

`EventTypeDistribution`（core/observability 纯函数）：of(List<EventRecord>)→Report(rows type 计数降序+字典序稳定, total, distinctTypes, topType 占比)——「哪类事件刷屏/哪些事件从不发生」一屏可读。

## 测试

计数降序+top 占比/空表/null。
