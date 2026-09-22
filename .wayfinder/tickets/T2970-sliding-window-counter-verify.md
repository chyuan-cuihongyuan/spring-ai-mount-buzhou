---
id: T2970
title: 滑动窗口计数器的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2969]
created: 2026-09-23
---

## Question)

插值在窗初/窗末/中点/双百与准入/畸形下正确吗？（spec 1884 / effort #1884 / R85）

## Resolution`

**SlidingWindowCounterTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=SlidingWindowCounterTest）：插值 (100,0,0)=100、(0,100,0.5)=50、
(100,100,0.5)=150、(100,0,1)=0；准入 =limit 拒 <limit 放；畸形
三型 fail-fast。
