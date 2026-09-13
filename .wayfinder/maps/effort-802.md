# effort #802 — 指标新鲜度审计

- 会话：H 会话 800 系第 3 轮 ｜ spec [802](../../../docs/spec/802-metric-freshness.md) ｜ 票 [T1105](../tickets/T1105-metric-freshness.md)/[T1106](../tickets/T1106-metric-freshness-verify.md) ｜ impl555
- 借鉴：Prometheus staleness 处理（prometheus/prometheus ≈59K star）——序列停止写入即从查询消失，静默是信号

## 勘察（排重）

- BuzhouMetrics/BuzhouMetricsHolder：写侧门面（counter/timer/gauge）——无「最后写入时刻」维度。
- HealthTimeline（149）：健康状态时间线——状态翻转史非指标写路径。
- grep -i `staleness|freshness|lastWrite`：仅 ConfigDoctorHealth 命中（无关）——新鲜度族缺位。

## 决定

`MetricFreshnessTracker`（core.metrics，BuzhouMetrics 装饰器）：counter/timer 写入刷新名字级最后写入时刻；`audit(now, staleAfter)` 报陈旧清单（年龄降序封顶 64）+ trackedNames + truncated。gauge 不追踪（连续量无写入语义——诚实边界）；名字级而非 series 级（tag 组合爆炸折中）；名字封顶 512。装饰器零委托变更——包一层即得面，可叠 MicrometerDualWriter。

## 测试

写入刷新+委托透传/陈旧判定年龄降序/gauge 不追踪/名字封顶 512+truncated/空名忽略+清单封顶/fail-fast——6 例全绿（首轮 CAS 溢出 1 名的边界 bug 被封顶测试当场抓获——测试先行红绿的价值见证）。

## 诚实边界

名字级（同名不同 tag 视为一体——series 级归 Micrometer 侧）；无持久化（进程内存有界）；不自动恢复（只报不修）；staleAfter 由调用方定（无 yml——装配留位）。
