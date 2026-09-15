---
id: T1687
title: 预算 needed 判定分布并入（BudgetClampStats 尾参追加）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1655
created: 2026-09-15
---

## Question

J 会话第 114 轮：budget 域读面深化的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R113 BudgetClampStats 深化——`compactionNeeded` 判定（total > effective*threshold）的 true/false 分布零计数：**needed=true 频次即压缩触发压力信号**（预算域判定面的另一半）。

形状裁决：`BudgetClampStats` record **尾参追加** `neededTrue`/`neededFalse` 两计数（record 追加纪律同 R49 headerDrops）——evaluate 里 needed 判定处落桶；stats()/resetForTest() 同步。既有五字段语义不变；调用方（测试）仅 record 构造位置参数需适配。零生产行为改动。
