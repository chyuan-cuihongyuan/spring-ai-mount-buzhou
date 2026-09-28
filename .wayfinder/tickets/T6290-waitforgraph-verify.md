---
id: T6290
title: T 会话 T45 WaitForGraph 等待图死锁检测的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6289]
created: 2026-09-28
---

## Question

T45 合同怎么逐一验绿？（spec 6045 / effort #6045 / T45）

## Resolution

**验证通过**：WaitForGraphTest 五测全绿——无环链零
误报；二环/三环带入口规范环逐值钉住；受难者打断后复原；
fail-fast 四路。（勘误：出边计数漏减+规范环未旋转已修）
