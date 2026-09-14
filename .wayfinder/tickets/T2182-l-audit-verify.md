---
id: T2182
title: 阶段对账测试与漂移修复的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2181
created: 2026-09-14
---

## Question

如何证明修复后工件链四面一致且对账测试自扩展？

## Resolution

**用户常设授权 AFK（可推翻）**

`LSessionLedgerAuditTest` 四测全绿（`mvn -pl buzhou-spring-boot-starter -am test`，与 SpecCoverageTest/ApiSurfaceSnapshotTest 同批绿）：票对存在（公式驱动）；impl ±1 窗；README 行含号；spec 号连续 1400-1439 缺 1439（1439→1438 重命名后 1400-1438 连续 39 张+1440）。修复批次：18 票重编号+spec 重命名+impl 置换+全仓引用单遍替换（python 正则防级联）。
