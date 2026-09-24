# Spec 5049 — S 系 S50 收口对账（effort #5049，S50）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6199–T6200，impl 2200）。
> 对账门五十零号（S 系终验波）：Wave 9 快照补登 + 全仓 verify
> + 台账核账 + 50 轮全闭合。

## Problem Statement

Wave 9（S49）PairingHeap 未入公共面快照——全仓 verify
快照门必红；S 会话 50 轮需终验封卷。

## Solution

- 快照补登：1208→1209（PairingHeap——concurrent）；
- api-surface.md 同步 +1 行；CONTEXT 计数 1208→1209；
- MAP.md #5000 行状态改为已收口；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- SSession5000LedgerAuditTest 台账核账（spec 5000–5049
  全五十轮四件套零缺位）。

## User Stories

1. 作为对账审计者，S 系 50 轮工件链四面互证全绿——封卷。
2. 作为后续会话作者，5000 系全闭合交接清晰。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳满
  5000–5049，严格递增终验）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：S50/50（100%）——S 会话全闭合；两换静脉勘误
  入档（S31 JumpHash→WoundWaitGate、S41 DG 指数直方图→
  分块滑窗计数），均因撞坑/不可自洽按诚实边界换面。
