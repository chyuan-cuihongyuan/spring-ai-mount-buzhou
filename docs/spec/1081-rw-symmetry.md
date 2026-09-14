# 1081 — 读写对称守恒组合测试轮

> 来源：J 会话第 81 轮 = effort #1081（[T1617](../../.wayfinder/tickets/T1617-rwsymmetry-shape.md) / [T1618](../../.wayfinder/tickets/T1618-rwsymmetry-verify.md) / impl 833）。纯测试轮（I 系 R52 先例）：零生产改动，组合语义验证。

## Problem Statement

R46 bytesWritten 与 R47 bytesRead 双口径并存——**同内容写入后读回两侧字节恒等**未经验证：若 UTF-8 编码口径漂移（写侧按字符读侧按字节），读面谱系可信度崩塌。

## 目标

新增 `ReadWriteSymmetryTest`（buzhou-tools）：①bytesWritten == bytesRead（ASCII/中文/混合三内容）②双侧守恒恒等式保持 ③reset 隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- read_range 侧口径（R62 已独立）。
- 其他域组合语义（按需另轮）。
