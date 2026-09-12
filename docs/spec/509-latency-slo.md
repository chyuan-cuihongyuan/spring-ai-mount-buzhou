# Spec 509 — 时延 SLO 燃尽（effort #509）

> wayfinder map：`.wayfinder/maps/effort-509.md`（T769–T770）。E 会话第 10 轮。

## Problem Statement

321 ErrorBudget 是错误率语义；「99% 轮次 < 3s」的时延 SLO 空白——
错误率正常但变慢了的隐蔽退化（416 分位数是观察面）没有燃尽语言与
阈值判定。Google SRE 同源思想：坏事件定义换成时延。

## Solution

`health.LatencySloMonitor`（BuzhouHook，TurnTimingHook 计时同法）+
`BuzhouLatencySloProperties`：

- **计时**：beforeTurn 记起点（LRU 1024 会话、重入覆盖）、afterTurn
  elapsed——坏事件 = elapsed > thresholdMillis。
- **燃尽**：复用 `ErrorBudget`（scope=agentName，record(agent, ok)）——
  burnRate/breaching（min-samples 防一败 100% 噪声）/topBreaching 全部
  语义继承，`budget()` 暴露接既有健康/面板。
- **yml**：`buzhou.latency-slo.{enabled, threshold-millis, slo-percent,
  burn-rate-threshold, window, min-samples}`——enabled 默认关（opt-in）；
  装配 `RuntimeConfig.hooks(...)` 进 merge。

## User Stories

1. 作为 SRE，我想声明「99% 轮次 < 3s」并看到燃尽率， so 变慢了的退化
   用 SRE 语言可见可告警。
2. 作为宿主，我想未声明时零钩子零开销， so 该能力完全 opt-in。

## Implementation Decisions

- 计时端到端口径（模型+工具全链——191 同）；异常轮缺 afterTurn 不计
  样本（不错配）。
- 状态机复用 321 不造第二套（星形内合法复用——504 同法）。
- 只观测不拦截（摘流量归网关域）。

## Testing Decisions

- 慢模型（sleep 30ms，threshold 10ms）多轮 → budget breaching；快模型
  → 不 breach；topBreaching 含 agent 名。
- Config 校验 fail-fast（threshold>0、slo∈(0,100)）；yml 装配/缺席。

## Out of Scope

- 分位数目标；多窗联合；自动摘流量。

## Further Notes

- 新公共类型 `LatencySloMonitor`、`BuzhouLatencySloProperties` 随轮
  regenerate 快照 + api-surface.md 加行。
