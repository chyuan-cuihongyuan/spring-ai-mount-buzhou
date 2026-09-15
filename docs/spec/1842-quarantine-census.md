# Spec 1842 — 隔离区普查（effort #1842，R43）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2885–T2886，impl 1443）。借鉴：
> 邮件隔离区 / 恶意样本沙箱——可疑未定罪内容暂存待审而非即杀，误报有
> 申诉出口、真阳有龄期上限。

## Problem Statement

护栏命中后的处置只有「放/杀」两极：疑似（低置信命中）即杀则误报无申诉
出口；只放则真阳逃逸——「暂存待审」的中间态缺积压读数：待审最老旧多长
（该催审了）、待审占比多高（隔离区变黑洞了没）。

## Solution

`QuarantineCensus`（buzhou-guard，静态纯函数）：

- `Quarantined(itemId, reason, ageMillis, reviewed)` 条目契约（id 非空白、
  age ≥ 0）；
- `census(entries)` → `Census(items, pendingReview, reviewedCount,
  oldestPendingAgeMillis)`（无待审 -1 哨兵）+ `pendingRatio()`（空区 -1
  哨兵）。

## User Stories

1. 作为审查运营者，oldestPending=900ms 且 2/3 待审 → 审查积压，该加审
   或放宽规则置信阈。
2. 作为误报申诉者，隔离而非即杀——命中原因可查、放行可期。
3. 作为框架宿主，龄期与审查口径自声明，纯读面零放行。

## Implementation Decisions

- 纯读不放行（审查归宿主）；最老旧只看待审侧（已审不积压）。

## Testing Decisions

- 待审/已审分账+最老旧；两极（全已审健康/全待审拥堵）；空表/null 哨兵；
  畸形两型 fail-fast。

## Out of Scope

- 不执行审查/放行；不做自动龄期过期（过期策略归宿主）。

## Further Notes

- 与 GuardExemptionRegistry 正交：那是豁免登记，这是待审积压读数。
