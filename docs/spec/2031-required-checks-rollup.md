# Spec 2031 — 必选检查聚合（effort #2031，R32）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3163–T3164，impl 1582）。
> 借鉴：GitHub required status checks rollup——多检查单结论的门禁语义。

## Problem Statement

质量门由多检查构成（lint / test / eval 回归 / 覆盖门）：各检查独立
上报、时机不一，「整体放不放行」的聚合口径散落各处——一票否决与
等待挂起的优先级、可选检查该不该阻断、未上报算什么，逐处自定必
漂移。

## Solution

`RequiredChecksRollup`（core/policy，synchronized 小临界区）：

- `register(name, required)`：注册检查（required 阻断门；可选仅
  显形）；`report(name, state)` 上报三态（SUCCESS/FAILURE/PENDING，
  未注册 fail-fast）；
- `rollup()` 聚合口径（GitHub 语义）：任一必选 FAILURE → FAILURE
  （**一票否决，优先于 PENDING**——已失败不必等挂起者）；否则任一
  必选 PENDING（含未报）→ PENDING（门未关）；全必选 SUCCESS →
  SUCCESS；空集恒 SUCCESS（无阻断项即放行）；可选检查不参与聚合但
  optionalFailures 显形；
- 读数：states()（注册序快照）/ stats()（总数/必选数/未报必选数/
  可选失败数）。

## User Stories

1. 作为质量门作者，多检查聚合口径一处定义——一票否决与挂起优先
   级不再逐处自定。
2. 作为 SRE，optionalFailures>0 即可选检查退化早期预警——不阻断但
   显形。

## Testing Decisions

- 全必选绿 SUCCESS；未报必选 PENDING + 计数；显式 PENDING 挂起；
  FAILURE 一票否决（混合 PENDING 场景）；可选失败不阻断但计数；空
  集 SUCCESS；快照注册序；畸形六型 fail-fast。

## Out of Scope

- 不做加权/评分聚合（布尔门口径）；不接 SpecCoverage/eval 门装配
  （接线归后续轮）。

## Further Notes

- 与评估回归门（#41）/评估胜率门（#63）正交：那些定单指标阈值，
  本件定多检查聚合语义。
