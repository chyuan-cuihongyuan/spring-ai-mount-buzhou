---
id: T2397
title: R24 影子读探针接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2396
created: 2026-09-15
---

## Question

N 会话第 24 轮：ShadowProbe 挂主路成功后还是降级路径？

## Resolution

选 **主路成功后**。降级路径上对照「刚失败的主路」无信息量；主路健康时对照
首个备模型回答「备模型被启用时结果是否一致」——正是容量预案需要的信心面。
确定性采样同 key 稳定（观测不抖动）；执行器复用会话 deadlineExecutor（零新
线程面）+ REE 关闭竞态防护。
