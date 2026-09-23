# Spec 5010 — Hinted Handoff 暂代投递（effort #5010，S11）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6121–T6122，impl 2161）。
> 借鉴：Cassandra Hinted Handoff（节点宕机时他节点暂代 + 恢复回放）。

## Problem Statement

目标暂不可用的投递病：直接丢弃（恢复后数据缺口）或无休止
阻塞重试（拖死主路径）——**暂代记账 + 恢复回放面**缺失。

## Solution

`HintedHandoff`（core/recovery）：

- `route(target, payloadId)`：目标健康 → 正常投递（false——
  无 hint）；目标下线 → 记 hint 入队（true——主路径不阻塞）；
- `markDown/markUp`：下线/恢复状态机（重复下线/恢复 IAE
  fail-fast——状态转换显式）；
- `markUp` 返回按 FIFO 排队序的全部暂代 payloadId 并清队
 （恢复回放语义）；恢复后再下线则新 hint 独立累积；
- 读数：hintCountOf/isDown；未知目标 fail-fast。

## User Stories

1. 作为投递作者，目标宕机不丢不阻塞——恢复后按序回放。
2. 作为审计作者，同事件序列同回放清单（确定性可回放）。

## Testing Decisions

- 健康/下线路由分叉；下线累积 FIFO；markUp 清队回放；
  恢复后再下线独立累积；重复状态转换 IAE；未知目标
  fail-fast。

## Out of Scope

- 不做 hint 过期（max_hint_window 归保留族）；不做传输本身
 （本件是记账回放面）；不做多副本一致性对账。

## Further Notes

- 与持久化 Outbox（effort #6）同族不同面：事件外发持久队列
  vs 目标级暂代回放。Wave 3（治理族）开波。
- 里程碑：S11/50（22%）。
