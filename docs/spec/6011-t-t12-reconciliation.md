# Spec 6011 — T 系 T12 周期对账（effort #6011，T12）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6223–T6224，impl 2212）。
> 对账门六十二号（T 系第二波）：Wave 2 快照补登 + 全仓 verify
> + 台账核账。

## Problem Statement

Wave 2（T7–T11）五个新公共类型未入公共面快照——全仓 verify
快照门必红；第二波需对账封账。

## Solution

- 快照补登：1213→1218（AhoCorasick/MyersDiff/BkTree/
  SuffixArray——metrics + PieceTable——fs）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1213→1218；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6010
  零缺位）。

## User Stories

1. 作为对账审计者，T 系第二波工件链四面互证全绿。
2. 作为后续轮作者，快照门恢复绿——继续推进。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6010）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：T12/50（24%）。勘误入档：T7 canonical 序由注册序
  改字典序并列（重复注册折叠）；T8 双空序列越界早退；T11
  delete 双缺陷根治。
