# 1611 · 未接线机制孤类普查 + 熔断旁路遥测接线

> 来源：N 会话 R12（effort #1611 / T2373–T2374 / impl 1164）。R11 发现
> ModelOutlierEjection 孤类的模式推广：全仓普查还有多少 spec 建成但生产零接线的机制。

## Problem Statement

自迭代流程的系统性风险：机制类建成、测试齐全、但装配/调用路径从未落地——机制等于
关闭，而 spec 台账显示「done」。R11 的离群驱逐是首例；普查证明不是孤例。

## 普查结论（2026-09-15，126 个机制类初筛、40 个深度核验）

**确认孤类 15 项（19 类）**，按域：
- buzhou-guard（5）：GuardExemptionRegistry（820 豁免登记，hook 无一征询）、
  SessionCanaryRegistry（528 泄漏金丝雀）、ToolRoleGuardHook（141 角色权限——
  无 GuardModule 装配路径）、InputFloodGuardHook（167 泛洪防护——同）、
  OnnxPromptGuard+InjectionClassifier（12 §guard-25 分类层，SPI 无消费点）。
- buzhou-core（7）：MetricFreshnessTracker（802，metrics 未包装饰器）、
  LayeredPolicy+PolicyLayerAttribution（1003）、LeakSuspectAggregator（839，
  listener 从未挂）、SessionQuarantine+Hook（143）、CatalogDriftWatcher（201——
  skills 镜像类已接线，core 版漏接）、IdleSessionMonitor+IdleDurationHistogram
  （179/841，喂点本身是孤类）、EstimatorCalibrationAudit（819，对账挂点未建）。
- buzhou-resilience（3）：CircuitCrashLoopDetector（811）、HalfOpenProbeStats
  （836）、ShadowProbe（189——注意与已接线的 ShadowTrafficController 是两个机制）。

**疑似 6 项（7 类）**：AgentCostLedgerHook（65——「宿主显式挂载」设计 vs 零示范）、
TurnConcurrencyTracker（1400——observer 先例存在但未自动接）、InjectionParanoiaPolicy
（826——整条分类→裁决→拦截链全断）、EvalFlakinessDetector（513）/EvalPassAtK（902）
（静态工具豁免范畴但零调用）、PropertiesPolicyConfigProvider+InMemoryBindingPolicyStore
（默认实现不对称）。

**共性模式**：①「读数/审计/看门狗」类喂点注释写「装配侧/宿主触发」而装配侧从未落地；
② guard hook 建成但 GuardModule.Builder 缺开关字段。**修复模板**：spec 1610 对
ModelOutlierEjection 的补接线方式（装配 + 喂点 + opt-in/恒挂）。

## 本轮修复（resilience 域两项）

- `ModelCircuitBreaker.withTelemetry(crashLoop, probeStats)` 链式注入（null=不喂）：
  跳闸变迁喂 `recordOpen`、HALF_OPEN→CLOSED 喂 `recordRecovery`（spec 811 指定挂点
  =702 journal 同源）；半开探测成败喂 `probeStats.record`（spec 836 指定挂点）。
- 装配恒挂（ResilienceModule，与 702 journal 同款：纯读数旁路、有界内存 32 模型
  封顶、crash-loop 短窗 = 10 分钟 3 次跳闸）。

## User Stories

1. 作为运维者，我想让 crash-loop 与半开探测质量读数真实存在，所以「恢复即再炸」的系统性故障可见。
2. 作为开发者，我想让普查结论可追溯，所以 15+6 项全档入 spec、修复排队明确。

## Testing Decisions

- `CircuitTelemetryWiringTest` 三断言：三次 OPEN（首跳+两轮探测失败回跳）→ crash-loop
  闩锁；半开探测一败一成 → ProbeStats 计数 + 恢复清闩；未注入遥测零行为。
- 回归：resilience 全量 380 用例。

## Out of Scope

- 其余 13 项确认孤类的修复（guard hook 家族装配面、core 读数家族喂点——每项独立轮）。
- 疑似 6 项的产品判断（「宿主挂载」设计 vs 可发现性）。

## Further Notes

- 普查方法论沉淀：机制类必须核验生产调用面（grep 类名+核心方法，排除测试/文档），
  「装配点即接线」与「SPI 按需」是仅有的两类豁免。
