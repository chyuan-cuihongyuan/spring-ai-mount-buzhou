# 1096 — compact×evidence 交叉组合测试轮

> 来源：J 会话第 96 轮 = effort #1096（编号修正轮：原占 1094 与 R94 撞号，改空闲号 1096；[T1651](../../.wayfinder/tickets/T1651-compact-evid-shape.md) / [T1652](../../.wayfinder/tickets/T1652-compact-evid-verify.md) / impl 848）。纯测试轮第十一弹（R81–R89 先例）。

## Problem Statement

R59 compact_now（压缩折入）与 R73 evidence_lookup（按 evidence-id 回查消息）交叉——压缩折入不影响 MessageStore（append-only 事实源），**回查计数语义在压缩前后一致**无组合验证。

## 目标

新增 `CompactEvidenceComboTest`（buzhou-memory）：压缩前后 evidence 回查各一次——双读面计数一致（compact 与 evidence 互不干扰）、各自守恒保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 注入视图层的证据展示联动（另轴）。
