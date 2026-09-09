# Spec 84 — agent 并发 Turn 隔离舱（effort #45）

> wayfinder map：`.wayfinder/maps/effort-45.md`（T323–T324）。借鉴：resilience4j Bulkhead。

## Problem Statement

spawn 闸（spec 背压维度①）限实例活跃<b>会话数</b>，但模型吞吐的争抢单位是<b>在飞
Turn</b>：一个热点 agent 用少量大会话即可吃光全实例模型并发——其他 agent 被饿死，
且 spawn 闸看不到（会话数没超）。

## Solution

`AgentBulkhead`（全局旋钮）：per-agent 信号量上限表（`buzhou.bulkhead.agents.<agent>
= maxConcurrentTurns`）；未配置的 agent = NOOP 舱零开销直通（默认全不限——零行为
变化）；舱满按 `acquire-timeout`（默认 0s = fail-fast）等待空位，仍无抛
QUOTA_EXCEEDED（NON_RETRYABLE）；计数器 `buzhou.bulkhead.rejected`。三入口接线：
chat / chatForEntity（try-with-resources）/ stream（Flux.defer 内获取、doFinally
释放——名额横跨流生命周期）。autoconfig `buzhou.bulkhead.enabled`（默认关）。

## User Stories

1. 作为运维，我要给热点 agent 设并发上限，所以模型吞吐不被单 agent 独占。
2. 作为宿主，我要默认零行为，所以升级无感、按需 opt-in。
3. 作为调用方，我要容量拒绝有类型可辨（QUOTA_EXCEEDED），所以分流重试/降载有据。

## Implementation Decisions

- 全局旋钮第三次复用（BuzhouMetricsHolder/EvalRunRegistry/ErrorSignatures 先例）。
- stream 名额不提前释放（与在途计数同收口点——提前释放=多占吞吐的假象）。
- 装配经 Binder 绑定 agents map（结构化键入 SKIPPED 矩阵登记）。

## Testing Decisions

- NOOP 默认（无配置直通、不计数）；上限拒绝（错误码+消息含 agent/limit）+ 释放
  复得；等待超时档（真等过）；端到端并发 chat 一成一拒（blocking model + latch）。

## Out of Scope

- 跨实例分布式舱；per-tool 隔离；等待队列深度观测（fog 记账）。

## Further Notes

- 与共享限流闸（spec 54，RPM/TPM）正交：那是速率维，这是并发维。
