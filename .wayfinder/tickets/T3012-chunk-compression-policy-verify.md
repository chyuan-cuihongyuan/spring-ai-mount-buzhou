---
id: T3012
title: 分块压缩策略的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3011]
created: 2026-09-23
---

## Question)

压缩判定在阈值/收益/代价/畸形下正确吗？（spec 1905 / effort #1905 / R106）

## Resolution`

**ChunkCompressionPolicyTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ChunkCompressionPolicyTest）：恰阈值含上压缩/未到不压；
3:1 省 2/3 精确；读代价直读 3.0；畸形三型（负年龄/负阈值/ratio≤1）
fail-fast。
