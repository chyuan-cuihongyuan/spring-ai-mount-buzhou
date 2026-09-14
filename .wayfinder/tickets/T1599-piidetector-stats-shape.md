---
id: T1599
title: PII 检测引擎读面（PiiDetectorStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1591
created: 2026-09-15
---

## Question

J 会话第 72 轮：guard/pii 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：PiiDetector（检测引擎原语：scan/pseudonymize）零计数——与 PiiHitStats（业务层 type×side 命中，hook 喂点）不同轴：引擎原生匹配数是检测规则覆盖率的直接口径（引擎 vs 业务双层分离，Yara 规则引擎统计思想）。

形状裁决：`PiiDetector` 内静态 `AtomicLong` 四计数——scanCalls（scan 入口）/ scansWithHits（命中非空——弱校验分子）/ matchesFound（原生匹配总数，dedupe 前口径）/ pseudonymizeCalls（假名化调用）；嵌套 `record PiiDetectorStats` + `stats()` + `resetForTest()`。口径诚实：scanCalls 与 scansWithHits 弱校验非硬守恒；scan/pseudonymize 返回语义逐位不变。

Out of scope：按 PiiType 分桶（HitStats 已覆盖业务侧）；pattern 匹配耗时（另轴）。

