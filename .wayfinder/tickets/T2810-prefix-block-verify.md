---
id: T2810
title: 前缀块命中读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2809]
created: 2026-09-16
---

## Question

块命中账目在共享前缀/尾块/去重/哨兵/畸形/无状态六面下正确吗？（spec 1804 / effort #1804 / R5）

## Resolution

**PrefixBlockHitStatsTest 6 用例全绿**（mvn -pl buzhou-resilience test
-Dtest=PrefixBlockHitStatsTest）：共享前缀跨请求命中；尾块丢弃；单请求全
miss+请求内去重；空表/null/全短文本哨兵；blockSize<1 fail-fast；同参重复
调用结果一致。首跑 1 红（同内容重复块数据与去重语义交互）修正测试数据后
转绿。

