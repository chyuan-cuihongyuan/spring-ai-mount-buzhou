# 1075 — 会话归档操作读面

> 来源：J 会话第 75 轮 = effort #1075（[T1605](../../.wayfinder/tickets/T1605-archiver-stats-shape.md) / [T1606](../../.wayfinder/tickets/T1606-archiver-stats-verify.md) / impl 827）。借鉴：S3 lifecycle 归档统计（归档成功/跳过/拒绝对账是生命周期策略健康的第一读面）。core/cleanup 域首轴。

## Problem Statement

`SessionArchiver.archive()`（会话归档主入口）分支零计数——pdbRejected 有单点 micrometer 但 archived 成功量与 emptySkipped 无进程内直读：**归档吞吐与跳过原因分布不可见**。宿主无法回答「归档策略跑了多少会话、多少是空会话空转、可用性下限拦了多少」。

## 目标

- `SessionArchiver` 增量（core/cleanup，静态面）：四 `AtomicLong`。
  - `archiveCalls`：archive 入口计数；`archived`（true=成功归档）；
  - `emptySkipped`（空会话诚实不动）/ `pdbRejected`（可用性下限拒——进程内对账补齐）。
- 嵌套 `record ArchiveStats(...)` + `stats()` + `resetForTest()`。
- 口径诚实：异常外溢的入口不入桶（异常打破正常流程，既有异常语义不变）。

## 兼容性

纯增量读面：archive() 返回语义、pdb 下限与互斥锁语义逐位不变；静态面理由同 R46–R74 先例；无新配置项。

## Out of Scope

- restore 面独立计量（另轴）。
- 归档字节量（store 层既有口径）。
