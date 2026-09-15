# Spec 1848 — Misra-Gries 频项素描（effort #1848，R49）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2897–T2898，impl 1449）。借鉴：
> Misra-Gries（流式 heavy hitters 经典）——O(k) 内存单遍找超 N/k 的一切
> 频繁项，计数为下界（真实−估计 ≤ N/k），确定性无哈希。

## Problem Statement

会话/工具访问流的「真热点」要全量计数才能知道——内存随基数爆炸；采样
 统计又只给概率答案——「确定性频项检测 + 有界误差」的流式素描缺位。

## Solution

`MisraGriesSketch`（core/observability，静态纯函数）：

- `sketch(k, stream)`：k−1 个计数器，计满后新项全员减一（抵消一轮投票）、
  归零淘汰；幸存者即候选（至多 k−1 个）；
- 保证：一切计数 > N/k 的项必幸存；估计 ≤ 真实 ≤ 估计 + N/k（下界口径）；
- `isHeavyCandidate(item, sketch)` 候选查询。

## User Stories

1. 作为容量治理者，单遍扫会话流（k=10）出一切 >1/10 流量的会话——
   O(k) 内存不随会话基数爆炸。
2. 作为审计者，计数是下界不是精确值——真实流量 ≥ 读数，热点不漏报。
3. 作为框架宿主，确定性可回放（对照 Count-Min 的概率口径）。

## Implementation Decisions

- 纯函数零状态；LinkedHashMap 保插入序（幸存者序确定）。

## Testing Decisions

- 多数项幸存+下界区间；下界口径断言（估计 ≤ 真实、误差 ≤ N/k、频项
  必幸存二择一断言）；确定性与空流；畸形两型 fail-fast。

## Out of Scope

- 不做 Count-Min（概率口径归未来静脉）；不做 top-k 排序输出。

## Further Notes

- 与 RateLimitKeyHotspot 正交：那是既有热点读数，这是流式素描基建。
