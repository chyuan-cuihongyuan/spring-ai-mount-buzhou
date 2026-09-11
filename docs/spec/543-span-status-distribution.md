# Spec 543 — span 状态分布读数（effort #543）

> wayfinder map：`.wayfinder/maps/effort-543.md`（T841-842）。E 会话第 43 轮。

## Problem Statement

TOOL/TURN/MODEL span 的 kind×status 分布无读数面——「MODEL 错误集中
还是 TOOL 错误集中」靠翻 span 流。

## Solution

`observability.analytics.SpanStatusDistribution`（纯函数+store 重载）：
kind×status 计数（status 大小写归一、null/空→UNSET；TreeMap 输出稳定）；
null spans fail-fast。

## User Stories

1. 作为运维，我想一眼看到各 kind 的量级与错误分布， so 排障方向
   （模型面 vs 工具面）秒级判断。

## Implementation Decisions

- status 大小写归一（供应商差异容忍）；输出 TreeMap 稳定序。

## Testing Decisions

- 计数/归一/UNSET/store 重载/null fail-fast。

## Out of Scope

- 跨会话聚合；时间维。

## Further Notes

- 新公共类型 `SpanStatusDistribution` 随轮 regenerate 快照 +
  api-surface.md 加行。
