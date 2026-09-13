# 802 — 指标新鲜度审计

> 来源：H 会话第 3 轮 = effort #802 / [T1105](../../.wayfinder/tickets/T1105-metric-freshness.md) / [T1106](../../.wayfinder/tickets/T1106-metric-freshness-verify.md) / impl 555。
> 借鉴：Prometheus staleness 处理（≈59K star）——序列静默是信号。

## Problem

某机制的指标突然没数了（写路径死亡/装配丢失/条件分支不再走到）只会被「图表空了」偶然发现：BuzhouMetrics 只有累计值无时间维度，「最近还在写吗」不可查询。

## Solution

`MetricFreshnessTracker`（core.metrics，BuzhouMetrics 装饰器，纯旁路）：

- **写入刷新**：counter/timer 每次调用刷新名字级 `lastWriteMillis`（名字封顶 512，超限不再记 + truncated 如实）。
- **审计读数**：`audit(nowMillis, staleAfterMillis)` → 超时未写的指标按年龄降序（封顶 64）+ trackedNames + limit + truncated。
- **gauge 不追踪**：连续量无「写入」语义（注册即持续采样）——口径显式。
- **零委托变更**：装饰器模式，包任意 BuzhouMetrics 即得面；Clock 注入可测。

## 兼容性

纯新增装饰器（不改变任何默认装配——应用显式包装）；无既有签名变更。

## 诚实边界

名字级非 series 级（tag 组合爆炸的折中——series 级归 Micrometer 注册表）；进程内存有界（重启清零）；只报不修（自动恢复是策略域）；staleAfterMillis 调用方传入（无 yml 键——健康面接线留位）。
