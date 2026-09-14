---
id: T2401
title: R26 泄漏金丝雀接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2400
created: 2026-09-15
---

## Question

N 会话第 26 轮：金丝雀令牌自动注入输入还是留宿主？

## Resolution

选 **留宿主**（hook 只种植+扫描）。自动注入污染每轮输入（模型看到无关标记
影响行为）——honeytoken 的经典语义是「放进数据等待触发」，注入位置与形态
（spill 证据/工具结果/系统提示）是宿主的威胁模型决定。LayeredPolicy 同轮
裁决：纯函数工具豁免不清亡。
