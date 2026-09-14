# 1628 · 熔断慢调用率维度（resilience4j slow call rate 思想）

> 来源：N 会话 R29（effort #1628 / T2407–T2408 / impl 1181）。

## Problem Statement

熔断只看失败率：未到 deadline 但持续极慢的成功调用（退化中的供应商——
P99 从 200ms 涨到 5s）不触发任何保护，用户端体验已经崩了熔断还 CLOSED。
「慢即降级」与「败即熔断」是两个互补的可用性维度。

## Solution

`ModelCircuitBreaker.withSlowCallPolicy(durationThreshold, rateThreshold)` 链式
注入（不扩 Config——零配置零行为默认）：
- `recordSuccess(model, emitter, callDuration)` 带时长重载：duration ≥
  durationThreshold 记慢样本（windowSlow 环形懒建，与失败窗同步 append/出窗/
  resetWindow）；无时长的旧入账面不计慢（既有语义零变化）。
- CLOSED 判定叠加：`失败率 ≥ failureRateThreshold || 慢率 ≥ slowRateThreshold`
  （样本 ≥ minCalls 门共用）。
- advisor 主路径 nanoTime 计时自动喂入（金丝雀目标调用点）。

## Testing Decisions

- `CircuitSlowCallTest` 五断言：未注入维度 10 次慢成功仍 CLOSED；注入后 5/5
  慢（≥80% 阈）OPEN（4 次时不足 minCalls 不跳）；全快不跳；无时长面不计慢；
  rate ≤ 0 fail-fast。
- 回归：resilience 全量 389 用例。

## Out of Scope

- 慢率与失败率的分权重判定（当前或逻辑——两个独立保护维度，加权无先验依据）。
- 半开探测的慢判定（探测时长超阈即回 OPEN——探测路径已有超时逃生）。
