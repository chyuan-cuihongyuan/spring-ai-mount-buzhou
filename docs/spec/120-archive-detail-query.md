# Spec 120 — 归档详情查询（effort #82）

> wayfinder map：`.wayfinder/maps/effort-82.md`（T431–T432）。

## Problem Statement

归档面有 id 清单（archived()）与在册数（spec 102 健康段）——合规审计工单要
「某会话何时归档、消息/state 规模几何」，当前要 restore 或翻 JSON。

## Solution

`SessionArchiver.archivedDetailed()`：`ArchivedDetail(sessionId, archivedAt,
messageCount, stateCount)` 列表（archivedAt 倒序）；损坏归档以 `messageCount=-1`
占位行（读面可见而非静默跳过——与 purge 写面跳过策略区分）。

## User Stories

1. 作为合规审计员，我查归档规模零解析，工单回答零成本。

## Testing Decisions

- 规模/时间倒序断言；损坏归档占位行可见。

## Out of Scope

- 单会话详情方法；分页。

## Further Notes

- 归档查询三件套：archived()（id）/ archivedDetailed()（审计）/ health（计数）。
