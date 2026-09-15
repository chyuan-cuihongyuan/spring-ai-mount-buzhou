---
id: T2854
title: 会话布隆粗筛的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2853]
created: 2026-09-16
---

## Question]

布隆契约在零假阴性/误报界/确定性/幂等/畸形五面下正确吗？（spec 1826 / effort #1826 / R27）

## Resolution

**SessionBloomFilterTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=SessionBloomFilterTest）：100 加入全报见过；空布隆全拒+千探针误报
<5%+饱和度 (0,0.5)；跨实例同答案同饱和度；重复 add 不抬升；空白 id/
bits<64/hashes 越界 fail-fast。

