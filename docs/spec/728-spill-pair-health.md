# 728 — spill 配对完整性健康面

> 来源：G 会话第 29 轮 = effort #728（707 审计的接线，548 同型）/ [T1056](../../.wayfinder/tickets/T1056-spill-pair-health.md) / [T1057](../../.wayfinder/tickets/T1057-spill-pair-health-verify.md) / impl 628。

## Problem

707 配对审计是原语——actuator/312 读不到「当前有多少残缺对、吞了多少字节」。

## Solution

`SpillPairHealth`（implements BuzhouHealth）：mechanism=spill-pair；禁用 UNKNOWN（BuzhouHealth 语义）/启用恒 UP（残缺 findings 是数据需关注）；details：dataFiles/metaFiles/dataBytes/dataWithoutMeta/metaWithoutData。装配随 BuzhouSpillHealthAutoConfiguration（buzhou.spill.root-dir 同源）。

## Out of Scope

DOWN 语义；自动清理（housekeeper 接线）。
