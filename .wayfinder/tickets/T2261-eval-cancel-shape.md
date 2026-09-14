---
id: T2261
title: EvalRunner 评估 run 协作式取消面的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 6 轮：进行中的评估 run 如何主动止损？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证：EvalRunner.run 一旦开始只能跑完全程或等自动止损（失败率剪枝 spec 901 / 预算闸 spec 520）——宿主发现数据集配错/方向不对时无「立即止损」通道（在飞大 run 白烧算力与模型费）。

形状：实例级协作式取消 requestCancel()（volatile 标记；run 开始时清零防上轮残留污染）——项边界生效（Kubernetes Job 删除传播语义：在飞项做完、未启动项不再启动），串行与并行两路径统一：剩余项诚实标新状态 STATUS_CANCELLED（"[CANCELLED] 宿主请求取消"——与 pruned 的失败率止损语义分立）；已完成项结果保留照常落盘（可分析已完成部分）；cancelled 项不进 pass/fail/error 任一桶；指标 buzhou.eval.run.cancelled。
