---
id: T2263
title: A/B 对比 run 宿主取消面（spec 1505 扩散）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2261
created: 2026-09-15
---

## Question

M 会话第 7 轮：PairwiseEvalRunner 的取消止损面？

## Resolution

**用户常设授权 AFK（可推翻）**

现状：A/B run 已有 SPRT 序贯提前终止（spec 1605 / N 会话，统计达界自动停）与 skipped 桶，但同样无宿主主动叫停通道（对比跑双 runtime 成本翻倍，配错方向的止损价值更高）。

形状：spec 1505 取消语义扩散——requestCancel()（实例级 AtomicBoolean，compare 开始清零）+ 两路径未起项检查（earlyStop || hostCancel，复用 skipped 桶）+ PairwiseSummary 加 hostCancelled 布尔（区分统计达界停与宿主叫停；9/7 参兼容构造器保留）+ 序列化仅取消 run 落 hostCancelled 位 + 指标 buzhou.eval.ab.cancelled。
