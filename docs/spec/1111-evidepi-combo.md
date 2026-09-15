# 1111 — evidence×episodic 独立性组合测试轮

> 来源：J 会话第 111 轮 = effort #1111（[T1681](../../.wayfinder/tickets/T1681-evidepi-shape.md) / [T1682](../../.wayfinder/tickets/T1682-evidepi-verify.md) / impl 863）。纯测试轮第十六弹。

## Problem Statement

R73 EvidenceLookupTool（回查）与 R55 EpisodeLedger（情景记忆）双读面同会话独立——回查不写情景、recall 不动消息存储的独立性无组合验证。

## 目标

新增 `EvidenceEpisodicComboTest`（buzhou-memory）：交叉调用后双读面各自计数独立（互不串账）+ reset 独立隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 语义向量层联动（EmbeddingProvider 共享面另轴）。
