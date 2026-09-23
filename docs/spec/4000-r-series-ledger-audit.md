# Spec 4000 — R 会话 4000 系对账门落位（effort #4000，R1）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6001–T6002，impl 2101）。
> 借鉴：Q/P/O 系对账门公式族（预防式对账）。

## Problem Statement

50 轮自迭代的工件链（spec ↔ README 行 ↔ 票对 ↔ impl 切片）跨四个
目录、四个号段，靠人工记忆对号必然漂移——错号/跳号/漏登记要到很远
的轮次后才暴露，补救成本高。

## Solution

`RSession4000LedgerAuditTest`（starter，QSession3000LedgerAuditTest
同款公式族第五应用）——扫描现有 spec 文件驱动的**范围自扩展**对账：

- 号段：spec 4000–4049（2151+ 留给后续会话，不入对账）；
- 票号公式：spec N → shape 票 = 6001+2(N−4000)，verify = shape+1；
- impl 公式：spec N → impl 2101+(N−4000)；
- 四面断言：每张系列 spec 必有 shape+verify 票对、必有 impl 切片、
  README 必含该 spec 号（覆盖门）、spec 号自 4000 严格递增（无跳号
  无重号——后续轮落地自动纳入对账）。

## User Stories

1. 作为自迭代会话，轮次工件链四面互证——错号当轮即红，不欠账。
2. 作为对账审计者，R6k 对账轮全量核账有既定公式可依。

## Testing Decisions

- 空档起步：仅 spec 4000 存在时四断言全绿（起点=4000、票对/impl/
  README 行就位）；后续轮落地自动纳入——外部行为测试，扫文件系统
  驱动，不 mock。

## Out of Scope

- 不对账其他会话号段（Q-3000 余量归 Q 系对账门管）。
- 不做 git 历史维度核账（工作树即真相）。

## Further Notes

- 里程碑：1/50（R 会话总图落位轮）。
