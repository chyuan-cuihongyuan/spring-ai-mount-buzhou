# Spec 117 — bulkhead 拒绝计数（effort #79）

> wayfinder map：`.wayfinder/maps/effort-79.md`（T421–T422）。spec 84 fog 项收口。

## Problem Statement

隔离舱全局 counter（buzhou.bulkhead.rejected）无 agent 维（agent 名不可进
micrometer tag——无界纪律）：限流风暴定位不到热点 agent。

## Solution

AgentBulkhead 进程内有界拒绝表（agent → count；256 封顶折 __overflow__——
ErrorSignatures 同款纪律）：acquire 失败路径旁路 recordRejection（计数不影响容量
语义）；`topRejections(n)`（count 降序 + 同 count 字典序——输出稳定）；NOOP agent
（未配置上限）不拒不计。BulkheadHealth 详情增 topRejected（3 条 "agent xN"）。

## User Stories

1. 作为运维，我要 top 被拒 agent，所以限流风暴一屏定位热点。
2. 作为红队，我要 NOOP agent 零计数，所以未配置的 agent 不污染统计。

## Testing Decisions

- hot×2 + warm×1 → topRejections 排序 [hot, warm]；cold（NOOP）不计；
  释放后再取不受计数影响。

## Out of Scope

- 计数 reset；时间窗拒绝率；跨实例聚合。

## Further Notes

- 健康段 topRejected 与 error-signatures top 段同形态——运维心智一致。
