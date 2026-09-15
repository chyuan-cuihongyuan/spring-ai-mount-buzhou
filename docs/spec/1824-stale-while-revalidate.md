# Spec 1824 — Stale-While-Revalidate 策略（effort #1824，R25）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2849–T2850，impl 1425）。借鉴：
> HTTP Cache-Control `stale-while-revalidate` / CDN——陈旧窗内回旧值+异步
> 后台刷新，尾延迟换轻微陈旧性的显式交易。

## Problem Statement

缓存 TTL 二值（新鲜/过期）逼出两难：过期即同步回源（调用方吃满回源
延迟），或放宽 TTL（拿到更旧的数据还不自知）——「回旧值但立刻异步刷新」
的中间态没有判面。

## Solution

`StaleWhileRevalidatePolicy`（buzhou-resilience/cache，静态纯函数）：

- `serving(age, freshMillis, staleWindowMillis)` → 三态 `FRESH / STALE /
  EXPIRED`：达 fresh 进陈旧、达总寿过期（边界归属显式）；
- `staleness(...)` 陈旧度读数：0=新鲜（钳零不与哨兵撞值）、(0,1)=窗内
  进度、≥1=过期；fresh=0 时 -1 哨兵（无新鲜窗语义不成立）；
- 零陈旧窗退化为纯 TTL（兼容现状语义）。

## User Stories

1. 作为读路径优化者，age=fresh+200ms → STALE：调用方 5ms 拿旧值，后台
   异步刷新——p99 不吃回源延迟。
2. 作为数据治理者，staleness=0.9 → 旧值快出窗了，刷新该提速。
3. 作为框架宿主，纯判态零状态，与 TTL 缓存正交可叠加。

## Implementation Decisions

- 纯判态不执行（刷新动作归宿主）；初版新鲜期返回负陈旧度与哨兵撞值，
  改钳 0 消歧义（设计期自查修正入档）。
- fail-fast：任一负值入参。

## Testing Decisions

- 三态+双边界；陈旧度三段+哨兵；零窗退化纯 TTL；畸形三型 fail-fast。

## Out of Scope

- 不执行异步刷新；不接 ResponseCacheAdvisor 热路径（接线归后续轮）。

## Further Notes

- 与 TtlCachingToolCallback 正交：那是容量 TTL，这是服务路径三态。
