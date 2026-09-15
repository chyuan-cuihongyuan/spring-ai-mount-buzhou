# 1213 — R14：ToolGraphAnalyzer 边缘分支补测

> 来源：K 会话第 14 轮 = effort #1213（[T1835](../../.wayfinder/tickets/T1835-graph-analyzer-edge-shape.md) / [T1836](../../.wayfinder/tickets/T1836-graph-analyzer-edge-verify.md) / impl 916）。方法论：批次化延续——既有测试密集类的**残余边缘分支**定向清扫（jacoco 行号定位 + 既有三套测试查重）。

## Problem Statement

ToolGraphAnalyzer 已有三套测试（edges/cycles/flame），但残余 1–3 missed/方法的边缘分支：cycles(null) fail-fast 从未测、count≤0 边的邻接跳过分支、边 count 同值时 from 字典序 tie-break、analyze 的 kind=null（既有测试只测非 TOOL 值而非 null 值）、timings 的 startedAt=null 计 0 与 null spanId 不入父子索引。

## 目标

- ToolGraphAnalyzerEdgeTest（6 用例）：cycles(null) fail-fast；零计数边（B→A 计 0）不构成环；同 count 边 from 字典序 tie-break（fetch 在 search 前）；kind=null span 忽略 + "tool" 小写计入；timings startedAt=null 计 0 + null spanId 不入父子索引；负时长夹 0。

## 实现决策

- 新增边缘测试类单文件（不动既有三套）；SpanRecord 直接构造（含 startedAt=null 的直接 record 构造——helper 不支持 null 起点）。

## 测试决策

- 断言只对外部行为：IAE 类型、报告的边/工具列表内容与排序、耗时归零。
- 验收门：定向绿 + ToolGraphAnalyzer 分支 95% 入账（实测，原 ~85%）+ observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ObservabilityAdvisor 剩余 96 missed（流式 harness 已落，细粒度留 R15 复扫后定）。

## Further Notes

- 「测试密集 ≠ 边缘已清」：jacoco 行号定位仍能在一个 5 测试方法的类里找到 6 条未走分支——分支读面台账（R7）的持续价值实证。
