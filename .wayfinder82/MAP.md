# Wayfinder Map — Buzhou 归档详情查询（effort #82，50 轮自迭代第 47 轮）

> effort #82，延续 #81（T427–T428 / impl-266）。主线：归档有清单（archived() 只
> 有 id）与在册数（健康段）——合规审计要「谁在何时归档、规模几何」（工单：某
> 用户会话归档了吗？多大规模？）。

## Destination

`SessionArchiver.archivedDetailed()`：ArchivedDetail(sessionId, archivedAt,
messageCount, stateCount) 列表（archivedAt 倒序）；损坏归档以 messageCount=-1
占位行（可见而非静默跳过）。

## Notes

- 借鉴：审计查询面惯例（合规工单零解析成本）。

## Decisions so far

- 损坏占位行（-1）不静默——修复走手工删（与 purge 跳过策略区分：读面可见、写面
  跳过）。

## Not yet specified

- 单会话详情详情（archivedDetail(sessionId)——列表过滤够用暂不单设）。

## Out of scope

- 沿用 #7–#81。

## Tickets

- [x] [T431 archivedDetailed + 占位行](tickets/T433-archive-detail.md)（impl-267）
- [x] [T432 红队（规模/倒序/损坏可见）+ 收口](tickets/T434-archive-detail-close.md)
