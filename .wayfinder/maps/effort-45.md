# Wayfinder Map — Buzhou agent 并发 Turn 隔离舱（effort #45，50 轮自迭代第 10 轮）

> effort #45，延续 #44（T321–T322 / impl-230）。主线：spawn 闸限<b>会话数</b>，
> 但一个热点 agent 少量大 session 可吃光全实例模型吞吐（每个 turn 都在飞）——
> 缺 per-agent 并发 Turn 隔离。resilience4j Bulkhead 的直接对应物。

## Destination

`AgentBulkhead`（全局旋钮）：per-agent 信号量上限表；未配置 agent = NOOP 舱
（默认全不限——零行为变化）；舱满按 acquire-timeout（默认 0 fail-fast）等待后抛
QUOTA_EXCEEDED；chat/chatForEntity/stream 三入口接线（stream 的名额横跨流生命
周期 doFinally 释放）；autoconfig 键 buzhou.bulkhead.enabled/agents/acquire-timeout
（默认关）；矩阵 +2 env 键 +1 SKIPPED。

## Notes

- 借鉴：resilience4j Bulkhead（per-resource 并发隔离）；全局旋钮模式第三次复用。

## Decisions so far

- 拒绝复用 QUOTA_EXCEEDED（NON_RETRYABLE——容量拒绝语义，调用方按类型分流）。
- stream 名额横跨流生命周期（与在途计数同收口点——不提前释放多占吞吐）。

## Not yet specified

- 健康端点暴露 per-agent inFlight；bulkhead 等待队列深度观测；per-tool 隔离舱
  （工具族慢调用隔离——需求证据后议）。

## Out of scope

- 沿用 #7–#44；跨实例舱（分布式并发上限——Redis 信号量语义另议）。

## Tickets

- [x] [T323 AgentBulkhead + 三入口接线 + autoconfig](../tickets/T323-agent-bulkhead.md)（impl-231）
- [x] [T324 4 例红队（NOOP/上限拒绝/等待超时/端到端）+ 矩阵 + 收口](../tickets/T324-bulkhead-close.md)
