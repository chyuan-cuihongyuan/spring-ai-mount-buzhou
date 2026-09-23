# Spec 5019 — 热点 Key 探测器（effort #5019，S20）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6139–T6140，impl 2170）。
> 借鉴：热点 Key 探测（确定性采样 + 阈值告警；Redis/网关热点面思想）。

## Problem Statement

热点 Key 的病：全量计数（内存随 key 基数爆炸）或拍脑袋
告警（阈值无观测依据）——**确定性采样 + 阈值告警面**缺失。

## Solution

`HotKeyDetector`（core/metrics）：

- 确定性采样：`record(key)` 每 `sampleRate` 次（全局序号
  `seq % sampleRate == sampleRate-1`）命中采样一次计数
  （mod 采样——确定性、无随机）；
- 告警：采样计数 ≥ `hotThreshold` 即热点（`isHot(key)`）；
- `hotKeys()`：达阈值 key 全集（TreeMap 字典序——确定性）；
- fail-fast：sampleRate≤0 / hotThreshold≤0 / null key。

## User Stories

1. 作为网关作者，热点 key 被采样观测并告警——缓存击穿
   可预警。
2. 作为审计作者，同访问序列同热点清单（确定性可回放）。

## Testing Decisions

- 采样确定性（rate=10 时恰好每 10 次命中一次计数）；阈值
  触发与未触发对照；hotKeys 字典序；畸形定构/null key
  fail-fast；同序列同热点集回放。

## Out of Scope

- 不做概率计数（Count-Min 素描 R2 已覆盖）；不做滑动窗
  采样（固定采样口径）；不做自动限流联动。

## Further Notes

- 与 CountMinSketch（R2 频次估计）互补：概率估计 vs 确定性
  采样告警。Wave 4 第二件。
- 里程碑：S20/50（40%）。
