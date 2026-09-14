# 1614 · 指标新鲜度追踪接线（spec 802 孤类救活）

> 来源：N 会话 R15（effort #1614 / T2379–T2380 / impl 1167）。spec 1611 普查修复
> 第四弹：MetricFreshnessTracker（spec 802，Prometheus staleness 借鉴）建成即孤——
> BuzhouCoreAutoConfiguration 的 metrics 装配链从未包装该装饰器。

## Solution

- 装配链恒包：`MicrometerBuzhouMetrics → MetricFreshnessTracker →（可选
  TagCardinalityGuard）→ install`。恒开理由：有界 512 名 + 纯旁路（counter/timer
  写入多一次 map put），无配置面负担。
- `MetricFreshnessHolder`：静态登记 + `audit(nowMillis, staleAfterMillis)` 便捷面
  ——观测端点/排障查询「哪些指标超过 N ms 没有写入」。

## Testing Decisions

- `MetricFreshnessHolderTest`（MutableClock）：装饰后 counter 写入 touch——
  active（持续写，20s 龄）新鲜 / dead（80s 龄）陈旧且年龄精确；未装配 audit empty。
- 回归：MetricFreshnessTrackerTest 6 用例零变化。

## Out of Scope

- /actuator/buzhou 端点的新鲜度段（audit 面已可编程查询——端点暴露后续健康面轮）。
- series 级（tag 组合）追踪（类注已声明名字级折中）。
