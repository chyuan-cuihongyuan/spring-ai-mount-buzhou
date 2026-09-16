# Spec 2000 — P 会话 2000 系对账门（effort #2000，R1）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3101–T3102，impl 1551）。
> 借鉴：LSession1700 / OSession1800 对账门公式族的第三应用——预防式
> 对账（把台账一致性做成常驻测试，而非事后补救轮）。

## Problem Statement

P 会话要连续落 150 轮（spec 2000–2149 × README 行 × T3101+2(N−2000)
票对 × impl 1551+(N−2000)），四类工件跨目录、逐轮手工登记——任何一环
漏登（spec 落了 README 没登 / 票对缺 verify / impl 缺号）都会在几十轮后
才在对账轮暴露，回溯成本高。

## Solution

`PSession2000LedgerAuditTest`（starter 模块，与 L/O 系同款公式族）：

- 号段常量：spec 2000–2149（150 轮），1950–1999 属 O 系缓冲不入账；
- 票号公式：spec N → shape 票 T3101+2(N−2000)，verify = shape+1；
- impl 公式：spec N → impl 1551+(N−2000)；
- 四断言：①每 spec 号有 shape+verify 票对文件；②每 spec 号有 impl
  切片文件；③根 README 含每 spec 号字符串（覆盖门接线）；④spec 号
  自 2000 严格递增（跳号即红）；
- 范围自扩展：扫现有 spec 文件驱动，后续轮落地自动纳入对账。

## User Stories

1. 作为 P 会话驾驶者，每轮 commit 前跑该测试即知四类工件是否齐整，
   漏登当场红。
2. 作为仓库审计者，对账轮全量核账时该测试是四面互证的机器口径，
   不依赖人工翻台账。

## Implementation Decisions

- 起点断言 spec[0] == 2000：若 R1 spec 未落盘则测试红（自举——本
  spec 落盘后即绿）。
- 文件前缀匹配（`T<id>` / `<impl>` 前缀），与 O 系一致，slug 不参与
  对账。

## Testing Decisions

- 只测外部行为（文件系统事实 ↔ 公式推值），不测实现细节；
- 先例：OSession1800LedgerAuditTest（starter，同款四断言结构）。

## Out of Scope

- 不做台账内容语义校验（票内 Resolution 完整性归人工对账轮）。

## Further Notes

- 150 轮纪律：每 6 轮一 R6k 对账轮全量核账 + 全仓 verify。
