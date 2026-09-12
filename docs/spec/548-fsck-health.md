# Spec 548 — fsck 巡检健康面（effort #548）

> wayfinder map：`.wayfinder/maps/effort-548.md`（T855-856）。E 会话第 48 轮。

## Problem Statement

538 巡检的 findings 只在 WARN 日志+计数——ops 标准健康读数面缺失
（/actuator 读不到巡检次数/findings）。

## Solution

`health.StoreFsckHealth implements BuzhouHealth`（mechanism=store-fsck，
观测面恒 UP）：details = runs/totalFindings/lastFindings（-1=尚未巡检）/
skippedNotLeader。装配随 fsck.enabled（538 同条件）。

## User Stories

1. 作为运维，我想从健康端点读巡检次数与 findings， so 数据衰变趋势
   无需翻日志。

## Implementation Decisions

- DOWN 语义不用于 findings（数据需关注非进程故障——分层诚实）。

## Testing Decisions

- 恒 UP；details 反映巡检读数；未巡检 lastFindings=-1；null fail-fast。

## Out of Scope

- DOWN 语义。

## Further Notes

- 新公共类型 `StoreFsckHealth` 随轮 regenerate 快照 + api-surface.md 加行。
