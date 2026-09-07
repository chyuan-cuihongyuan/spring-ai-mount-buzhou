# Wayfinder Map — Buzhou 虚拟 key 健康面（effort #122，A 会话第 17 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 148 fog 项（key 配额的可观测面）。

## Destination

/actuator/buzhou virtual-keys 段：在册数 + 耗尽数 + top-8 用量行
（used/limit/exhausted）——哪个 key 见顶一屏定位。

## Notes

- 恒 UP 观测不裁决（耗尽由预算闸拦截——与 bulkhead/archive 同纪律）；
  @ConditionalOnBean(VirtualKeys)——宿主声明 bean 才出现（编程面默认无）；
  top-8 有界 + exhaustedKeys 全量计数不截断。

## Decisions so far

- [VirtualKeysHealth](../tickets/T507-vkeys-health.md) — topUsage 行映射 +
  isExhausted 行内标记。

## Not yet specified

- reset 面的操作端点；成本（micro-USD）key 列。

## Out of scope

- DOWN 判定；key 增删操作面。

## Tickets

- [x] [T507 虚拟 key 健康面](../tickets/T507-vkeys-health.md)（impl-289）
- [x] [T508 收口提交](../tickets/T508-vkeys-health-close.md)（impl-289）
