---
id: T2602
title: 评测分数 MAD 鲁棒离散度的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2601
created: 2026-09-15
---

## Question

EvalScoreMad 怎么验证？（spec 1700 验收）

## Resolution

`EvalScoreMadTest`（core 模块，纯函数直测）：①空表/n=2 → INSUFFICIENT+mad=−1；
②[1..5] → median=3/MAD=1/SPREAD 无离群；③[1,1,2,2,100] → outliers=[4]；
④全同分 → TIGHT 无离群；⑤[5,5,5,5,6] → TIGHT 且 outliers=[4]；
⑥maxZ=1.0 收紧后 [1..5] 两端点入离群；⑦null 按空表；⑧报告不可变
（入参后续变异不影响报告）。同轮落 `LSession1700LedgerAuditTest` 对账门
（spec↔票↔impl↔README 四面互证公式机检）随 starter 测试常驻。
