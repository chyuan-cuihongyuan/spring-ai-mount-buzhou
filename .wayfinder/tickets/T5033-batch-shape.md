---
id: T5033
title: Q 会话 R17 攒批器的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

吞吐与延迟的攒批权衡怎么显式旋钮化？（spec 3016 / effort #3016 / R17）

## Resolution

**BatchAccumulator（core/concurrent，泛型）**：Kafka producer 双阈值
——条数满或批龄 ≥linger 任一达标即冲；offer 返回满批信号+drain 保序
重锚+时间调用方传入（确定性）+守恒对账面（offered==flushed+在批）。
纯判定与持有件，调度归调用方。
