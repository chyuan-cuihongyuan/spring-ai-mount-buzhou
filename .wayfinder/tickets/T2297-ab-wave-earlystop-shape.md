---
id: T2297
title: A/B 并行 SPRT 波间早停（spec 1522 扩散）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2295
created: 2026-09-15
---

## Question

M 会话第 26 轮：PairwiseEvalRunner 并行路径的 SPRT 早停/宿主取消如何真生效？

## Resolution

**用户常设授权 AFK（可推翻）**

现状：earlyStop/hostCancel 检查在 task 首行，但全量 invokeAll 派发——虚拟线程全起，检查只挡住"尚未被调度"的少数项，波内早停近似无效。形状：spec 1522 分波执行扩散——items 按 workers 分块，波间检查 earlyStop/hostCancel（达界/取消即 break，剩余 skipped）；波内语义不变（scored 原子更新 liveWins 供下波判定）。16 用例零回归（行为等价面内收紧：早停更早生效）。
