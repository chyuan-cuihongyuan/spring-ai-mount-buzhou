# Spec 8000 — V 系 V1 对账门落位（effort #8000，V1）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8001–V8002，impl 2302）。
> 对账门落位轮（V 会话开工轮——USession7000LedgerAuditTest 同款公式族第九应用）。

## Problem Statement

V 会话 8000 系 50 轮工件链（spec ↔ README 纵深行 ↔ shape/verify 成对票 ↔
impl 切片）需要**预防式公式族对账门**：任一环节缺位即红，而不是等收口
才发现台账残缺——U 系八应用的同一病根（四工件手工四面维护，漏登静默）。

## Solution

`VSession8000LedgerAuditTest`（buzhou-spring-boot-starter 测试面）：
范围自扩展（扫现有 spec 文件驱动——后续轮落地自动纳入对账）——

- spec 文件（docs/spec/，8000–8049 数值号段）↔ shape 票
  V(8001+2(N−8000)) + verify 票（+1）成对存在；
- ↔ impl 切片 2302+(N−8000) 存在；
- ↔ README 含 spec 号（覆盖门复核）；
- spec 号严格递增且起点恰为 8000。

## Testing Decisions

- 门自身四测：成对票零缺位 / impl 零缺位 / README 覆盖 /
  号段递增；V1 落门即跑绿（spec 8000 自证）。

## Out of Scope

- 不做跨会话（U 系 7000 段）对账——各系对账门自守自段。

## Further Notes

- 与 USession7000LedgerAuditTest（spec 7000）同族不同面：
  第八应用 vs 第九应用——公式基点 7000/7201/2252 vs 8000/8001/2302。
- 里程碑：V1/50（2%）。
