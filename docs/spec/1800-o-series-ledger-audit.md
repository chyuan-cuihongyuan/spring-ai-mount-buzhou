# Spec 1800 — O 系对账门落位（effort #1800，R1）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2801–T2802，impl 1401）。借鉴：
> LSession1700LedgerAuditTest 的「预防式对账」——号段公式在 R1 钉死为可执行
> 断言，漂移在落盘瞬间即红，而非事后对账轮补救。

## Problem Statement

O 会话计划 150 轮连续 effort（#1800–#1949，与 L/M 并行、共享检出与提交面）。
工件链（spec ↔ README 行 ↔ 票对 ↔ impl）靠人工记忆维持号段公式，长会话必然
漂移：漏登 README 触发覆盖门死链、票对错位让对账轮误报、并行会话误入号段
无哨兵拦截。

## Solution

`OSession1800LedgerAuditTest`（starter 模块测试，扫描仓库文件驱动——后续轮
落地自动纳入对账，范围自扩展）：

- 号段公式钉死：spec N（1800–1949）→ shape 票 = T2801+2(N−1800)、verify 票
  = shape+1、impl = 1401+(N−1800)；
- 四面互证四断言：每 spec 必有票对、必有 impl、README 必含 spec 号、
  spec 号从 1800 严格递增（无缺号乱号）。

## User Stories

1. 作为 O 会话驾驶者，我落错票号（如 R3 用了 T2805 以外的号）时 verify 红
   在当轮——不用等对账轮盘点。
2. 作为并行会话，误在 1800–1949 落 spec 即触发我的严格递增/票对断言——号段
   占用有哨兵。
3. 作为仓库维护者，O 系工件链四面一致性随时可由一条测试命令核验。

## Implementation Decisions

- 纯文件扫描测试（starter 既有 LedgerAudit 族同款基座），零生产代码。
- 号段区间用数值范围（1800–1949）而非前缀正则——本系跨越 18xx/19xx 两个
  前缀段。
- 与 LSession1700LedgerAuditTest 并存互不干扰（号段不相交）。

## Testing Decisions

- 测试自身即交付物（自证四断言）；沿用 LedgerAudit 族先例（外部行为＝文件
  系统工件链形状）。

## Out of Scope

- 不做跨会话全局号段登记表；不动 L/M 系既有对账门。

## Further Notes

- R1 为纯对账门轮（K 会话 R18 纯对账轮先例）；首个机制轮自 R2 起。
