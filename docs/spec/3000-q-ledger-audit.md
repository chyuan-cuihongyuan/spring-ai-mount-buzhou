# Spec 3000 — Q 会话 3000 系对账门（effort #3000，R1）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5001–T5002，impl 2001）。
> 预防式对账门落位（PSession2000LedgerAuditTest 同款公式族第四应用）。

## Problem Statement

150 轮自迭代的工件链（spec ↔ README 行 ↔ 票对 ↔ impl 切片）若无
机器门，漂移只能事后人肉补救——P 会话 2000 系已验证预防式公式族
有效（十一波零漂移），Q 系沿用同款纪律再应用。

## Solution

`QSession3000LedgerAuditTest`（starter 模块，范围自扩展——扫现有
spec 文件驱动，后续轮落地自动纳入对账）：

1. 号段 3000–3149 内每个 spec 有 shape/verify 票对
   （shape = 5001+2(N−3000)，verify = shape+1）；
2. 每 spec 有 impl 切片（2001+(N−3000)）；
3. README 引用每 spec 号（覆盖门互证）；
4. spec 号自 3000 严格递增（缺口即红）。

## Testing Decisions

- 沿用 P/O 系账门测试先例：Files 驱动四向断言，路径从 starter 模块
  向上定位仓库根（快照测试同款路径策略）。
- 模块测试门：`mvn -pl buzhou-spring-boot-starter -am test` 指定
  测试类；全局三门在 R6k 对账轮全仓 verify 覆盖。

## Out of Scope

- 不改任何生产代码；不占用 P-2000 余量号段（2066–2149 留给 P 续轮）。

## Further Notes

- 里程碑：1/150。Q 系基线：快照 1056 / CONTEXT 955（P R66 收口后）。
