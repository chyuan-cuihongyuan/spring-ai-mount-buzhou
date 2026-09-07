# Wayfinder Map — Buzhou 体检陈旧度（effort #116，A 会话第 11 轮）

> A 侧编号策略沿用 #111 声明。借鉴 Consul TTL health check。

## Destination

健康面的「新鲜度」：体检报告超过 TTL 未刷新转 UNKNOWN(stale)——旧快照不
冒充「现在干净」；reexamine 手动刷新面恒可用。

## Notes

- 默认无 TTL = 既有行为零变化（兼容构造）；details 增 examinedAt/
  freshnessTtlMs/stale 三键（有界）。

## Decisions so far

- [ConfigDoctorHealth TTL](../tickets/T467-doctor-staleness.md) — Examined(报告+
  时刻) 事实对 + status 三态 + reexamine 逃逸面。

## Not yet specified

- 其余健康段（bulkhead/archive/signature）同款 TTL 化；autoconfig yml 键。

## Out of scope

- 自动重体检调度（不自装调度纪律——cron 驱动 reexamine）。

## Tickets

- [x] [T467 体检陈旧度](../tickets/T467-doctor-staleness.md)（impl-283）
- [x] [T468 收口提交](../tickets/T468-doctor-staleness-close.md)（impl-283）
