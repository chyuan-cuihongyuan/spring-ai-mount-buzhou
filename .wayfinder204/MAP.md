# Wayfinder Map — Buzhou 摘要溯源台账（effort #204，B 会话第 27 轮）

> B 会话第 27 轮。主题池「摘要溯源」：摘要折入后「这段结论来自哪些原始消息」
> 不可回查——审计/争议裁决缺证据链。借鉴 W&B artifact lineage（产物→源 lineage）。

## Destination

SummaryProvenance（memory 模块）+ SummaryProvenanceListener（实现
CompactionListener）：onCompacted 累积 folded 源消息 id → onSummaryFolded
落一条 lineage（generation/trigger/源 ids/count）；lineage(sessionId) 回查。
LRU 有界。

## Notes

- 号段：B=奇数 spec（本轮 171）；轮次 .wayfinder200+。
- 零管线侵入：挂 listener 即记账（spec 95 已有挂点）。
- 与 evidence 引用计数（spec 26）正交：那管 spill 证据存活，这管摘要出处。

## Decisions so far

- 源粒度 = 微压缩折叠的 messageIds（管线既有事实，零新增采集）。

## Not yet specified

- lineage 持久化（当前进程内）；导出 JSONL。

## Out of scope

- 沿用各轮；摘要内容级 diff；跨会话 lineage。

## Tickets

- [x] [T537 SummaryProvenance 台账 + listener 累积落账](tickets/T537-provenance.md)（impl-299）
- [x] [T538 溯源回归（累积/落账/回查/有界）](tickets/T538-provenance-tests.md)（impl-299）
