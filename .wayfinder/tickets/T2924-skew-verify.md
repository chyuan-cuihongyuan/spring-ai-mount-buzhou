---
id: T2924
title: 写偏斜检测的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2923]
created: 2026-09-16
---

## Question]

偏斜判定在医生反例/写冲突/不交/扫描/畸形五面下正确吗？（spec 1861 / effort #1861 / R62）

## Resolution`

**WriteSkewDetectorTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=WriteSkewDetectorTest）：双读 oncall-count 各写己行为真；同键
竞争非偏斜；读不交/只读残缺非风险；3 事务扫描恰 1 对 (a,b)+null 空；
空白 id/null 读写集 fail-fast。

