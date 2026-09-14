# 1063 — 双时序事实台账操作读面

> 来源：J 会话第 63 轮 = effort #1063（[T1581](../../.wayfinder/tickets/T1581-factledger-stats-shape.md) / [T1582](../../.wayfinder/tickets/T1582-factledger-stats-verify.md) / impl 815）。借鉴：bitemporal 表 query/mutation 对账（双时序维护语义里写与读是两类一等公民操作，Kleppmann《DDIA》）。J 系 fact 域首轴。

## Problem Statement

`BiTemporalFactLedger`（事实台账双时序记录：section 级废止链 + 代际有效期）全路径零计数——废止写入量、历史/时点查询频次不可见；**load 损坏 JSON 静默吞掉**（catch 返回空表）意味着台账记录在无声蒸发：摘要事实链完整性对账无从下手。

## 目标

- `BiTemporalFactLedger` 增量（memory/summary，静态面）：四 `AtomicLong`。
  - `supersededWrites`：recordSuperseded 写入数；
  - `historyLookups`：historyOf 调用数；`validAtLookups`：validAt 调用数；
  - `corruptRecordLoads`：损坏段装载次数（load 解析失败按段级全损静默吞，蒸发显形核心桶）。
- 嵌套 `record FactLedgerStats(long supersededWrites, long historyLookups, long validAtLookups, long corruptRecordLoads)` + `stats()` + `resetForTest()`。
- 口径诚实：三类操作（写/历史读/时点读）语义不同，不做人为统一守恒，每计数独立对账。

## 兼容性

纯增量读面：recordSuperseded/historyOf/validAt 返回语义、代际有效期谓词、损坏容错（单条损坏不拖垮整表）逐位不变；静态面理由同 R46–R62 先例；无新配置项。

## Out of Scope

- 按 section 分桶（节名配置面，分布留后续轮）。
- validAt 命中/未命中细分（时点谓词结果面）。
