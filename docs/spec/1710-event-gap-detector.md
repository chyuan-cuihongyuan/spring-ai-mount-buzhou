# Spec 1710 — 会话事件时间间隙检测（effort #1710，R11）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2621–T2622，impl 1310）。借鉴：
> Flink event-time gap——事件流的时间间隙是「卡住/断流」第一信号。

## Problem Statement

TurnSequenceAudit 管序号连续性（乱序/跳号），但**时间维**无检测：事件间
隔超长（模型挂起/管道堵塞/宿主断连）不留痕——事后只有「最后一事件时间」
孤立点，无法回答「流在哪一段断过、断多久」。

## Solution

`EventGapDetector`（core/session，静态纯函数）：`analyze(eventEpochMillis,
thresholdMillis)` → `GapReport(events/thresholdMillis/gapCount/
largestGapMillis)`。间隙 = 相邻差 > 阈值（严格大于——恰等不算）；<2 事件
哨兵 largest=−1；绝对值取差（容忍乱序输入不炸）。

## User Stories

1. 作为平台运维，gapCount=1 + largestGap=45s → 事件流断过一次 45 秒，定位挂起。
2. 作为审计者，threshold 由调用方声明（不同会话类型容忍不同）。

## Implementation Decisions

- 纯读面；不动事件主路径；largest 取绝对差以在乱序数据下仍给可用信号。

## Testing Decisions

- 无间隙/两间隙计数 + 最大间隙；阈值恰等不计；<2 与 null 哨兵。

## Out of Scope

- 不做告警联动/不缓冲事件；不做乱序重排（序维归 TurnSequenceAudit）。

## Further Notes

- 序（TurnSequenceAudit）+ 时（本轮）= 事件流双维卫生。
