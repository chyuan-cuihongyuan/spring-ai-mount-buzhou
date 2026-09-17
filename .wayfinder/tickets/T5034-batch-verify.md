---
id: T5034
title: Q 会话 R17 攒批器的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5033]
created: 2026-09-18
---

## Question

R17 合同怎么逐一验绿？（spec 3016 / effort #3016 / R17）

## Resolution

**验证通过**：BatchAccumulatorTest 九测全绿——条满龄 1ms 即冲、龄
恰 100 达界（99 不冲）、双未达持有、drain 重锚批龄、2000 随机操作
守恒恒等逐次断言、空批不动账、三批循环账面（3 冲 5 条）、配置
fail-fast、冲出保序。
