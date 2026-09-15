# Spec 1711 — 轮间到达间隔读面（effort #1711，R12）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2623–T2624，impl 1311）。借鉴：
> 产品交互节奏遥测——轮间隔刻画用户节奏（机器连打 vs 人工思考）。

## Problem Statement

空闲判定（IdleSessionMonitor）与采样（TurnSamplerHook）都需要「用户节奏」
作依据，但节奏本身无读数：中位轮间隔 2s（脚本压测）与 40s（人工深思）
的会话被同一套空闲阈值/采样率对待。

## Solution

`TurnInterArrivalStats`（core/session，静态纯函数）：`analyze(turnEpochMillis)`
→ `InterArrivalReport(turns/intervalsMillis/medianMillis/p95Millis)`。
中位（偶数取中间均值）；p95 最近秩法；<2 轮哨兵 −1；输入应单调递增
（非单调由调用方负责——诚实边界）。

## User Stories

1. 作为采样调参者，p95 轮间隔 3s 的机器流量会话 → 提高采样率（信息密度低）。
2. 作为空闲调参者，人工会话中位间隔 40s → 空闲阈值不应低于该量级。

## Implementation Decisions

- 纯读面；间隔副本不可变；最近秩 p95（与 TurnLatencyPercentiles 族口径一致）。

## Testing Decisions

- 六轮间隔账目 + 中位/p95；偶数中位取均值；<2 与 null 哨兵。

## Out of Scope

- 不做直方桶（年龄/空闲两直方已占位）；不联动改采样行为。

## Further Notes

- 会话时间三维收齐：年龄（1707）+ 空闲（既有）+ 轮间节奏（本轮）。
