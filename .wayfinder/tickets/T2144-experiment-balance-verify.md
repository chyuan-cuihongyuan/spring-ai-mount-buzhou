---
id: T2144
title: 均衡审计容差边界与零桶失衡的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2143
created: 2026-09-14
---

## Question

如何证明容差边界含端点、零桶失衡与哨兵语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`ExperimentBalanceAuditTest` 五测全绿（`mvn -pl buzhou-core -am test`）：完全均匀 balanced；90/10 越容差失衡+降序；**缺桶计零且判失衡**（三桶只喂两桶）；无样本 -1 哨兵不冒充；容差端点含入（三桶 1150/1000/850 整 5pp——评审修正：初版双比较 FP 误差把端点推出界，实现加 1e-9 卫生余量）。
