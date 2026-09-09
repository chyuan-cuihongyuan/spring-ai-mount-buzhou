# Spec 107 — 配置体检健康段（effort #69）

> wayfinder map：`.wayfinder/maps/effort-69.md`（T393–T394）。spec 91 fog 项收口。

## Problem Statement

ConfigDoctor（spec 91）报告只在启动日志一闪而过：无 bad key 时零输出（感知不到
体检存在），有 bad key 时日志也被滚动冲走——运维回看不便。

## Solution

`ConfigDoctorHealth implements BuzhouHealth, ApplicationListener<ApplicationReadyEvent>`
（mechanism `config-doctor`）：就绪事件跑一次 ConfigDoctor（发现照旧走日志）+
报告缓存 AtomicReference。状态：就绪前 UNKNOWN + `{pending: true}`（未体检不冒充
干净）；就绪后 UP + `{errors, warnings, checkedKeys}`（明细留日志——健康详情有界
纪律；DOWN 留给核心职能机制，error-signatures 同纪律）。autoconfig 原 listener
bean 替换为复合 bean（同键 `buzhou.config-doctor.enabled` 默认关）。

## User Stories

1. 作为运维，我健康端点回看体检结论，所以不用翻启动日志。
2. 作为看板作者，我 errors/warnings 可画趋势，所以配置腐化可见。

## Implementation Decisions

- 体检一次缓存（键宇宙 classpath 扫描是一次性成本——不每查询重跑）。
- listener+health 复合 bean（单一 bean 双面——装配面不增）。

## Testing Decisions

- 就绪前 UNKNOWN/pending → 就绪后 UP + 缓存计数（拼错键 warnings=1）；
  端点聚合段含 config-doctor。

## Out of Scope

- 手动重跑触发面；报告导出。

## Further Notes

- 健康段家族第六员（error-signatures/bulkhead/webhook-outbox/session-index/archive）。
