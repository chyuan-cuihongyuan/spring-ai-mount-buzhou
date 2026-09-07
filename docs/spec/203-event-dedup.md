# Spec 203 — 事件去重抑制（effort #220）

> wayfinder map：`.wayfinder/maps/effort-220.md`（T575–T576）。发射方防线——重复
> 事件不出门，接收方幂等键从「必须」变「双保险」。

## Problem Statement

同一事件被发射两次的路径不少见：hook 重试重入、宿主代码双发、advisor 链
同点多次触发——webhook 接收方靠幂等键去重（spec 20），但重复流量本身占
outbox 容量、占投递带宽、污染 lag 统计（135）。发射侧拦在门口是最省的层。

## Solution

`EventDeduplicator`（core/webhook，`SessionEventListener` 装饰器）：

- **指纹**：`sha256(type + 规范化 payload JSON)`——完全相同才同指纹。
- **抑制**：环形指纹集（默认 1024，构造可配）——重复即丢弃 +
  `buzhou.event.deduped` 计数；首见透传被装饰 listener 并入环。
- **环形滚出**：最老指纹滚出后同事件可再过（时间窗语义留档——环形即
  「最近 N 个事件」窗）。
- 与 fanout（151）组合：fanout 前包一层 = 全站入站去重。

## User Stories

1. 作为接收方，重复在发射侧被拦——幂等键从必须防线变双保险。
2. 作为运维，deduped 计数即「发射侧重复率」——宿主双发 bug 的显影剂。
3. 作为宿主，正常事件流零影响（首见透传零延迟）。

## Implementation Decisions

- payload 规范化：键排序 JSON（Map 序不定的等价 payload 同指纹——不误放行）。

## Testing Decimals

- 重复拦（计数）；同事件隔不同事件后仍拦；异质透传；环形滚出后再过；
  与被装饰 listener 组合透传；容量配置校验。

## Out of Scope

- TTL 时间窗；语义相似；跨实例。

## Further Notes

- 去重防线双面：发射方（本轮）+ 接收方幂等键（20）。
