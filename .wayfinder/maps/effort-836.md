# effort #836 — 半开探测成功率读数

- 会话：H 会话 800 系第 37 轮 ｜ spec [836](../../../docs/spec/836-halfopen-probe-stats.md) ｜ 票 [T1173](../tickets/T1173-halfopen-probe-stats.md)/[T1174](../tickets/T1174-halfopen-probe-stats-verify.md) ｜ impl589
- 借鉴：Resilience4j permitted probe 语义扩散（811 crash-loop 互补：跳闸频次 vs 探测质量）

## 勘察（排重）

- RoutingHealthDampener/ModelCircuitBreaker：半开执行面——探测质量读数缺位。
- CircuitCrashLoopDetector（811）：跳闸频次——探测成败分布互补。
- grep -i `probe.*success|halfopen.*rate`：无命中。

## 决定

`HalfOpenProbeStats`（resilience.ratelimit）：record(model, success)——成败累计+连续失败 streak（成功清零）+近窗 20 boolean 环成功率；模型封顶 32+truncated；null 忽略；未知模型 stats null。喂点=探测结果处装配侧。

## 测试

计数+streak 清零+近窗率 0.6/连续失败 streak=2→成功归零/近窗滑动（早期全败滑出率 1.0、累计失败保留）/封顶 32+truncated+超封顶 null/null 忽略——4 例全绿。

## 诚实边界

读数不控制探测许可（许可归断路器）；streak 无自动惩罚（决策面）；近窗率是样本均值非加权。
