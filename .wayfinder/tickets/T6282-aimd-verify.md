---
id: T6282
title: T 会话 T41 AIMD Window 加性增/乘性减窗口的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6281]
created: 2026-09-26
---

## Question

T41 合同怎么逐一验绿？（spec 6041 / effort #6041 / T41）

## Resolution

**验证通过**：AimdWindowTest 五测全绿——锯齿形态逐值钉住；
取整与 min 钳制（20→10→20 锯齿+4 下界）；max 钳制；双实例
确定性；参数校验 fail-fast。
