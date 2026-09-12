# 713 — span 状态分布读数

> 来源：G 会话第 13 轮 = effort #712（E 会话池「span 状态分布」未做项兑现；换题注记见 map）/ [T1024](../../.wayfinder/tickets/T1024-span-status-dist.md) / [T1025](../../.wayfinder/tickets/T1025-span-status-dist-verify.md) / impl 612。

## Problem

span 状态常量（RUNNING/OK/ERROR/CANCELLED）与按会话查询都在，但**分布面**不存在：「过去这批 turn 里工具 span 错误率多少」「有多少 span 开了没关」都要全量拉记录自算。RUNNING 残留（开了未关闭的 span）是泄漏/崩溃的信号——散在各会话里永远看不见。E 会话池「span 状态分布(519 扩散)」列入未做。

## Solution

OpenTelemetry span status 语义（状态是健康的_first_读数）：

- `SpanStatusDistribution`（core.observability，纯函数静态原语）：
  - `of(List<SpanRecord>)` → `Report(rows, total, runningResidue, errorRate)`；
  - rows：`(kind, status) → count` 聚合，kind/status 字典序（确定序——快照断言可复现）；
  - `runningResidue`：status=RUNNING 的 span 数——开启后从未关闭，泄漏/进程崩溃的直接信号；
  - `errorRate`：ERROR 数 / total（total=0 → 0.0 诚实口径）；
  - 纯读数：告警裁决归 312 AlertRuleEngine 订阅，存储归 store。

## User Stories

1. 排障：某会话表现异常——of(spansOfSession) 一屏看出 tool span 8 错 2 成，错误集中在哪类操作。
2. 健康巡检：runningResidue 持续增长 = span 关闭路径有泄漏（崩溃残留/忘记 close）。

## Implementation Decisions

- 纯函数（调用方供 spans——与 704 PromptComposition/711 TurnSequenceAudit 同「证据面原语」节奏）。
- 状态字符串不假设闭集（ SpanStatus 常量之外的值照常聚合——前向兼容）。
- errorRate 分母=total（含 RUNNING/CANCELLED——口径显式不做「终态率」歧义变体）。

## Testing Decisions

- 三 kind × 三状态混合计数精确；字典序断言；runningResidue 与 errorRate 精确值。
- 空表：total=0、errorRate=0.0、rows 空；null fail-fast。

## Out of Scope

- 时间窗过滤（调用方投影前自滤）。
- per-span 明细（聚合面先行）。
- 告警规则预置（312 订阅自选）。

## Further Notes
