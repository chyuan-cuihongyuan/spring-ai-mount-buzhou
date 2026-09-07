# Spec 178 — 重试预算（effort #133）

> wayfinder map：`.wayfinder/maps/effort-133.md`（T535–T536）。借鉴：Twitter Finagle
> retry budget（重试配额 = 流量百分比，防重试风暴）。

## Problem Statement

固定次数重试在全局故障时放大流量（1k QPS × 3 重试 = 4k QPS 打向垂死的
上游）——重试预算把重试量压到「流量占比」内：故障期自动勒紧、恢复期自动
回填，无需窗口或人工。

## Solution

`backpressure/RetryBudget`：内部毫单位整数账（percent=20 → 每请求 200 毫；
1000 毫 = 1 次重试）。`deposit()` 每请求存入（正常/失败都存——流量即预算
来源）；`tryAcquire()` 支取（CAS 原子，不足即拒 + `denied()` 计数——风暴
被压制的证据面）；初始底数 `minBalance` 覆盖冷启动；`refill()` 运维逃逸。
连续累积无窗口（Finagle 同款要义）。

## User Stories

1. 作为 SRE，上游故障时重试被预算勒住，所以级联雪崩少一个放大器，且
   denied 计数直读「压制了多少次」。
2. 作为开发者，恢复后配额随流量自动回填，所以无需定时任务或窗口调参。

## Testing Decisions

- 红队：10 请求 @20% = 2 次额度后耗尽拒绝；底数冷启动；恢复随流量回填 +
  refill；参数 fail-fast。

## Out of Scope

- 接线（模型/工具重试路径）；per-model 分账；窗口化；异步。

## Further Notes

- 背压族第三员：SpawnGate（并发）/ AgentBulkhead（隔舱）/ RetryBudget
  （重试占比）。
