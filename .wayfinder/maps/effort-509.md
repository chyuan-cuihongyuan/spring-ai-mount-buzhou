# Wayfinder Map — Buzhou 时延 SLO 燃尽（effort #509，E 会话第 10 轮）

> E 会话第 10 轮（321 扩散轮）。勘察：321 ErrorBudget 是**错误率**语义
> （成败二值）；**时延 SLO**（「99% 轮次 < 3s」的坏事件=latency>阈值）
> 空白——「错误率正常但变慢了」的隐蔽退化（416 分位数观察面）没有
> SRE 燃尽语言与阈值告警。Google SRE 多窗燃烧率同源思想。

## Destination

`health.LatencySloMonitor implements BuzhouHook`（TurnTimingHook 计时
同法：beforeTurn 起点 LRU 1024/afterTurn elapsed）：坏事件 =
elapsed > thresholdMillis → `ErrorBudget.record(agentName, ok)`（复用
321 全部燃尽语义：burnRate/breaching/topBreaching/min-samples 防一败
100% 噪声——不造第二状态机）；`budget()` 暴露观测面接既有健康/面板。
`BuzhouLatencySloProperties`（buzhou.latency-slo.{enabled, threshold-
millis, slo-percent, burn-rate-threshold, window, min-samples}，enabled
默认关 opt-in）。装配：RuntimeConfig.hooks(...) 单维度工厂进 merge。

## Notes

- 号段：spec 509 / T769–T770 / impl-412。
- 借鉴源：Google SRE workbook（321 同源——bad-event 定义换成时延）。
- 诚实边界：计时含模型调用+工具全链（端到端口径——191 同）；只观测
  不拦截；异常轮（无 afterTurn）不计样本（TurnTiming 同法不错配）。

## Out of scope

- 分位数目标（p99<p3s 形态——阈值绝对值先行）；多窗联合燃烧率
  （321 单窗同法）；自动摘流量。

## Tickets

- [x] [T769 LatencySloMonitor hook](../tickets/T769-latency-slo-monitor.md)
- [x] [T770 yml 装配与观测面](../tickets/T770-latency-slo-assembly.md)
