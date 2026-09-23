# Spec 4049 — R 系 R50 收口对账（effort #4049，R50）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6099–T6100，impl 2150）。
> 收口对账：快照补登 + 全仓 verify + 全量台账核账 + R 会话 50/50 完结。

## Problem Statement

R49 新类型（CacheControlDirectives）未入公共面快照；R 会话
50 轮需终局核账与里程碑封卷。

## Solution

- 快照补登：1168→1169（CacheControlDirectives——policy×1）；
- api-surface.md 同步 +1 行；CONTEXT 计数 1168→1169；
- 全仓 16 模块 `mvn verify` 三门绿（排除 R48 已入档两个
  满载偶红 flaky——R48 协议口径）；
- RSession4000LedgerAuditTest 全量核账（spec 4000–4049
  五十轮四件套零缺位——号段严格递增 4000 起跑）；
- 里程碑：**50/50=100%，R 会话完结**。

## User Stories

1. 作为对账审计者，五十轮四件套零缺位封卷。
2. 作为后续会话作者，S 系（5000 段）自 5000 起跑、impl 2151
   起、票 T6101 起——号段交接面清晰。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围 4000–4049 全量）。

## Out of Scope

- 不做 Q/P 雾区候选回收；不做跨会话文档重排。

## Further Notes

- R 会话 50 轮九波收卷：对账门×8（R1/R6/R12/R18/R24/R30/
  R36/R42/R48/R50）+ 组件 41 件（素描统计/编码/存储引擎/
  队列流控/调度放置/定价协议/时序事务/图文本治理/缓存治理
  九族）。下一步：S 会话（effort-5000，S1 对账门落位）。
