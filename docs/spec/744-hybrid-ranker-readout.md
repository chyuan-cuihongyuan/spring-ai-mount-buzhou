# 744 — 混合排序融合权重读数

> 来源：G 会话第 45 轮 = effort #744（605 混合排序的声明生效确认面，638 同型）/ [T1090](../../.wayfinder/tickets/T1090-hybrid-ranker-readout.md) / [T1091](../../.wayfinder/tickets/T1091-hybrid-ranker-readout-verify.md) / impl 645。

## Problem

605 混合排序支持加权构造（semantic:lexical）——但权重本身无读数：yml 声明 2:1 是否真的生效、融合发生了多少次、语义路降级了多少次，全不可见（semanticFallbackCount 是唯一样本）。装配错误（权重写错位）静默失效。

## Solution

638「声明生效确认面」同型：

- `semanticWeight()` / `lexicalWeight()`：构造期权重读数；
- `fusedCount()`：两路信号齐备完成 RRF 融合的次数（语义降级单路序不计——那条路走 semanticFallbacks）。

## Out of Scope

运行期改权重（构造期定死——与 710 holdout 同取舍）；per-query 分数明细（rank 返回列表语义不变）。
