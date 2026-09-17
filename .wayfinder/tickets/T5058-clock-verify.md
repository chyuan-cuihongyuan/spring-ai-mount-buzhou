---
id: T5058
title: Q 会话 R29 CLOCK 驱逐的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5057]
created: 2026-09-18
---

## Question

R29 合同怎么逐一验绿？（spec 3028 / effort #3028 / R29）

## Resolution

**验证通过**：ClockEvictionTest 七测全绿——命中/未命中、100 写
容量恒 ≤3 逐出恰 97、二次机会两代手迹（变体 A 新条目带位进入：
首代全 T 扫清逐首 FIFO 样，第二代引用过者幸存未引用者被逐——
首版手迹按进入位 0 推演被实测打脸，进入位语义变体分辨教训
入档）、全引用环扫清位再逐首、同键更新零增长零逐出、两代插入
压力访问者幸存、容量 0/负 fail-fast。
